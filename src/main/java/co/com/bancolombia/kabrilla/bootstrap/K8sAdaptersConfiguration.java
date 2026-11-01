package co.com.bancolombia.kabrilla.bootstrap;

import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.gateway.CredentialRepository;
import co.com.bancolombia.kabrilla.domain.gateway.KubeconfigRepository;
import co.com.bancolombia.kabrilla.domain.gateway.k8s.DeploymentRepository;
import co.com.bancolombia.kabrilla.domain.gateway.k8s.KubeResourceRepository;
import co.com.bancolombia.kabrilla.domain.gateway.k8s.LogsRepository;
import co.com.bancolombia.kabrilla.domain.gateway.k8s.PodsRepository;
import co.com.bancolombia.kabrilla.infrastructure.adapters.EksTokenProvider;
import co.com.bancolombia.kabrilla.infrastructure.adapters.fs.FileClusterRepository;
import co.com.bancolombia.kabrilla.infrastructure.adapters.fs.JsonCredentialRepository;
import co.com.bancolombia.kabrilla.infrastructure.adapters.k8s.DeploymentAdapter;
import co.com.bancolombia.kabrilla.infrastructure.adapters.k8s.KubeResourceAdapter;
import co.com.bancolombia.kabrilla.infrastructure.adapters.k8s.LogsAdapter;
import co.com.bancolombia.kabrilla.infrastructure.adapters.k8s.PodAdapter;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeClientFactory;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeconfigAdapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Adaptadores reales: ~/.kube/config, archivos locales y API de Kubernetes (EKS).
 */
@Configuration(proxyBeanMethods = false)
@Profile("!mock")
public class K8sAdaptersConfiguration {

    @Bean
    public KubeconfigRepository kubeconfigRepository() {
        return new KubeconfigAdapter();
    }

    @Bean
    public ClusterRepository clusterRepository() {
        return new FileClusterRepository();
    }

    @Bean
    public CredentialRepository credentialRepository() {
        return new JsonCredentialRepository();
    }

    @Bean(destroyMethod = "shutdown")
    public KubeClientFactory kubeClientFactory() {
        return new KubeClientFactory(EksTokenProvider::getEksToken);
    }

    @Bean
    public DeploymentRepository deploymentRepository(KubeClientFactory factory) {
        return new DeploymentAdapter(factory);
    }

    @Bean
    public PodsRepository podsRepository(KubeClientFactory factory) {
        return new PodAdapter(factory);
    }

    @Bean
    public LogsRepository logsRepository(KubeClientFactory factory) {
        return new LogsAdapter(factory);
    }

    @Bean
    public KubeResourceRepository kubeResourceRepository(KubeClientFactory factory) {
        return new KubeResourceAdapter(factory);
    }
}
