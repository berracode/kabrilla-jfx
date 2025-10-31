package co.com.bancolombia.kabrilla.bootstrap;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.application.service.CredentialService;
import co.com.bancolombia.kabrilla.application.service.KubeconfigService;
import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.gateway.KubeconfigRepository;
import co.com.bancolombia.kabrilla.infrastructure.adapters.fs.FileClusterRepository;
import co.com.bancolombia.kabrilla.infrastructure.adapters.fs.JsonCredentialRepository;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeconfigAdapter;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceStateViewModel;
import lombok.Getter;

@Getter
public class AppBootstrap {

    private static AppBootstrap instance;

    private final ClusterService clusterService;
    private final ClusterNamespaceStateViewModel clusterNamespaceStateViewModel;
    private final CredentialService credentialService;

    private AppBootstrap() {
        // Inicialización de dependencias
        KubeconfigRepository kubeconfigRepo = new KubeconfigAdapter();
        KubeconfigService kubeconfigService = new KubeconfigService(kubeconfigRepo);
        ClusterRepository clusterRepo = new FileClusterRepository();

        this.clusterService = new ClusterService(kubeconfigService, clusterRepo);
        this.clusterNamespaceStateViewModel = new ClusterNamespaceStateViewModel();

        this.credentialService = new CredentialService(new JsonCredentialRepository());
    }

    public static AppBootstrap getInstance() {
        if (instance == null) {
            instance = new AppBootstrap();
        }
        return instance;
    }

}
