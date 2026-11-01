package co.com.bancolombia.kabrilla.infrastructure.adapters.k8s;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.LogsRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;
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
        CoreV1Api api = new CoreV1Api(client);

        // Usamos el builder del endpoint de logs con follow(true)
        var reqBuilder = api.readNamespacedPodLog(podName, namespace)
                .follow(true)
                .timestamps(timestamps)
                .container(Objects.requireNonNullElse(container, "movilidad-integration-product-container"));

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
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + " - " + response.message());
                }
                try (InputStream is = response.body().byteStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        onLine.accept(LogLine.now(line));
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
}

