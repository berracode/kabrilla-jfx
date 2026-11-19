package co.com.bancolombia.kabrilla.ui.controller.usecases;

import co.com.bancolombia.kabrilla.ui.services.ErrorMessages;
import co.com.bancolombia.kabrilla.ui.viewmodel.credentials.CredentialsStateViewModel;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddCredentialsController {

    private final CredentialsStateViewModel credentialsStateViewModel;
    @FXML
    private ComboBox<String> clusterComboBox;
    @FXML
    private TextArea credentialsArea;
    @FXML
    private Button saveButton;

    public AddCredentialsController(CredentialsStateViewModel credentialsStateViewModel) {
        this.credentialsStateViewModel = credentialsStateViewModel;
    }

    @FXML
    public void initialize() {
        credentialsStateViewModel.getAvailableClusters().forEach(vm ->
                                                                         clusterComboBox.getItems().add(vm.getName())
        );
        clusterComboBox.valueProperty().bindBidirectional(credentialsStateViewModel.selectedClusterProperty());
        credentialsArea.textProperty().bindBidirectional(credentialsStateViewModel.rawCredentialsProperty());

    }

    @FXML
    public void handleSave() {
        boolean success;
        try {
            success = credentialsStateViewModel.saveCredentials();
        } catch (RuntimeException e) {
            log.error("No se pudieron guardar las credenciales del cluster {}",
                      credentialsStateViewModel.selectedClusterProperty().get(), e);
            showAlert("No se pudieron guardar las credenciales: " + ErrorMessages.describe(e),
                      Alert.AlertType.ERROR);
            return;
        }

        if (!success) {
            showAlert("Debes seleccionar un cluster y pegar las credenciales.");
        } else {
            showAlert(
                    "Credenciales guardadas correctamente para el cluster: " + credentialsStateViewModel.selectedClusterProperty()
                            .get(), Alert.AlertType.INFORMATION);
            credentialsStateViewModel.clear();
        }
    }


    private void showAlert(String message) {
        showAlert(message, Alert.AlertType.WARNING);
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(message);


        alert.showAndWait();
    }

}

