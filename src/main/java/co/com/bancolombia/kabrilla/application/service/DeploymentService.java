package co.com.bancolombia.kabrilla.application.service;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.DeploymentRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.Deployment;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;

public class DeploymentService {
    private final DeploymentRepository deploymentRepository;

    public DeploymentService(DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    public List<Deployment> getDeployments(KubeConfig kubeConfig, AwsCredentials credentials, String namespace) {
        return deploymentRepository.findAll(kubeConfig, credentials, namespace);
    }
}
