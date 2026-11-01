package co.com.bancolombia.kabrilla.application.service;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.KubeResourceRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.KubeResource;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;

public class KubeResourceService {

    private final KubeResourceRepository repository;

    public KubeResourceService(KubeResourceRepository repository) {
        this.repository = repository;
    }

    public List<KubeResource> findByDeployment(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                               String deploymentName, ResourceKind kind) throws Exception {
        return repository.findByDeployment(kubeConfig, credentials, namespace, deploymentName, kind);
    }
}
