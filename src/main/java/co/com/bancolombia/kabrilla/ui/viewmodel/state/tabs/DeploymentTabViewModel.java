package co.com.bancolombia.kabrilla.ui.viewmodel.state.tabs;

import lombok.Getter;

@Getter
public class DeploymentTabViewModel {
    private final String clusterName;
    private final String namespace;
    private final String deploymentName;

    public DeploymentTabViewModel(String clusterName, String namespace, String deploymentName) {
        this.clusterName = clusterName;
        this.namespace = namespace;
        this.deploymentName = deploymentName;

    }

}
