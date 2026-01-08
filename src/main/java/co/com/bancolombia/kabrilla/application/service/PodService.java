package co.com.bancolombia.kabrilla.application.service;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.PodsRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;

public class PodService {

    private final PodsRepository podsRepository;

    public PodService(PodsRepository podsRepository) {
        this.podsRepository = podsRepository;
    }

    public List<Pod> getPods(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                             String deploymentName) throws Exception {
        return podsRepository.listPods(kubeConfig, credentials, namespace, deploymentName);
    }
}
