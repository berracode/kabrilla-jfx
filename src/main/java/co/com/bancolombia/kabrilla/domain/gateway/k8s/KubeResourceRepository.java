package co.com.bancolombia.kabrilla.domain.gateway.k8s;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.KubeResource;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;

public interface KubeResourceRepository {

    /**
     * Recursos del tipo indicado que aplican al deployment (por selector de labels, scaleTargetRef o referencias
     * desde el pod template, según el tipo).
     */
    List<KubeResource> findByDeployment(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                        String deploymentName, ResourceKind kind) throws Exception;
}
