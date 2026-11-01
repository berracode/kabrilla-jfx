package co.com.bancolombia.kabrilla.infrastructure.config.k8s;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.infrastructure.adapters.OkHttpClientFactory;
import co.com.bancolombia.kabrilla.infrastructure.config.tokeneks.TokenProvider;

import io.kubernetes.client.openapi.ApiClient;
import okhttp3.OkHttpClient;

public class KubeClientFactory {

    // TTL estimado para token de EKS 15 min
    private static final Duration TOKEN_TTL = Duration.ofMinutes(14);
    private final ConcurrentMap<String, ClientHolder> cache = new ConcurrentHashMap<>();
    private final TokenProvider tokenProvider;

    public KubeClientFactory(TokenProvider eksTokenProvider) {
        this.tokenProvider = Objects.requireNonNull(eksTokenProvider);
    }

    /**
     * Devuelve un ApiClient configurado y listo para usar contra el cluster indicado.
     * Si existe en cache y el token está vigente, lo reutiliza.
     * Si el token expiró, lo refresca en caliente.
     */
    public ApiClient forCluster(KubeConfig kubeConfig, AwsCredentials credentials) {
        String key = kubeConfig.getClusterName(); // o kubeConfig.getServerUrl()
        ClientHolder holder = cache.compute(key, (k, current) -> {
            try {
                if (current == null) {
                    return buildNewClient(kubeConfig, credentials);
                }
                if (tokenExpired(current)) {
                    // Refrescamos token en caliente
                    String newToken = tokenProvider.getToken(credentials);
                    current.client.setApiKey(newToken);
                    current.expiresAt = LocalDateTime.now().plus(TOKEN_TTL);
                }
                if (!Objects.equals(current.serverUrl, kubeConfig.getServerUrl()) ||
                        !bytesEquals(current.caCertBytes, kubeConfig.getCaCertBytes())) {
                    return buildNewClient(kubeConfig, credentials);
                }
                return current;
            } catch (Exception e) {
                throw new RuntimeException("No se pudo construir/refrescar ApiClient para cluster: " + key, e);
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
            OkHttpClient http = removed.client.getHttpClient();
            if (http != null && http.connectionPool() != null) {
                http.connectionPool().evictAll();
            }
        }
    }

    /**
     * Limpia y cierra todos los clientes cacheados.
     */
    public void shutdown() {
        cache.values().forEach(holder -> {
            OkHttpClient http = holder.client.getHttpClient();
            if (http != null && http.connectionPool() != null) {
                http.connectionPool().evictAll();
            }
        });
        cache.clear();
    }

    private ClientHolder buildNewClient(KubeConfig kubeConfig, AwsCredentials credentials) throws Exception {
        String token = tokenProvider.getToken(credentials);

        ApiClient client = new ApiClient();
        client.setBasePath(kubeConfig.getServerUrl());
        client.setApiKeyPrefix("Bearer");
        client.setApiKey(token);

        client.setVerifyingSsl(true);

        OkHttpClient secureClient = OkHttpClientFactory.createSecureClient(kubeConfig.getCaCertBytes());
        client.setHttpClient(secureClient);

        client.getHttpClient().newBuilder()
                .readTimeout(java.time.Duration.ofSeconds(30))
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        ClientHolder holder = new ClientHolder();
        holder.client = client;
        holder.serverUrl = kubeConfig.getServerUrl();
        holder.caCertBytes = kubeConfig.getCaCertBytes();
        holder.expiresAt = LocalDateTime.now().plus(TOKEN_TTL);
        return holder;
    }

    private boolean tokenExpired(ClientHolder holder) {
        return holder.expiresAt == null || LocalDateTime.now().isAfter(holder.expiresAt);
    }

    private boolean bytesEquals(byte[] a, byte[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static final class ClientHolder {
        ApiClient client;
        String serverUrl;
        byte[] caCertBytes;
        LocalDateTime expiresAt;
    }
}

