package co.com.bancolombia.kabrilla.infrastructure.adapters.manifest;

import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.list;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.map;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.name;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.str;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.stringMap;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.KubeResourceRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.KubeResource;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;

/**
 * Lógica común (real y mock) para decidir qué recursos pertenecen a un deployment.
 * Las subclases solo saben obtener manifiestos; aquí se filtran y se convierten con {@link ManifestMapper}.
 */
public abstract class ManifestResourceRepository implements KubeResourceRepository {

    private final ManifestMapper mapper = new ManifestMapper();

    protected abstract Map<String, Object> findDeployment(KubeConfig kubeConfig, AwsCredentials credentials,
                                                          String namespace, String deploymentName)
            throws Exception;

    protected abstract List<Map<String, Object>> listManifests(KubeConfig kubeConfig, AwsCredentials credentials,
                                                               String namespace, ResourceKind kind)
            throws Exception;

    @Override
    public List<KubeResource> findByDeployment(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                               String deploymentName, ResourceKind kind) throws Exception {
        Predicate<Map<String, Object>> belongs;
        if (kind == ResourceKind.HPA) {
            belongs = hpa -> "Deployment".equals(str(hpa, "spec", "scaleTargetRef", "kind"))
                    && deploymentName.equals(str(hpa, "spec", "scaleTargetRef", "name"));
        } else {
            Map<String, Object> deployment = findDeployment(kubeConfig, credentials, namespace, deploymentName);
            belongs = belongsToDeployment(kind, deployment);
        }

        return listManifests(kubeConfig, credentials, namespace, kind).stream()
                .filter(belongs)
                .map(manifest -> mapper.toResource(kind, manifest))
                .sorted(Comparator.comparing(KubeResource::name))
                .toList();
    }

    static Predicate<Map<String, Object>> belongsToDeployment(ResourceKind kind, Map<String, Object> deployment) {
        Map<String, String> podLabels = stringMap(deployment, "spec", "template", "metadata", "labels");
        return switch (kind) {
            case SERVICE -> service -> selects(stringMap(service, "spec", "selector"), podLabels);
            case CONFIG_MAP -> {
                Set<String> referenced = referencedConfigMaps(deployment);
                yield configMap -> referenced.contains(name(configMap));
            }
            // Istio: sin selector la política aplica a todo el namespace
            case AUTHORIZATION_POLICY, REQUEST_AUTHENTICATION -> policy -> {
                Map<String, String> selector = stringMap(policy, "spec", "selector", "matchLabels");
                return selector.isEmpty() || selects(selector, podLabels);
            };
            case HPA -> throw new IllegalArgumentException("HPA se asocia por scaleTargetRef");
        };
    }

    private static boolean selects(Map<String, String> selector, Map<String, String> labels) {
        return !selector.isEmpty() && labels.entrySet().containsAll(selector.entrySet());
    }

    /**
     * ConfigMaps usados por el pod template: volúmenes (incluidos projected), envFrom y env.valueFrom.
     */
    static Set<String> referencedConfigMaps(Map<String, Object> deployment) {
        Map<String, Object> podSpec = map(deployment, "spec", "template", "spec");
        Set<String> names = new HashSet<>();
        for (Object volume : list(podSpec, "volumes")) {
            names.add(str(volume, "configMap", "name"));
            for (Object source : list(volume, "projected", "sources")) {
                names.add(str(source, "configMap", "name"));
            }
        }
        for (String containerList : List.of("containers", "initContainers")) {
            for (Object container : list(podSpec, containerList)) {
                for (Object envFrom : list(container, "envFrom")) {
                    names.add(str(envFrom, "configMapRef", "name"));
                }
                for (Object env : list(container, "env")) {
                    names.add(str(env, "valueFrom", "configMapKeyRef", "name"));
                }
            }
        }
        names.removeIf(Objects::isNull);
        return names;
    }
}
