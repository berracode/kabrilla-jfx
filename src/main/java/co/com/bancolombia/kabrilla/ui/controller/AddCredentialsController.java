package co.com.bancolombia.kabrilla.ui.controller;

import co.com.bancolombia.kabrilla.application.service.CredentialService;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceState;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

public class AddCredentialsController {

    private final ClusterNamespaceState clusterState;
    private final CredentialService credentialService;
    @FXML
    private ComboBox<String> clusterComboBox;
    @FXML
    private TextArea credentialsArea;
    @FXML
    private Button saveButton;

    public AddCredentialsController(ClusterNamespaceState clusterState, CredentialService credentialService) {
        this.clusterState = clusterState;
        this.credentialService = credentialService;
    }

    @FXML
    public void initialize() {
        clusterState.getObservableClusters().forEach(vm ->
                                                             clusterComboBox.getItems().add(vm.getName())
        );
    }

    @FXML
    public void handleSave() {
        String cluster = clusterComboBox.getValue();
        String credentials = credentialsArea.getText().trim();

        if (cluster == null || cluster.isBlank() || credentials.isBlank()) {
            showAlert("Debes seleccionar un cluster y pegar las credenciales.");
            return;
        }

        credentialService.saveCredentials(cluster, credentials);
        showAlert("Credenciales guardadas correctamente para el cluster: " + cluster, Alert.AlertType.INFORMATION);
    }

    private void showAlert(String message) {
        showAlert(message, Alert.AlertType.WARNING);
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(message);

        var alertPane = alert.getDialogPane();
        alertPane.getStylesheets().add(getClass().getResource("/css/demo.css").toExternalForm());
        alertPane.setStyle("-fx-background-color: #2B2B2B;");
        alertPane.lookup(".content.label").setStyle("-fx-text-fill: white;");

        alert.showAndWait();
    }
}

