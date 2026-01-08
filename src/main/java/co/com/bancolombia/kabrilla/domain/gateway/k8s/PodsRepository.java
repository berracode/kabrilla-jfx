package co.com.bancolombia.kabrilla.domain.gateway.k8s;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;

public interface PodsRepository {

    List<Pod> listPods(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                       String deplyomentName) throws Exception;

}
