package co.com.bancolombia.kabrilla.bootstrap;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.application.service.KubeconfigService;
import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.gateway.KubeconfigRepository;
import co.com.bancolombia.kabrilla.infrastructure.kubernetes.config.KubeconfigAdapter;
import co.com.bancolombia.kabrilla.infrastructure.persistence.FileClusterRepository;
import co.com.bancolombia.kabrilla.state.namespace.ClusterNamespaceState;

public class AppBootstrap {

    private static AppBootstrap instance;

    private final ClusterService clusterService;
    private final ClusterNamespaceState clusterNamespaceState;

    private AppBootstrap() {
        // Inicialización de dependencias
        KubeconfigRepository kubeconfigRepo = new KubeconfigAdapter();
        KubeconfigService kubeconfigService = new KubeconfigService(kubeconfigRepo);
        ClusterRepository clusterRepo = new FileClusterRepository();

        this.clusterService = new ClusterService(kubeconfigService, clusterRepo);
        this.clusterNamespaceState = new ClusterNamespaceState();
    }

    public static AppBootstrap getInstance() {
        if (instance == null) {
            instance = new AppBootstrap();
        }
        return instance;
    }

    public ClusterService getClusterService() {
        return clusterService;
    }

    public ClusterNamespaceState getClusterNamespaceState() {
        return clusterNamespaceState;
    }
}
