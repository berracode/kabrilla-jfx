package co.com.bancolombia.kabrilla.ui.viewmodel.logs;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import co.com.bancolombia.kabrilla.application.service.LogsService;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;
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
            String display = line.timestamp() + " - " + line.text();
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
                        error.set("Stream de logs falló: " + (t.getMessage() != null ? t.getMessage() : t.getClass()
                                .getSimpleName()));
                        following.set(false);
                    })
            );
        } catch (Exception e) {
            error.set("No se pudo abrir el stream: " + e.getMessage());
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
    }
}
