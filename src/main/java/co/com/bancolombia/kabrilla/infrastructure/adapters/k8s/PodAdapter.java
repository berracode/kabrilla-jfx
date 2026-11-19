package co.com.bancolombia.kabrilla.infrastructure.adapters.k8s;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.PodsRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.ContainerSelector;
import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeClientFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Container;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PodAdapter implements PodsRepository {
    private final KubeClientFactory kubeClientFactory;

    public PodAdapter(KubeClientFactory kubeClientFactory) {
        this.kubeClientFactory = kubeClientFactory;
    }

    @Override
    public List<Pod> listPods(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                              String deploymentName) throws Exception {
        ApiClient client = kubeClientFactory.forCluster(kubeConfig, credentials);
        var core = new io.kubernetes.client.openapi.apis.CoreV1Api(client);

        io.kubernetes.client.openapi.models.V1PodList podList;
        try {
            podList = core.listNamespacedPod(namespace).execute();
        } catch (io.kubernetes.client.openapi.ApiException e) {
            throw K8sErrors.from(e, "listar los pods de " + namespace);
        }

        return podList.getItems().stream().filter(p -> p.getMetadata().getName().startsWith(deploymentName))
                .map(p -> {
                    List<String> containers = p.getSpec() != null
                            ? p.getSpec().getContainers().stream().map(V1Container::getName).toList()
                            : List.of();
                    String annotatedDefault = p.getMetadata().getAnnotations() != null
                            ? p.getMetadata().getAnnotations().get(ContainerSelector.DEFAULT_CONTAINER_ANNOTATION)
                            : null;
                    return Pod.builder()
                            .name(p.getMetadata().getName())
                            .status(p.getStatus() != null ? p.getStatus().getPhase() : "Unknown")
                            .restarts(p.getStatus() != null && p.getStatus().getContainerStatuses() != null
                                              ? p.getStatus().getContainerStatuses().stream()
                                    .mapToInt(cs -> cs.getRestartCount()).sum()
                                              : 0)
                            .age(ageFrom(p.getMetadata().getCreationTimestamp()))
                            .node(p.getSpec() != null ? p.getSpec().getNodeName() : "N/A")
                            .containers(containers)
                            .defaultContainer(ContainerSelector.pickDefault(containers, annotatedDefault))
                            .build();
                }).toList();
    }

    private String ageFrom(java.time.OffsetDateTime creationTs) {
        java.time.Duration d = java.time.Duration.between(creationTs, java.time.OffsetDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h";
        }
        long days = hours / 24;
        return days + "d";
    }

}
