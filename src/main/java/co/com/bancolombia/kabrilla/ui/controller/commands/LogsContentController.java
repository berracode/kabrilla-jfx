package co.com.bancolombia.kabrilla.ui.controller.commands;

import co.com.bancolombia.kabrilla.ui.controller.tabs.DeploymentTab;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

public class LogsContentController implements DeploymentTab {
    @FXML
    private ComboBox<String> comboBox;
    @FXML
    private TextArea logsArea;

    @FXML
    public void initialize() {
        comboBox.getItems().addAll("Combined logs", "pod-1", "pod-2", "pod-3", "pod-4");
        comboBox.getSelectionModel().selectFirst();
    }

    @Override
    public void setDeploymentName(String name) {
        logsArea.setText("Logs para " + name + "=# _ 2W".repeat(900));
    }
}