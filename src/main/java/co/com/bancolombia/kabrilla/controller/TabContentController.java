package co.com.bancolombia.kabrilla.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

public class TabContentController {

    @FXML
    private TabPane commandTabPane;

    @FXML
    private AnchorPane commandContent;

    private String deploymentName;

    public void setDeploymentName(String name) {
        this.deploymentName = name;
        loadCommandView("logs-content.fxml");

    }

    @FXML
    public void initialize() {
        commandTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                switch (newTab.getText()) {
                    case "Logs" -> loadCommandView("logs-content.fxml");
                    case "Pods" -> loadCommandView("pods-content.fxml");
                    case "ConfigMap" -> loadCommandView("configmap-content.fxml");
                    case "RequestPolicy" -> loadCommandView("requestpolicy-content.fxml");
                    case "Service" -> loadCommandView("service-content.fxml");
                    case "AuthorizationPolicy" -> loadCommandView("authorizationpolicy-content.fxml");
                    case "HPA" -> loadCommandView("hpa-content.fxml");
                }
            }
        });

    }

    private void loadCommandView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlPath));
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DeploymentTab) {
                ((DeploymentTab) controller).setDeploymentName(deploymentName);
            }

            commandContent.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
