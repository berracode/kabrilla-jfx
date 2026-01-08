package co.com.bancolombia.kabrilla.ui.context;


import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.ui.viewmodel.credentials.CredentialsItemViewModel;

public record DeploymentContext(
        String clusterName,
        String namespace,
        String deploymentName,
        CredentialsItemViewModel credentials,
        KubeConfig kubeConfig
) {
}

