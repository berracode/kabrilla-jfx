package co.com.bancolombia.kabrilla.infrastructure.adapters.mock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.DeploymentRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;

import lombok.extern.slf4j.Slf4j;

/**
 * Cada subcarpeta de {root}/{cluster}/{namespace}/ es un deployment.
 */
@Slf4j
public class MockDeploymentAdapter implements DeploymentRepository {

    private final Path logsRoot;

    public MockDeploymentAdapter(Path logsRoot) {
        this.logsRoot = logsRoot;
    }

    @Override
    public List<Deployment> findAll(KubeConfig kubeConfig, AwsCredentials credentials, String namespace) {
        Path namespaceDir = logsRoot.resolve(MockFiles.shortClusterName(kubeConfig)).resolve(namespace);
        log.info("[MOCK] Leyendo deployments de {}", namespaceDir.toAbsolutePath());
        try {
            return MockFiles.list(namespaceDir).stream()
                    .filter(Files::isDirectory)
                    .map(dir -> Deployment.builder()
                            .clusterName(kubeConfig.getClusterName())
                            .namespace(namespace)
                            .name(dir.getFileName().toString())
                            .build())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Error leyendo deployments mock en " + namespaceDir, e);
        }
    }
}
