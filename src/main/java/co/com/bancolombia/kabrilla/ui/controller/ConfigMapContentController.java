package co.com.bancolombia.kabrilla.ui.controller;

import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ConfigMapContentController implements DeploymentTab {


    @FXML
    private FlowPane configMapContainer;

    public void loadConfigMaps(Map<String, Map<String, String>> configMaps) {
        // Limpia el contenedor
        configMapContainer.getChildren().clear();

        for (var entry : configMaps.entrySet()) {
            String name = entry.getKey();
            Map<String, String> data = entry.getValue();

            VBox card = new VBox(8);
            card.setStyle("-fx-background-color:#3C3F41; -fx-background-radius:10; -fx-padding:15;");

            Label title = new Label(name);
            title.setStyle("-fx-text-fill:white; -fx-font-size:14; -fx-font-weight:bold;");

            VBox dataBox = new VBox(4);
            dataBox.setStyle("-fx-background-color:#4E5254; -fx-background-radius:6; -fx-padding:8;");

            data.forEach((k, v) -> {
                HBox row = new HBox(8);
                Label key = new Label(k + ":");
                key.setStyle("-fx-text-fill:#1EBFF2; -fx-font-family:Consolas;");
                Label value = new Label(v);
                value.setStyle("-fx-text-fill:white; -fx-font-family:Consolas;");
                row.getChildren().addAll(key, value);
                dataBox.getChildren().add(row);
            });

            card.getChildren().addAll(title, dataBox);
            configMapContainer.getChildren().add(card);
        }
    }

    @Override
    public void setDeploymentName(String name) {
        System.out.println("NAME: " + name);
    }
}
