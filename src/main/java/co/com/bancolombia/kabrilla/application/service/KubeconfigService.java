package co.com.bancolombia.kabrilla.application.service;

import co.com.bancolombia.kabrilla.domain.gateway.KubeconfigRepository;
import co.com.bancolombia.kabrilla.domain.model.KubeConfigModel;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class KubeconfigService {

    private final KubeconfigRepository kubeconfigRepository;

    public KubeConfigModel getKubeConfig() {
        return kubeconfigRepository.findAllKubeconfig();
    }


}
