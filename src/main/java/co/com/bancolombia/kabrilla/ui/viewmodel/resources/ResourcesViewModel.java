package co.com.bancolombia.kabrilla.ui.viewmodel.resources;

import java.util.List;

import co.com.bancolombia.kabrilla.application.service.KubeResourceService;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.KubeResource;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;
import co.com.bancolombia.kabrilla.ui.context.DeploymentContext;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

/**
 * Estado de una pestaña de recursos (Service, HPA, ConfigMap...) de un deployment.
 */
@Slf4j
public class ResourcesViewModel {

    private final KubeResourceService service;
    private final ResourceKind kind;
    private final ObservableList<KubeResource> resources = FXCollections.observableArrayList();
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty loaded = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty();

    public ResourcesViewModel(KubeResourceService service, ResourceKind kind) {
        this.service = service;
        this.kind = kind;
    }

    public ResourceKind kind() {
        return kind;
    }

    public ObservableList<KubeResource> resources() {
        return resources;
    }

    public ReadOnlyBooleanProperty loadingProperty() {
        return loading;
    }

    /**
     * true después de la primera carga terminada (con o sin error).
     */
    public ReadOnlyBooleanProperty loadedProperty() {
        return loaded;
    }

    public ReadOnlyStringProperty errorProperty() {
        return error;
    }

    public void load(DeploymentContext ctx) {
        if (loading.get()) {
            return;
        }
        loading.set(true);
        error.set(null);

        AwsCredentials credentials = ctx.awsCredentials(); // en el hilo FX: lee el estado de la UI
        Task<List<KubeResource>> task = new Task<>() {
            @Override
            protected List<KubeResource> call() throws Exception {
                return service.findByDeployment(ctx.kubeConfig(), credentials, ctx.namespace(),
                                                ctx.deploymentName(), kind);
            }
        };
        task.setOnSucceeded(e -> {
            resources.setAll(task.getValue());
            finish();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            log.error("No se pudieron cargar {} de {}/{}/{}", kind.k8sKind(), ctx.clusterName(), ctx.namespace(),
                      ctx.deploymentName(), ex);
            resources.clear();
            error.set(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            finish();
        });

        Thread thread = new Thread(task, "load-" + kind.name().toLowerCase());
        thread.setDaemon(true);
        thread.start();
    }

    private void finish() {
        loading.set(false);
        loaded.set(true);
    }
}
