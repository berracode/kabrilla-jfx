package co.com.bancolombia.kabrilla.infrastructure.adapters.k8s;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.LogsRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogFetchResult;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogRange;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogRangeCollector;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeClientFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

@Slf4j
public class LogsAdapter implements LogsRepository {


    private final KubeClientFactory kubeClientFactory;
    private final ExecutorService executor;

    public LogsAdapter(KubeClientFactory kubeClientFactory) {
        this.kubeClientFactory = kubeClientFactory;
        // Hilos daemon: un stream abierto no debe impedir que la JVM termine al cerrar la app
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "k8s-logs");
            t.setDaemon(true);
            return t;
        });
    }


    @Override
    public Closeable streamPodLogs(
            KubeConfig kubeConfig,
            AwsCredentials credentials,
            String namespace,
            String podName,
            String container,
            boolean timestamps,
            Integer tailLines,
            Integer sinceSeconds,
            Consumer<LogLine> onLine,
            Consumer<Throwable> onError
    ) throws Exception {

        ApiClient client = kubeClientFactory.forCluster(kubeConfig, credentials);
        var reqBuilder = logRequest(client, namespace, podName, container)
                .follow(true)
                .timestamps(timestamps);
        if (tailLines != null) {
            reqBuilder.tailLines(tailLines);
        }
        if (sinceSeconds != null) {
            reqBuilder.sinceSeconds(sinceSeconds);
        }

        // buildCall(null) porque no usamos ProgressListener
        okhttp3.Call okCall = client.getHttpClient().newCall(reqBuilder.buildCall(null).request());

        // Tarea que abre y lee el stream
        Future<?> f = executor.submit(() -> {
            try (Response response = okCall.execute()) {
                ensureSuccess(response, podName);
                try (BufferedReader reader = reader(response)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        onLine.accept(timestamps ? parseTimestamped(line) : LogLine.now(line));
                    }
                }
            } catch (Throwable t) {
                // Si fue cancelado, OkHttp lanza IOException por cancelación: no es un error
                if (!okCall.isCanceled()) {
                    onError.accept(t);
                }
            }
        });

        // Devolvemos Closeable para cancelar
        return () -> {
            try {
                okCall.cancel();
            } catch (Exception ignored) {
            }
            f.cancel(true);
        };
    }

    @Override
    public LogFetchResult fetchPodLogs(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                       String podName, String container, LogRange range, int maxLines)
            throws Exception {
        ApiClient client = kubeClientFactory.forCluster(kubeConfig, credentials);
        var reqBuilder = logRequest(client, namespace, podName, container)
                .follow(false)
                .timestamps(true)
                .previous(range.previous());
        if (range.since() != null) {
            // La API no acepta "desde fecha": se pide en segundos (redondeando hacia atrás) y el collector recorta
            long seconds = Duration.between(range.since(), Instant.now()).toSeconds() + 1;
            if (seconds <= 0) {
                return new LogFetchResult(List.of(), false);
            }
            reqBuilder.sinceSeconds((int) Math.min(seconds, Integer.MAX_VALUE));
        }

        LogRangeCollector collector = new LogRangeCollector(range, maxLines);
        okhttp3.Call okCall = client.getHttpClient().newCall(reqBuilder.buildCall(null).request());
        // Cerrar la respuesta a mitad de lectura corta la descarga (al pasar el "hasta" o llenar el máximo)
        try (Response response = okCall.execute()) {
            ensureSuccess(response, podName);
            try (BufferedReader reader = reader(response)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!collector.offer(parseTimestamped(line))) {
                        break;
                    }
                }
            }
        }
        return collector.result();
    }

    private static CoreV1Api.APIreadNamespacedPodLogRequest logRequest(ApiClient client, String namespace,
                                                                      String podName, String container) {
        var reqBuilder = new CoreV1Api(client).readNamespacedPodLog(podName, namespace);
        // Obligatorio si el pod tiene varios containers (p. ej. app + istio-proxy); con uno solo se puede omitir
        if (container != null) {
            reqBuilder.container(container);
        }
        return reqBuilder;
    }

    private static BufferedReader reader(Response response) {
        InputStream is = response.body().byteStream();
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    /**
     * Con timestamps=true el kubelet antepone la hora RFC3339 en UTC: "2026-11-01T14:32:10.123456789Z mensaje".
     */
    static LogLine parseTimestamped(String raw) {
        int space = raw.indexOf(' ');
        if (space > 0) {
            try {
                return new LogLine(Instant.parse(raw.substring(0, space)), raw.substring(space + 1));
            } catch (DateTimeParseException ignored) {
                // Línea sin prefijo de tiempo: se usa la hora de recepción
            }
        }
        return LogLine.now(raw);
    }

    private static void ensureSuccess(Response response, String podName) throws IOException {
        if (response.isSuccessful()) {
            return;
        }
        String body = response.body() != null ? response.body().string() : "";
        throw new IOException(K8sErrors.message(response.code(), body, "leer los logs del pod " + podName));
    }
}
