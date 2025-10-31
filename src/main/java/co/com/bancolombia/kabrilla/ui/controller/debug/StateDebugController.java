package co.com.bancolombia.kabrilla.ui.controller.debug;


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

        updateCredentialsList();
    }

    private void updateCredentialsList() {
        Platform.runLater(() -> {
            credentialsListView.getItems().clear();
            for (CredentialsItemViewModel cred : context.getAppState().getCredentials()) {
                credentialsListView.getItems().add(formatCredential(cred));
            }
        });
    }

    private String formatCredential(CredentialsItemViewModel cred) {
        return String.format("Cluster: %s\nAccessKey: %s\nSecretKey: %s\nSessionToken: %s",
                             cred.clusterName(), cred.accessKey(), cred.secretKey(), cred.sessionToken());
    }

}

