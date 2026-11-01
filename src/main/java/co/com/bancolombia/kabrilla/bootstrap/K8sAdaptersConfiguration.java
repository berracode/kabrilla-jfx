package co.com.bancolombia.kabrilla.bootstrap;

import java.io.File;
import java.nio.file.Path;

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
import co.com.bancolombia.kabrilla.infrastructure.adapters.fs.LocalDataDirectory;
import co.com.bancolombia.kabrilla.infrastructure.adapters.k8s.DeploymentAdapter;
import co.com.bancolombia.kabrilla.infrastructure.adapters.k8s.KubeResourceAdapter;
import co.com.bancolombia.kabrilla.infrastructure.adapters.k8s.LogsAdapter;
import co.com.bancolombia.kabrilla.infrastructure.adapters.k8s.PodAdapter;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeClientFactory;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeconfigAdapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Adaptadores reales: ~/.kube/config, carpeta de datos del usuario (~/.kabrilla) y API de Kubernetes (EKS).
 */
@Configuration(proxyBeanMethods = false)
@Profile("!mock")
public class K8sAdaptersConfiguration {

    @Bean
    public LocalDataDirectory localDataDirectory(@Value("${kabrilla.data-dir}") String dataDir) {
        // Las versiones anteriores guardaban los archivos en el directorio de trabajo: se migran desde ahí
        return new LocalDataDirectory(Path.of(dataDir), Path.of(System.getProperty("user.dir")));
    }

    /**
     * Igual que kubectl: kabrilla.kubeconfig, si no la variable KUBECONFIG (primer archivo), si no ~/.kube/config.
     */
    @Bean
    public KubeconfigRepository kubeconfigRepository(@Value("${kabrilla.kubeconfig:}") String kubeconfig) {
        if (!kubeconfig.isBlank()) {
            return new KubeconfigAdapter(kubeconfig);
        }
        String env = System.getenv("KUBECONFIG");
        if (env != null && !env.isBlank()) {
            return new KubeconfigAdapter(env.split(File.pathSeparator)[0]);
        }
        return new KubeconfigAdapter();
    }

    @Bean
    public ClusterRepository clusterRepository(LocalDataDirectory dataDirectory) {
        return new FileClusterRepository(dataDirectory.resolve(LocalDataDirectory.CLUSTERS_FILE).toFile());
    }

    @Bean
    public CredentialRepository credentialRepository(LocalDataDirectory dataDirectory) {
        return new JsonCredentialRepository(dataDirectory.resolve(LocalDataDirectory.CREDENTIALS_FILE).toString());
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
