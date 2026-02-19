package co.com.bancolombia.kabrilla.ui.viewmodel.pods;


import java.util.List;

import co.com.bancolombia.kabrilla.application.service.PodService;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;
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
                System.out.println("LLEGA AQUI 4444");
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
            error.set("No se pudieron cargar pods: " + e.getSource().getMessage());
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }
}

