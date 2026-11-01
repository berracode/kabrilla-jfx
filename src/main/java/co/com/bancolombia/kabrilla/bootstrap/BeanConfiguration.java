package co.com.bancolombia.kabrilla.bootstrap;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.application.service.CredentialService;
import co.com.bancolombia.kabrilla.application.service.DeploymentService;
import co.com.bancolombia.kabrilla.application.service.KubeconfigService;
import co.com.bancolombia.kabrilla.application.service.LogsService;
import co.com.bancolombia.kabrilla.application.service.PodService;
import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.gateway.CredentialRepository;
import co.com.bancolombia.kabrilla.domain.gateway.KubeconfigRepository;
import co.com.bancolombia.kabrilla.domain.gateway.k8s.DeploymentRepository;
import co.com.bancolombia.kabrilla.domain.gateway.k8s.LogsRepository;
import co.com.bancolombia.kabrilla.domain.gateway.k8s.PodsRepository;
import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.AppContext;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.AppState;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.tabs.MainTabViewModel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de dependencias común. Domain y application no conocen Spring: sus clases se registran aquí.
 * Los adaptadores (gateways) se definen por perfil en {@link K8sAdaptersConfiguration} y
 * {@link MockAdaptersConfiguration}.
 */
@Configuration(proxyBeanMethods = false)
public class BeanConfiguration {

    // --- Servicios (application) ---

    @Bean
    public KubeconfigService kubeconfigService(KubeconfigRepository repository) {
        return new KubeconfigService(repository);
    }

    @Bean
    public ClusterService clusterService(KubeconfigService kubeconfigService, ClusterRepository repository) {
        return new ClusterService(kubeconfigService, repository);
    }

    @Bean
    public CredentialService credentialService(CredentialRepository repository) {
        return new CredentialService(repository);
    }

    @Bean
    public DeploymentService deploymentService(DeploymentRepository repository) {
        return new DeploymentService(repository);
    }

    @Bean
    public PodService podService(PodsRepository repository) {
        return new PodService(repository);
    }

    @Bean
    public LogsService logsService(LogsRepository repository) {
        return new LogsService(repository);
    }

    // --- Estado compartido de UI ---

    @Bean
    public AppContext appContext(CredentialService credentialService) {
        AppContext context = new AppContext();
        context.getAppState().loadCredentials(credentialService.findAll());
        return context;
    }

    @Bean
    public AppState appState(AppContext appContext) {
        // Misma instancia que AppContext#getAppState para que todos vean el mismo estado
        return appContext.getAppState();
    }

    @Bean
    public ClusterNamespaceStateViewModel clusterNamespaceStateViewModel() {
        return new ClusterNamespaceStateViewModel();
    }

    @Bean
    public MainTabViewModel mainTabViewModel(DeploymentStateViewModel deploymentStateViewModel) {
        return new MainTabViewModel(deploymentStateViewModel);
    }
}
