package co.com.bancolombia.kabrilla.ui.controller.debug;


import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceStateViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateDebugController {
    private static final Logger log = LoggerFactory.getLogger(StateDebugController.class);
    private final ClusterNamespaceStateViewModel clusterNamespaceState;
    private final DeploymentStateViewModel deploymentStateViewModel;
    @FXML
    private Text clusterText;
    @FXML
    private Text namespaceText;
    @FXML
    private Text deploymentText;

    public StateDebugController(ClusterNamespaceStateViewModel clusterNamespaceState,
                                DeploymentStateViewModel deploymentStateViewModel) {
        this.clusterNamespaceState = clusterNamespaceState;
        this.deploymentStateViewModel = deploymentStateViewModel;
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
    }
}
