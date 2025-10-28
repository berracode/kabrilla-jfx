package co.com.bancolombia.kabrilla.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class AuthorizationPolicyContentController implements DeploymentTab {

    @FXML
    private Label nameLabel;
    @FXML
    private Label namespaceLabel;
    @FXML
    private Label actionLabel;
    @FXML
    private TextArea rulesArea;

    @Override
    public void setDeploymentName(String deploymentName) {
        nameLabel.setText("auth-policy-" + deploymentName);
        namespaceLabel.setText("xyz-1-dev");
        actionLabel.setText("ALLOW");

        String rules = """
                - from:
                    - source:
                        principals: ["cluster.local/ns/xyz-1-dev/sa/frontend"]
                - to:
                    - operation:
                        methods: ["GET", "POST"]
                        paths: ["/api/*"]
                """;
        rulesArea.setText(rules);
    }
}
