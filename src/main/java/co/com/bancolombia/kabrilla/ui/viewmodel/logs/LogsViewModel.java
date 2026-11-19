package co.com.bancolombia.kabrilla.ui.viewmodel.logs;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import co.com.bancolombia.kabrilla.application.service.LogsService;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogFetchResult;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogRange;
import co.com.bancolombia.kabrilla.ui.services.ErrorMessages;
import co.com.bancolombia.kabrilla.ui.services.UiLogFeederService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Flujo: hilo lector del adaptador → {@link #logBuffer} → {@link UiLogFeederService} (lotes) → vista.
 * Los métodos públicos se llaman desde el hilo de JavaFX.
 */
@Slf4j
public class LogsViewModel {

    private static final int BUFFER_CAPACITY = 5000;
    // Lo que se puede volver a filtrar. La vista muestra como máximo MAX_RENDERED_LINES.
    private static final int MAX_HISTORY_LINES = 50_000;
    public static final int MAX_RENDERED_LINES = 5000;
    // Hora del pod (o de recepción en mock en vivo) en la zona horaria del equipo
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    // Diferencia aceptable entre el "desde" pedido y la primera línea antes de avisar que falta historia
    private static final Duration COVERAGE_TOLERANCE = Duration.ofMinutes(1);

    private final LogsService service;
    private final LogHistory history = new LogHistory(MAX_HISTORY_LINES);
    private final IntegerProperty totalLines = new SimpleIntegerProperty();
    private final IntegerProperty visibleLines = new SimpleIntegerProperty();
    private LogFilter filter = LogFilter.none();
    private LogView view;

    // Si la UI no alcanza a consumir, el hilo lector se bloquea (contrapresión) en vez de perder líneas
    @Getter
    private final BlockingQueue<LogLine> logBuffer = new LinkedBlockingQueue<>(BUFFER_CAPACITY);
    private final BooleanProperty following = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty();
    private final StringProperty info = new SimpleStringProperty();
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    // Identifica el stream vigente: líneas y errores de streams ya cancelados se ignoran
    private final AtomicLong currentSession = new AtomicLong();

    private final UiLogFeederService uiLogFeederService;
    private Closeable subscription;

    // Cache para evitar reabrir el mismo stream
    private String lastCluster, lastNamespace, lastDeployment, lastPod, lastContainer;

    public LogsViewModel(LogsService service) {
        this.service = service;
        // El feeder toma lotes del buffer y los entrega en el hilo de JavaFX
        this.uiLogFeederService = new UiLogFeederService(logBuffer, this::onBatch);
    }

    public void setView(LogView view) {
        this.view = view;
    }

    public ReadOnlyIntegerProperty totalLinesProperty() {
        return totalLines;
    }

    public ReadOnlyIntegerProperty visibleLinesProperty() {
        return visibleLines;
    }

    public LogFilter getFilter() {
        return filter;
    }

    /**
     * Aplica un filtro nuevo sobre todo el historial y redibuja la vista.
     */
    public void setFilter(LogFilter newFilter) {
        this.filter = newFilter;
        List<String> rendered = new ArrayList<>();
        for (LogEntry entry : history.entries()) {
            boolean visible = filter.matches(entry);
            entry.setVisible(visible);
            if (visible) {
                rendered.addAll(entry.lines());
            }
        }
        // Solo las últimas MAX_RENDERED_LINES; el resto sigue en el historial
        int from = Math.max(0, rendered.size() - MAX_RENDERED_LINES);
        if (view != null) {
            view.reset(rendered.subList(from, rendered.size()));
        }
        updateCounts();
    }

    /**
     * Lote nuevo del stream (hilo de JavaFX): se agrega al historial y se pinta lo que pase el filtro.
     */
    private void onBatch(List<LogLine> batch) {
        List<String> toAppend = new ArrayList<>();
        for (LogLine line : batch) {
            String display = display(line);
            LogEntry entry = history.add(line.text(), display);
            if (entry.isVisible()) {
                toAppend.add(display); // continuación de una entrada ya visible
            } else if (filter.matches(entry)) {
                // Entrada nueva, o continuación que hizo coincidir la entrada: se pinta completa
                entry.setVisible(true);
                toAppend.addAll(entry.lines());
            }
        }
        if (view != null && !toAppend.isEmpty()) {
            view.append(toAppend);
        }
        updateCounts();
    }

    /**
     * Consulta un rango de tiempo (sin follow) y reemplaza el historial con el resultado. Detiene el stream en vivo.
     */
    public synchronized void loadRange(KubeConfig kubeConfig, AwsCredentials creds, String namespace, String podName,
                                       String container, LogRange range) {
        stop();
        clear();
        long session = currentSession.get();
        error.set(null);
        info.set(null);
        loading.set(true);

        Task<LogFetchResult> task = new Task<>() {
            @Override
            protected LogFetchResult call() throws Exception {
                return service.fetchPodLogs(kubeConfig, creds, namespace, podName, container, range,
                                            MAX_HISTORY_LINES);
            }
        };
        task.setOnSucceeded(e -> {
            if (currentSession.get() != session) {
                return; // el usuario cambió de pod/rango mientras se consultaba
            }
            LogFetchResult result = task.getValue();
            history.clear();
            for (LogLine line : result.lines()) {
                history.add(line.text(), display(line));
            }
            setFilter(filter); // redibuja aplicando el filtro actual
            info.set(describe(range, result));
            loading.set(false);
        });
        task.setOnFailed(e -> {
            if (currentSession.get() != session) {
                return;
            }
            Throwable ex = task.getException();
            log.error("Falló la consulta de logs por rango de {}/{} container {} ({})", namespace, podName, container,
                      range, ex);
            error.set("No se pudo consultar el rango: " + ErrorMessages.describe(ex));
            loading.set(false);
        });
        Thread thread = new Thread(task, "logs-range");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Resumen para el usuario: cuántas líneas, y si Kubernetes no tenía todo el rango o se recortó el resultado.
     */
    static String describe(LogRange range, LogFetchResult result) {
        List<LogLine> lines = result.lines();
        if (lines.isEmpty()) {
            return "Sin logs en el rango. Kubernetes solo conserva el archivo actual del container"
                    + (range.previous() ? " anterior." : "; prueba \"Todo lo disponible\" o \"Container anterior\".");
        }
        List<String> parts = new ArrayList<>();
        parts.add(lines.size() + " líneas entre " + DISPLAY_TIME.format(lines.getFirst().timestamp())
                          + " y " + DISPLAY_TIME.format(lines.getLast().timestamp()));
        Instant first = lines.getFirst().timestamp();
        if (range.since() != null && first.isAfter(range.since().plus(COVERAGE_TOLERANCE))) {
            parts.add("Kubernetes conserva logs de este container solo desde " + DISPLAY_TIME.format(first));
        }
        if (result.truncated()) {
            parts.add("se muestran las primeras " + MAX_HISTORY_LINES + " líneas, acota el rango para ver el resto");
        }
        return String.join(" · ", parts);
    }

    private static String display(LogLine line) {
        return DISPLAY_TIME.format(line.timestamp()) + "  " + line.text();
    }

    private void updateCounts() {
        totalLines.set(history.lineCount());
        visibleLines.set(history.visibleLineCount());
    }

    public ReadOnlyBooleanProperty followingProperty() {
        return following;
    }

    public ReadOnlyStringProperty errorProperty() {
        return error;
    }

    /**
     * Limpia error e info (p. ej. antes de mostrar una validación de la vista).
     */
    public void clearMessages() {
        error.set(null);
        info.set(null);
    }

    /**
     * Mensaje informativo de la última consulta por rango (cobertura, recorte).
     */
    public ReadOnlyStringProperty infoProperty() {
        return info;
    }

    /**
     * true mientras corre una consulta por rango.
     */
    public ReadOnlyBooleanProperty loadingProperty() {
        return loading;
    }

    /**
     * Descarta las líneas pendientes y el historial, y limpia la vista.
     */
    public void clear() {
        logBuffer.clear();
        history.clear();
        if (view != null) {
            view.reset(List.of());
        }
        updateCounts();
    }

    /**
     * Inicia (o conmuta) el stream de logs al pod indicado.
     * Evita reabrir si ya está siguiendo exactamente el mismo contexto/pod.
     */
    public synchronized void startFollowing(
            KubeConfig kubeConfig,
            AwsCredentials creds,
            String cluster,
            String namespace,
            String deployment,
            String podName,
            String container,
            boolean timestamps,
            Integer tailLines,
            Integer sinceSeconds
    ) {
        if (following.get()
                && Objects.equals(cluster, lastCluster)
                && Objects.equals(namespace, lastNamespace)
                && Objects.equals(deployment, lastDeployment)
                && Objects.equals(podName, lastPod)
                && Objects.equals(container, lastContainer)) {
            return; // ya está siguiendo ese mismo stream
        }

        stop();                // cierra stream previo si había
        logBuffer.clear();     // no mezclar líneas del pod anterior
        error.set(null);
        info.set(null);
        following.set(true);

        lastCluster = cluster;
        lastNamespace = namespace;
        lastDeployment = deployment;
        lastPod = podName;
        lastContainer = container;

        long session = currentSession.incrementAndGet();
        try {
            subscription = service.followPodLogs(
                    kubeConfig, creds, namespace, podName, container,
                    timestamps, tailLines, sinceSeconds,
                    // onLine: corre en el hilo lector, la cola es thread-safe
                    line -> {
                        if (currentSession.get() != session) {
                            return;
                        }
                        try {
                            logBuffer.put(line);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // stream cancelado
                        }
                    },
                    // onError
                    t -> Platform.runLater(() -> {
                        if (currentSession.get() != session) {
                            return; // error de un stream que ya fue reemplazado o detenido
                        }
                        log.error("Falló el stream de logs de {}/{}/{} container {}", cluster, namespace, podName,
                                  container, t);
                        error.set("Stream de logs falló: " + ErrorMessages.describe(t));
                        following.set(false);
                    })
            );
        } catch (Exception e) {
            log.error("No se pudo abrir el stream de logs de {}/{}/{} container {}", cluster, namespace, podName,
                      container, e);
            error.set("No se pudo abrir el stream: " + ErrorMessages.describe(e));
            following.set(false);
            return;
        }

        log.info("Arrancando hilo consumidor");
        uiLogFeederService.restart(); // restart() funciona aunque se haya cancelado antes
    }

    /**
     * Detiene el stream actual (si existe) y el hilo consumidor.
     */
    public synchronized void stop() {
        currentSession.incrementAndGet();
        if (subscription != null) {
            try {
                subscription.close();
            } catch (Exception ignored) {
            }
            subscription = null;
        }
        if (uiLogFeederService.isRunning()) {
            uiLogFeederService.cancel();
        }
        following.set(false);
        loading.set(false); // una consulta por rango en curso queda descartada por la sesión
    }
}
