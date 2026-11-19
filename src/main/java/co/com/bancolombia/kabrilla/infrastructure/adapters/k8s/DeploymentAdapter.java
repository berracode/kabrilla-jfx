package co.com.bancolombia.kabrilla.infrastructure.adapters.k8s;

import java.util.ArrayList;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.DeploymentRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeClientFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentAdapter implements DeploymentRepository {


    private final KubeClientFactory kubeClientFactory;

    public DeploymentAdapter(KubeClientFactory kubeClientFactory) {
        this.kubeClientFactory = kubeClientFactory;
    }


    @Override
    public List<Deployment> findAll(KubeConfig kubeConfig, AwsCredentials credentials, String namespace) {
        try {
            ApiClient client = kubeClientFactory.forCluster(kubeConfig, credentials);
            AppsV1Api appsV1Api = new AppsV1Api(client);
            V1DeploymentList deploymentList = appsV1Api.listNamespacedDeployment(namespace).execute();
            List<Deployment> deployments = new ArrayList<>();

            for (V1Deployment d : deploymentList.getItems()) {
                var item = Deployment.builder()
                        .clusterName(kubeConfig.getClusterName())
                        .namespace(namespace)
                        .name(d.getMetadata().getName())
                        .build();
                deployments.add(item);
                log.info("Deployment: {}", item.getName());
            }
            return deployments;

        } catch (io.kubernetes.client.openapi.ApiException apiEx) {
            throw K8sErrors.from(apiEx, "listar los deployments de " + namespace);
        }
    }
}
