package co.com.bancolombia.kabrilla.ui.context;


import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.ui.viewmodel.credentials.CredentialsItemViewModel;

public record DeploymentContext(
        String clusterName,
        String namespace,
        String deploymentName,
        CredentialsItemViewModel credentials,
        KubeConfig kubeConfig
) {

    /**
     * Credenciales en formato de dominio; {@code null} si el cluster no tiene credenciales registradas.
     */
    public AwsCredentials awsCredentials() {
        if (credentials == null) {
            return null;
        }
        return AwsCredentials.builder()
                .clusterName(credentials.clusterName())
                .accessKey(credentials.accessKey())
                .secretKey(credentials.secretKey())
                .sessionToken(credentials.sessionToken())
                .build();
    }
}

