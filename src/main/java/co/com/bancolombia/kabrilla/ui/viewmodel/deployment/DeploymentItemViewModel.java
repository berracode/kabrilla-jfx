package co.com.bancolombia.kabrilla.ui.viewmodel.deployment;

import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;

public record DeploymentItemViewModel(Deployment domainDeployment) {

    public String getName() {
        return domainDeployment.getName();
    }
}

