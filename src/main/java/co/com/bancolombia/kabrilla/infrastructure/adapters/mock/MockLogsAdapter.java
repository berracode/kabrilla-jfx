package co.com.bancolombia.kabrilla.infrastructure.adapters.mock;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.LogsRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;

import lombok.extern.slf4j.Slf4j;

/**
 * Simula `kubectl logs -f`: reproduce el archivo del pod línea a línea con un retardo y, al llegar al final,
 * queda escuchando: las líneas que agregues al archivo (a mano o con un script) aparecen en la app.
 */
@Slf4j
public class MockLogsAdapter implements LogsRepository {

    private static final long POLL_INTERVAL_MS = 1000;

    private final Path logsRoot;
    private final long lineDelayMs;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "mock-logs");
        t.setDaemon(true);
        return t;
    });

    public MockLogsAdapter(Path logsRoot, long lineDelayMs) {
        this.logsRoot = logsRoot;
        this.lineDelayMs = lineDelayMs;
    }

    @Override
    public Closeable streamPodLogs(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                   String podName, String container, boolean timestamps, Integer tailLines,
                                   Integer sinceSeconds, Consumer<LogLine> onLine,
                                   Consumer<Throwable> onError) throws Exception {
        Path namespaceDir = logsRoot.resolve(MockFiles.shortClusterName(kubeConfig)).resolve(namespace);
        Path logFile = findPodFile(namespaceDir, podName)
                .orElseThrow(() -> new FileNotFoundException(
                        "No existe archivo de logs para el pod " + podName + " en " + namespaceDir));
        log.info("[MOCK] Siguiendo logs de {}", logFile.toAbsolutePath());

        Future<?> task = executor.submit(() -> {
            int emitted = 0;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    List<String> lines = readLines(logFile);
                    if (lines.size() < emitted) {
                        emitted = lines.size(); // el archivo se reescribió más corto: seguimos desde su final
                    }
                    for (; emitted < lines.size(); emitted++) {
                        onLine.accept(LogLine.now(lines.get(emitted)));
                        if (lineDelayMs > 0) {
                            Thread.sleep(lineDelayMs);
                        }
                    }
                    Thread.sleep(POLL_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                // Cancelado desde el Closeable: fin normal del stream
            } catch (Exception e) {
                onError.accept(e);
            }
        });

        return () -> task.cancel(true);
    }

    private Optional<Path> findPodFile(Path namespaceDir, String podName) throws Exception {
        for (Path deploymentDir : MockFiles.list(namespaceDir)) {
            for (Path file : MockFiles.list(deploymentDir)) {
                if (MockFiles.isLogFile(file) && MockFiles.podName(file).equals(podName)) {
                    return Optional.of(file);
                }
            }
        }
        return Optional.empty();
    }

    private List<String> readLines(Path file) throws Exception {
        // Lectura tolerante a bytes no UTF-8 (logs reales a veces los traen)
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8).lines().toList();
    }
}
