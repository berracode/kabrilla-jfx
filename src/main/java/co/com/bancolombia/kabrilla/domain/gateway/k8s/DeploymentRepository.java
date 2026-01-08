package co.com.bancolombia.kabrilla.domain.gateway.k8s;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;

public interface DeploymentRepository {
    List<Deployment> findAll(KubeConfig kubeConfig, AwsCredentials credentials, String namespace);
}
