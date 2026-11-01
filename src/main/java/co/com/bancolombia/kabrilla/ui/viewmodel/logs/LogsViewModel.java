package co.com.bancolombia.kabrilla.ui.viewmodel.logs;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import co.com.bancolombia.kabrilla.application.service.LogsService;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;
import co.com.bancolombia.kabrilla.ui.services.UiLogFeederService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

    private final LogsService service;

    // Si la UI no alcanza a consumir, el hilo lector se bloquea (contrapresión) en vez de perder líneas
    @Getter
    private final BlockingQueue<LogLine> logBuffer = new LinkedBlockingQueue<>(BUFFER_CAPACITY);
    private final BooleanProperty following = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty();

    // Identifica el stream vigente: líneas y errores de streams ya cancelados se ignoran
    private final AtomicLong currentSession = new AtomicLong();

    private UiLogFeederService uiLogFeederService;
    private Runnable onCleared;
    private Closeable subscription;

    // Cache para evitar reabrir el mismo stream
    private String lastCluster, lastNamespace, lastDeployment, lastPod, lastContainer;

    public LogsViewModel(LogsService service) {
        this.service = service;
    }

    public ReadOnlyBooleanProperty followingProperty() {
        return following;
    }

    public ReadOnlyStringProperty errorProperty() {
        return error;
    }

    /**
     * Descarta las líneas pendientes y pide a la vista limpiar lo mostrado.
     */
    public void clear() {
        logBuffer.clear();
        if (onCleared != null) {
            onCleared.run();
        }
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

        if (uiLogFeederService != null) {
            log.info("Arrancando hilo consumidor");
            uiLogFeederService.restart(); // restart() funciona aunque se haya cancelado antes
        }
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
        if (uiLogFeederService != null && uiLogFeederService.isRunning()) {
            uiLogFeederService.cancel();
        }
        following.set(false);
    }

    // Aquí es donde realmente se engancha la UI al flujo
    public void setOnLinesAppended(Consumer<String> onLinesAppended) {
        // El feeder toma lotes del buffer y llama al callback que actualiza el RichTextFX en la Vista
        this.uiLogFeederService = new UiLogFeederService(
                this.logBuffer,
                onLinesAppended
        );
    }

    public void setOnCleared(Runnable onCleared) {
        this.onCleared = onCleared;
    }
}
