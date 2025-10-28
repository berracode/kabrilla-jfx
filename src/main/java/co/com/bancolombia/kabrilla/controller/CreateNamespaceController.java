package co.com.bancolombia.kabrilla.controller;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.state.namespace.ClusterNamespaceState;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;

public class CreateNamespaceController {


    private final ClusterService clusterService;
    private final ClusterNamespaceState clusterState;
    @FXML
    private TextField clusterNameField;
    @FXML
    private TextField namespaceField;
    @FXML
    private DialogPane dialogPane;

    public CreateNamespaceController(ClusterService clusterService, ClusterNamespaceState clusterState) {
        this.clusterService = clusterService;
        this.clusterState = clusterState;
    }


    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            if (okButton != null) {
                okButton.disableProperty().bind(clusterNameField.textProperty().isEmpty());
                // okButton.setOnAction(e -> saveCluster());
            }
        });
    }


    public String getClusterName() {
        return clusterNameField.getText().trim();
    }

    public String getNamespace() {
        return namespaceField.getText().trim();
    }

    public void saveCluster() {
        String clusterName = clusterNameField.getText().trim();
        String namespace = namespaceField.getText().trim();

        if (!clusterName.isEmpty() && !namespace.isEmpty()) {
            clusterService.createNamespace(clusterName, namespace);
            clusterState.addNamespaceToCluster(clusterName, new Namespace(namespace));
            System.out.println("Namespace guardado en clusters.json");
        }
    }
}
