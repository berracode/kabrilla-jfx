package co.com.bancolombia.kabrilla.infrastructure.adapters.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.PodsRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.ContainerSelector;
import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;

import lombok.extern.slf4j.Slf4j;

/**
 * Cada archivo .log/.txt dentro de {root}/{cluster}/{namespace}/{deployment}/ es un pod.
 */
@Slf4j
public class MockPodAdapter implements PodsRepository {

    private final Path logsRoot;

    public MockPodAdapter(Path logsRoot) {
        this.logsRoot = logsRoot;
    }

    @Override
    public List<Pod> listPods(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                              String deploymentName) throws Exception {
        Path deploymentDir = logsRoot.resolve(MockFiles.shortClusterName(kubeConfig))
                .resolve(namespace)
                .resolve(deploymentName);
        log.info("[MOCK] Leyendo pods de {}", deploymentDir.toAbsolutePath());

        // Como en un cluster con Istio: container de la app + sidecar istio-proxy
        List<String> containers = List.of(deploymentName, "istio-proxy");
        // <pod>.previous.log es el container anterior de <pod>, no un pod aparte
        List<Path> files = MockFiles.list(deploymentDir).stream()
                .filter(MockFiles::isLogFile)
                .filter(file -> !MockFiles.podName(file).endsWith(".previous"))
                .toList();
        return files.stream()
                .map(file -> Pod.builder()
                        .name(MockFiles.podName(file))
                        .status("Running")
                        .age(ageOf(file))
                        .node("mock-node-1")
                        .containers(containers)
                        .defaultContainer(ContainerSelector.pickDefault(containers, null))
                        .build())
                .toList();
    }

    private String ageOf(Path file) {
        try {
            Duration d = Duration.between(Files.getLastModifiedTime(file).toInstant(), Instant.now());
            if (d.toMinutes() < 60) {
                return d.toMinutes() + "m";
            }
            if (d.toHours() < 24) {
                return d.toHours() + "h";
            }
            return d.toDays() + "d";
        } catch (Exception e) {
            return "N/A";
        }
    }
}
