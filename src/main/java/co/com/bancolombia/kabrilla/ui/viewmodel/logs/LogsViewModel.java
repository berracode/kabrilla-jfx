package co.com.bancolombia.kabrilla.ui.viewmodel.logs;


import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogsViewModel {

    private static final int MAX_LINES = 1_000_000;

    private final LogsService service;
    @Getter
    private final ObservableList<LogLine> lines = FXCollections.observableArrayList();

    @Getter
    private final BlockingQueue<LogLine> logBuffer = new LinkedBlockingQueue<>(5000);
    private final BooleanProperty following = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty();
    private UiLogFeederService uiLogFeederService;
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

    public void clear() {
        Platform.runLater(lines::clear);
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
        error.set(null);
        following.set(true);

        lastCluster = cluster;
        lastNamespace = namespace;
        lastDeployment = deployment;
        lastPod = podName;
        lastContainer = container;

        try {
            subscription = service.followPodLogs(
                    kubeConfig, creds, namespace, podName, container,
                    timestamps, tailLines, sinceSeconds,
                    // onLine
                    line -> Platform.runLater(() -> {
                        //lines.add(line);
                        logBuffer.add(line);
                        if (lines.size() > MAX_LINES) {
                            lines.remove(0, lines.size() - MAX_LINES);
                        }

                    }),
                    // onError
                    t -> Platform.runLater(() -> {
                        error.set("Stream de logs falló: " + (t.getMessage() != null ? t.getMessage() : t.getClass()
                                .getSimpleName()));
                        following.set(false);
                    })
            );
        } catch (Exception e) {
            error.set("No se pudo abrir el stream: " + e.getMessage());
            following.set(false);
        }

        // 2. Arranca el hilo consumidor (JavaFX Service en Background Pool)
        if (uiLogFeederService != null && !uiLogFeederService.isRunning()) {
            log.info("Arrancando hilo consumidor");
            uiLogFeederService.restart(); // Usamos restart() por si ya se había cancelado antes
        }
    }

    /**
     * Detiene el stream actual (si existe).
     */
    public synchronized void stop() {
        if (subscription != null) {
            try {
                subscription.close();
            } catch (Exception ignored) {
            }
            subscription = null;
        }
        following.set(false);
    }


    //NUEVA

    // Aquí es donde realmente se engancha la UI al flujo
    public void setOnLinesAppended(Consumer<String> onLinesAppended) {
        // Instanciamos TU UiLogFeederService pasándole el buffer real del backend
        // y el callback que actualizará el RichTextFX en la Vista
        this.uiLogFeederService = new UiLogFeederService(
                this.logBuffer,
                onLinesAppended
        );
    }
}

