package co.com.bancolombia.kabrilla.ui.controller.usecases;

import java.util.List;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterStateViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateNamespaceController {


    private final ClusterService clusterService;
    private final ClusterNamespaceStateViewModel clusterState;

    @FXML
    private ComboBox<String> clusterComboBox;
    @FXML
    private TextField namespaceField;
    @FXML
    private DialogPane dialogPane;

    public CreateNamespaceController(ClusterService clusterService, ClusterNamespaceStateViewModel clusterState) {
        this.clusterService = clusterService;
        this.clusterState = clusterState;
    }


    @FXML
    public void initialize() {
        List<String> clusterNames = clusterState.getObservableClusters().stream()
                .map(ClusterStateViewModel::getName)
                .toList();
        clusterComboBox.getItems().addAll(clusterNames);
        Platform.runLater(() -> {
            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            if (okButton != null) {
                okButton.disableProperty().bind(
                        clusterComboBox.getSelectionModel().selectedItemProperty().isNull()
                                .or(namespaceField.textProperty().isEmpty())
                );
            }
        });
    }


    public String getClusterName() {
        return clusterComboBox.getValue();
    }

    public String getNamespace() {
        return namespaceField.getText().trim();
    }

    public void saveCluster() {
        String clusterName = getClusterName();
        String namespace = getNamespace();

        if (!clusterName.isEmpty() && !namespace.isEmpty()) {
            clusterService.createNamespace(clusterName, namespace);
            clusterState.addNamespaceToCluster(clusterName, new Namespace(namespace));
            log.info("Namespace {} registrado en el cluster {}", namespace, clusterName);
        }
    }
}
