package co.com.bancolombia.kabrilla.domain.gateway;

import co.com.bancolombia.kabrilla.domain.model.KubeConfigModel;

public interface KubeconfigRepository {

    KubeConfigModel findAllKubeconfig();
}
