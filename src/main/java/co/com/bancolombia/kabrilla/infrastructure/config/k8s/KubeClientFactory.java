package co.com.bancolombia.kabrilla.infrastructure.config.k8s;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.infrastructure.adapters.OkHttpClientFactory;
import co.com.bancolombia.kabrilla.infrastructure.config.tokeneks.TokenProvider;

import io.kubernetes.client.openapi.ApiClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Un {@link ApiClient} por cluster, reutilizado mientras su token de EKS siga sirviendo.
 * <p>
 * El token se regenera cuando: vence el TTL, cambian las credenciales AWS (el usuario pegó unas nuevas) o el
 * cluster responde 401. Esto último importa porque {@code aws eks get-token} firma el token localmente: con
 * credenciales vencidas igual devuelve un token, que el cluster rechaza.
 */
@Slf4j
public class KubeClientFactory {

    // Los tokens de EKS duran 15 min: se renuevan un poco antes
    static final Duration TOKEN_TTL = Duration.ofMinutes(14);
    private final ConcurrentMap<String, ClientHolder> cache = new ConcurrentHashMap<>();
    private final TokenProvider tokenProvider;
    private final HttpClientFactory httpClientFactory;
    private final Clock clock;

    public KubeClientFactory(TokenProvider eksTokenProvider) {
        this(eksTokenProvider, OkHttpClientFactory::createSecureClient, Clock.systemUTC());
    }

    KubeClientFactory(TokenProvider tokenProvider, HttpClientFactory httpClientFactory, Clock clock) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.httpClientFactory = Objects.requireNonNull(httpClientFactory);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Devuelve un ApiClient listo para usar contra el cluster indicado, con un token válido para esas credenciales.
     */
    public ApiClient forCluster(KubeConfig kubeConfig, AwsCredentials credentials) {
        String key = kubeConfig.getClusterName();
        String fingerprint = fingerprint(credentials);
        ClientHolder holder = cache.compute(key, (k, current) -> {
            try {
                if (current == null || !current.matches(kubeConfig, fingerprint)) {
                    if (current != null) {
                        log.info("Cambiaron las credenciales o el endpoint del cluster {}: se crea un cliente nuevo",
                                 key);
                        close(current);
                    }
                    return buildNewClient(key, kubeConfig, credentials, fingerprint);
                }
                if (current.tokenExpired(clock.instant())) {
                    log.info("Renovando token de EKS del cluster {}", key);
                    current.client.setApiKey(tokenProvider.getToken(credentials));
                    current.expiresAt = clock.instant().plus(TOKEN_TTL);
                }
                return current;
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo obtener un token de EKS para el cluster " + key
                                                        + ": " + e.getMessage(), e);
            }
        });

        return holder.client;
    }

    /**
     * Invalida el cliente en cache para el cluster (fuerza reconstrucción en siguiente llamada).
     */
    public void invalidate(String clusterName) {
        ClientHolder removed = cache.remove(clusterName);
        if (removed != null) {
            close(removed);
        }
    }

    /**
     * Limpia y cierra todos los clientes cacheados.
     */
    public void shutdown() {
        cache.values().forEach(KubeClientFactory::close);
        cache.clear();
    }

    private ClientHolder buildNewClient(String key, KubeConfig kubeConfig, AwsCredentials credentials,
                                        String fingerprint) throws Exception {
        String token = tokenProvider.getToken(credentials);

        ClientHolder holder = new ClientHolder();
        holder.serverUrl = kubeConfig.getServerUrl();
        holder.caCertBytes = kubeConfig.getCaCertBytes();
        holder.credentialsFingerprint = fingerprint;
        holder.expiresAt = clock.instant().plus(TOKEN_TTL);

        OkHttpClient.Builder http = httpClientFactory.create(kubeConfig.getCaCertBytes()).newBuilder();
        // Primero en la cadena: ve la respuesta final de toda llamada (listados, recursos y streams de logs)
        http.interceptors().add(0, chain -> {
            Response response = chain.proceed(chain.request());
            if (response.code() == 401) {
                log.warn("El cluster {} respondió 401 (token o credenciales vencidas): se regenera el token en el "
                                 + "siguiente intento", key);
                holder.expiresAt = null;
            }
            return response;
        });

        ApiClient client = new ApiClient();
        client.setBasePath(kubeConfig.getServerUrl());
        client.setApiKeyPrefix("Bearer");
        client.setApiKey(token);
        client.setVerifyingSsl(true);
        client.setHttpClient(http.build());
        holder.client = client;
        return holder;
    }

    private static void close(ClientHolder holder) {
        OkHttpClient http = holder.client.getHttpClient();
        if (http != null && http.connectionPool() != null) {
            http.connectionPool().evictAll();
        }
    }

    /**
     * Huella de las credenciales para saber si cambiaron sin guardar otra copia de los secretos.
     */
    static String fingerprint(AwsCredentials credentials) {
        if (credentials == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : new String[]{credentials.getAccessKey(), credentials.getSecretKey(),
                    credentials.getSessionToken()}) {
                digest.update(String.valueOf(part).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /**
     * Crea el OkHttpClient que confía en la CA del cluster (reemplazable en tests).
     */
    @FunctionalInterface
    interface HttpClientFactory {
        OkHttpClient create(byte[] caCertBytes) throws Exception;
    }

    private static final class ClientHolder {
        ApiClient client;
        String serverUrl;
        byte[] caCertBytes;
        String credentialsFingerprint;
        // null = hay que regenerar el token (p. ej. tras un 401). volatile: lo escribe el hilo de la llamada HTTP
        volatile Instant expiresAt;

        boolean matches(KubeConfig kubeConfig, String fingerprint) {
            return Objects.equals(serverUrl, kubeConfig.getServerUrl())
                    && Arrays.equals(caCertBytes, kubeConfig.getCaCertBytes())
                    && Objects.equals(credentialsFingerprint, fingerprint);
        }

        boolean tokenExpired(Instant now) {
            Instant expiry = expiresAt;
            return expiry == null || !now.isBefore(expiry);
        }
    }
}
