package co.com.bancolombia.kabrilla.ui.controller.commands;

import co.com.bancolombia.kabrilla.ui.controller.tabs.DeploymentTab;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class RequestPolicyContentController implements DeploymentTab {

    @FXML
    private TextArea yamlArea;

    @Override
    public void setDeploymentName(String deploymentName) {
        String yaml = """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata:
                  name: restrict-external-%s
                spec:
                  podSelector:
                    matchLabels:
                      app: %s
                  policyTypes:
                    - Ingress
                  ingress:
                    - from:
                        - podSelector:
                            matchLabels:
                              app: backend
                        - namespaceSelector:
                            matchLabels:
                              name: xyz-1-dev
                """.formatted(deploymentName, deploymentName);

        yamlArea.setText(yaml);
    }
}
