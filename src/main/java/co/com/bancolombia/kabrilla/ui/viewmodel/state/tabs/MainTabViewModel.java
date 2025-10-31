package co.com.bancolombia.kabrilla.ui.viewmodel.state.tabs;

import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentStateViewModel;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MainTabViewModel {
    private final DeploymentStateViewModel deploymentStateViewModel;

    public DeploymentStateViewModel getDeploymentStateViewModel() {
        return deploymentStateViewModel;
    }
}
