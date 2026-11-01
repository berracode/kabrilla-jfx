package co.com.bancolombia.kabrilla.infrastructure.adapters.mock;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;
import co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.ManifestResourceRepository;
import co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

/**
 * Lee manifiestos YAML (como los de `kubectl get -o yaml`) de {root}/{cluster}/{namespace}/*.yaml.
 * Un archivo puede tener varios documentos separados por "---".
 */
@Slf4j
public class MockKubeResourceAdapter extends ManifestResourceRepository {

    private final Path resourcesRoot;

    public MockKubeResourceAdapter(Path resourcesRoot) {
        this.resourcesRoot = resourcesRoot;
    }

    @Override
    protected Map<String, Object> findDeployment(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                                 String deploymentName) throws Exception {
        return loadNamespace(kubeConfig, namespace).stream()
                .filter(m -> "Deployment".equals(Manifests.str(m, "kind")))
                .filter(m -> deploymentName.equals(Manifests.name(m)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "[MOCK] No hay manifiesto Deployment '" + deploymentName + "' en "
                                + namespaceDir(kubeConfig, namespace)));
    }

    @Override
    protected List<Map<String, Object>> listManifests(KubeConfig kubeConfig, AwsCredentials credentials,
                                                      String namespace, ResourceKind kind) throws Exception {
        return loadNamespace(kubeConfig, namespace).stream()
                .filter(m -> kind.k8sKind().equals(Manifests.str(m, "kind")))
                .toList();
    }

    private List<Map<String, Object>> loadNamespace(KubeConfig kubeConfig, String namespace) throws Exception {
        Path dir = namespaceDir(kubeConfig, namespace);
        log.info("[MOCK] Leyendo manifiestos de {}", dir.toAbsolutePath());
        List<Map<String, Object>> manifests = new ArrayList<>();
        for (Path file : MockFiles.list(dir)) {
            String fileName = file.getFileName().toString();
            if (!fileName.endsWith(".yaml") && !fileName.endsWith(".yml")) {
                continue;
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                for (Object document : new Yaml().loadAll(reader)) {
                    if (document instanceof Map<?, ?>) {
                        manifests.add(Manifests.map(document));
                    }
                }
            }
        }
        return manifests;
    }

    private Path namespaceDir(KubeConfig kubeConfig, String namespace) {
        return resourcesRoot.resolve(MockFiles.shortClusterName(kubeConfig)).resolve(namespace);
    }
}
