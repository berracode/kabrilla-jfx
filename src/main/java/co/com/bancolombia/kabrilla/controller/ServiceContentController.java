package co.com.bancolombia.kabrilla.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ServiceContentController implements DeploymentTab {

    @FXML
    private Label nameLabel;
    @FXML
    private Label namespaceLabel;
    @FXML
    private Label typeLabel;
    @FXML
    private Label clusterIpLabel;
    @FXML
    private Label portsLabel;
    @FXML
    private Label selectorLabel;

    @Override
    public void setDeploymentName(String deploymentName) {
        nameLabel.setText(deploymentName + "-service");
        namespaceLabel.setText("xyz-1-dev");
        typeLabel.setText("ClusterIP");
        clusterIpLabel.setText("10.43.112.54");
        portsLabel.setText("80/TCP → 8080");
        selectorLabel.setText("app=" + deploymentName);
    }
}
