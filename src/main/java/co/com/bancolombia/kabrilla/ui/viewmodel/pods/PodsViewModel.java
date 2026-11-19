package co.com.bancolombia.kabrilla.ui.viewmodel.pods;


import java.util.List;

import co.com.bancolombia.kabrilla.application.service.PodService;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;
import co.com.bancolombia.kabrilla.ui.context.DeploymentContext;
import co.com.bancolombia.kabrilla.ui.services.ErrorMessages;

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

@Slf4j
public class PodsViewModel {
    private final ObservableList<Pod> pods = FXCollections.observableArrayList();
    private final PodService podService;

    // estado UI opcional
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty();

    public PodsViewModel(PodService podService) {
        this.podService = podService;
    }

    public ObservableList<Pod> pods() {
        return pods;
    }

    public ReadOnlyBooleanProperty loadingProperty() {
        return loading;
    }

    public ReadOnlyStringProperty errorProperty() {
        return error;
    }

    public void loadPods(DeploymentContext ctx) {
        loading.set(true);
        error.set(null);


        Task<List<Pod>> task = new Task<>() {
            @Override
            protected List<Pod> call() throws Exception {
                var credentials = AwsCredentials.builder().accessKey(ctx.credentials().accessKey())
                        .secretKey(ctx.credentials().secretKey())
                        .clusterName(ctx.credentials().clusterName())
                        .sessionToken(ctx.credentials().sessionToken()).build();
                return podService.getPods(ctx.kubeConfig(), credentials, ctx.namespace(), ctx.deploymentName());
            }
        };

        task.setOnSucceeded(e -> {
            List<Pod> result = task.getValue();

            loading.set(false);
            pods.clear();
            pods.addAll(result);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            log.error("No se pudieron cargar los pods de {}/{}/{}", ctx.clusterName(),
                      ctx.namespace(), ctx.deploymentName(), ex);
            loading.set(false);
            error.set("No se pudieron cargar pods: " + ErrorMessages.describe(ex));
        });

        Thread thread = new Thread(task, "load-pods");
        thread.setDaemon(true);
        thread.start();
    }
}

