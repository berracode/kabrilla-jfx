package co.com.bancolombia.kabrilla.ui.context;


import java.util.function.Supplier;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.ui.viewmodel.credentials.CredentialsItemViewModel;

/**
 * Datos para consultar un deployment desde su pestaña.
 *
 * @param credentialsSource credenciales vigentes del cluster. Se consultan en cada uso (no se copian al abrir la
 *                          pestaña) para que, al renovarlas, una pestaña ya abierta use las nuevas con "Refrescar".
 *                          Llamar desde el hilo de JavaFX: lee el estado de la UI.
 */
public record DeploymentContext(
        String clusterName,
        String namespace,
        String deploymentName,
        Supplier<CredentialsItemViewModel> credentialsSource,
        KubeConfig kubeConfig
) {

    /**
     * Credenciales vigentes en formato de dominio; {@code null} si el cluster no tiene credenciales registradas.
     */
    public AwsCredentials awsCredentials() {
        CredentialsItemViewModel credentials = credentialsSource.get();
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

    public DeploymentContext withDeploymentName(String name) {
        return new DeploymentContext(clusterName, namespace, name, credentialsSource, kubeConfig);
    }
}
