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
import co.com.bancolombia.kabrilla.infrastructure.adapters.fs.FileClusterRepository;
import co.com.bancolombia.kabrilla.infrastructure.adapters.fs.JsonCredentialRepository;
import co.com.bancolombia.kabrilla.infrastructure.adapters.mock.MockDeploymentAdapter;
import co.com.bancolombia.kabrilla.infrastructure.adapters.mock.MockKubeResourceAdapter;
import co.com.bancolombia.kabrilla.infrastructure.adapters.mock.MockLogsAdapter;
import co.com.bancolombia.kabrilla.infrastructure.adapters.mock.MockPodAdapter;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeconfigAdapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Perfil "mock": todo sale de la carpeta mock-data/ (ver mock-data/README.md). No requiere AWS ni cluster.
 * Los adaptadores de kubeconfig, clusters y credenciales son los reales apuntando a archivos de mock-data,
 * así también se ejercita su código de parsing.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@Profile("mock")
public class MockAdaptersConfiguration {

    private final Path mockDir;

    public MockAdaptersConfiguration(@Value("${kabrilla.mock.dir}") String mockDir) {
        this.mockDir = Path.of(mockDir).toAbsolutePath();
        log.warn("[MOCK] Perfil mock activo. Datos desde: {}", this.mockDir);
    }

    @Bean
    public KubeconfigRepository kubeconfigRepository() {
        return new KubeconfigAdapter(mockDir.resolve("kubeconfig.yaml").toString());
    }

    @Bean
    public ClusterRepository clusterRepository() {
        return new FileClusterRepository(new File(mockDir.toFile(), "clusters.json"));
    }

    @Bean
    public CredentialRepository credentialRepository() {
        return new JsonCredentialRepository(mockDir.resolve("credentials.json").toString());
    }

    @Bean
    public DeploymentRepository deploymentRepository() {
        return new MockDeploymentAdapter(mockDir.resolve("logs"));
    }

    @Bean
    public PodsRepository podsRepository() {
        return new MockPodAdapter(mockDir.resolve("logs"));
    }

    @Bean
    public KubeResourceRepository kubeResourceRepository() {
        return new MockKubeResourceAdapter(mockDir.resolve("resources"));
    }

    @Bean
    public LogsRepository logsRepository(@Value("${kabrilla.mock.line-delay-ms}") long lineDelayMs) {
        return new MockLogsAdapter(mockDir.resolve("logs"), lineDelayMs);
    }
}
