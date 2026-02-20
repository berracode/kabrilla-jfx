package co.com.bancolombia.kabrilla.ui.viewmodel.logs;


import java.io.Closeable;
import java.util.Objects;

import co.com.bancolombia.kabrilla.application.service.LogsService;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;

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

public class LogsViewModel {

    private static final int MAX_LINES = 5000;

    private final LogsService service;

    @Getter
    private final ObservableList<LogLine> lines = FXCollections.observableArrayList();
    private final BooleanProperty following = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty();

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
                        lines.add(line);
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
}

