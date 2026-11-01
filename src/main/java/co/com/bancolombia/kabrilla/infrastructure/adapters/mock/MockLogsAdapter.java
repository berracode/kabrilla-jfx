package co.com.bancolombia.kabrilla.infrastructure.adapters.mock;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

import lombok.extern.slf4j.Slf4j;

/**
 * En vivo simula `kubectl logs -f`: reproduce el archivo del pod línea a línea con un retardo y, al llegar al
 * final, queda escuchando: las líneas que agregues al archivo (a mano o con un script) aparecen en la app.
 * <p>
 * Para consultas por rango el archivo hace de "log que conserva Kubernetes": la última línea tiene la hora de
 * arranque de la app y cada anterior {@code historyStepMs} menos. {@code <pod>.previous.log} es el container
 * anterior (previous=true).
 */
@Slf4j
public class MockLogsAdapter implements LogsRepository {

    private static final long POLL_INTERVAL_MS = 1000;
    private static final long DEFAULT_HISTORY_STEP_MS = 2000;
    private static final String PREVIOUS_SUFFIX = ".previous";

    private final Path logsRoot;
    private final long lineDelayMs;
    private final long historyStepMs;
    // Fijo durante la sesión para que las consultas por rango sean consistentes entre sí
    private final Instant historyEnd = Instant.now();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "mock-logs");
        t.setDaemon(true);
        return t;
    });

    public MockLogsAdapter(Path logsRoot, long lineDelayMs) {
        this(logsRoot, lineDelayMs, DEFAULT_HISTORY_STEP_MS);
    }

    public MockLogsAdapter(Path logsRoot, long lineDelayMs, long historyStepMs) {
        this.logsRoot = logsRoot;
        this.lineDelayMs = lineDelayMs;
        this.historyStepMs = historyStepMs;
    }

    @Override
    public LogFetchResult fetchPodLogs(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                       String podName, String container, LogRange range, int maxLines)
            throws Exception {
        Path namespaceDir = logsRoot.resolve(MockFiles.shortClusterName(kubeConfig)).resolve(namespace);
        String fileName = range.previous() ? podName + PREVIOUS_SUFFIX : podName;
        Path logFile = findPodFile(namespaceDir, fileName).orElseThrow(() -> range.previous()
                ? new IllegalStateException("[MOCK] previous terminated container \"" + container + "\" in pod \""
                                                    + podName + "\" not found (crea " + fileName + ".log)")
                : new FileNotFoundException("No existe archivo de logs para el pod " + podName));
        log.info("[MOCK] Consulta por rango {} en {}", range, logFile.toAbsolutePath());

        List<String> lines = readLines(logFile);
        LogRangeCollector collector = new LogRangeCollector(range, maxLines);
        for (int i = 0; i < lines.size(); i++) {
            Instant timestamp = historyEnd.minusMillis((lines.size() - 1L - i) * historyStepMs);
            if (!collector.offer(new LogLine(timestamp, lines.get(i)))) {
                break;
            }
        }
        return collector.result();
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
