package co.com.bancolombia.kabrilla.ui.controller.debug;


import java.nio.charset.StandardCharsets;

import co.com.bancolombia.kabrilla.ui.viewmodel.cluster.KubeClusterItemViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.credentials.CredentialsItemViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.AppContext;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceStateViewModel;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StateDebugController {
    private static final Logger log = LoggerFactory.getLogger(StateDebugController.class);
    private final ClusterNamespaceStateViewModel clusterNamespaceState;
    private final DeploymentStateViewModel deploymentStateViewModel;
    private final AppContext context;
    @FXML
    private Text clusterText;
    @FXML
    private Text namespaceText;
    @FXML
    private Text deploymentText;

    @FXML
    private ListView<String> credentialsListView;
    @FXML
    private ListView<String> kubeConfigListView;


    public StateDebugController(ClusterNamespaceStateViewModel clusterNamespaceState,
                                DeploymentStateViewModel deploymentStateViewModel, AppContext context) {
        this.clusterNamespaceState = clusterNamespaceState;
        this.deploymentStateViewModel = deploymentStateViewModel;
        this.context = context;
    }

    @FXML
    public void initialize() {
        // Inicializa texto
        clusterText.setText("Clusters: " + clusterNamespaceState.getObservableClusters().size());
        namespaceText.setText("Namespaces: —");
        deploymentText.setText("Deployment: —");

        // Escucha cambios en el deployment seleccionado
        deploymentStateViewModel.selectedDeploymentProperty().addListener((obs, old, selected) -> {
            Platform.runLater(() -> {
                if (selected != null) {
                    clusterText.setText("Cluster: " + selected.getClusterName());
                    namespaceText.setText("Namespace: " + selected.getNamespace());
                    deploymentText.setText("Deployment: " + selected.getName());
                    log.info("⚙️ Deployment seleccionado: {}/{}/{}", selected.getClusterName(), selected.getNamespace(),
                             selected.getName());
                }
            });
        });


        context.getAppState().getCredentials().addListener((ListChangeListener<CredentialsItemViewModel>) change -> {
            updateCredentialsList();
        });

        context.getAppState().getKubeConfig().addListener((ListChangeListener<KubeClusterItemViewModel>) change -> {
            updateKubeConfigList();
        });

        updateCredentialsList();
        updateKubeConfigList();
    }

    private void updateCredentialsList() {
        Platform.runLater(() -> {
            credentialsListView.getItems().clear();
            for (CredentialsItemViewModel cred : context.getAppState().getCredentials()) {
                credentialsListView.getItems().add(formatCredential(cred));
            }
        });
    }

    private void updateKubeConfigList() {
        Platform.runLater(() -> {
            kubeConfigListView.getItems().clear();
            for (var kube : context.getAppState().getKubeConfig()) {
                kubeConfigListView.getItems().add(formatKubeConfig(kube));
            }
        });
    }

    private String formatCredential(CredentialsItemViewModel cred) {
        return String.format("Cluster: %s\nAccessKey: %s\nSecretKey: %s\nSessionToken: %s",
                             cred.clusterName(), cred.accessKey(), cred.secretKey(), cred.sessionToken());
    }

    private String formatKubeConfig(KubeClusterItemViewModel kube) {
        return String.format("ClusterName: %s\nCA: %s\nURL: %s",
                             kube.clusterName(), new String(kube.caCertBytes(), StandardCharsets.UTF_8),
                             kube.serverUrl());
    }

}

