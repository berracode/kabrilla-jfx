package co.com.bancolombia.kabrilla.infrastructure.adapters.manifest;

import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.list;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.map;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.name;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.str;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.stringMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.KubeResourceRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.KubeResource;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;

import lombok.extern.slf4j.Slf4j;

/**
 * Lógica común (real y mock) para decidir qué recursos pertenecen a un deployment.
 * Las subclases solo saben obtener manifiestos; aquí se filtran y se convierten con {@link ManifestMapper}.
 */
@Slf4j
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
        if (kind == ResourceKind.VIRTUAL_SERVICE || kind == ResourceKind.GATEWAY) {
            Routing routing = routingOf(kubeConfig, credentials, namespace, deploymentName);
            List<KubeResource> resources = kind == ResourceKind.VIRTUAL_SERVICE
                    ? routing.virtualServices().stream()
                    .map(vs -> mapper.toVirtualService(vs, routing.services(), namespace))
                    .toList()
                    : gatewaysOf(kubeConfig, credentials, namespace, routing);
            return resources.stream().sorted(Comparator.comparing(KubeResource::name)).toList();
        }

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

    /**
     * Services del deployment y VirtualServices de su namespace que enrutan hacia alguno de ellos.
     */
    private record Routing(Set<String> services, List<Map<String, Object>> virtualServices) {
    }

    private Routing routingOf(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                              String deploymentName) throws Exception {
        Map<String, Object> deployment = findDeployment(kubeConfig, credentials, namespace, deploymentName);
        Predicate<Map<String, Object>> isOwnService = belongsToDeployment(ResourceKind.SERVICE, deployment);
        Set<String> services = new TreeSet<>();
        listManifests(kubeConfig, credentials, namespace, ResourceKind.SERVICE).stream()
                .filter(isOwnService)
                .forEach(service -> services.add(name(service)));
        if (services.isEmpty()) {
            return new Routing(services, List.of()); // sin Service no hay ruta de Istio hacia el deployment
        }
        List<Map<String, Object>> virtualServices =
                listManifests(kubeConfig, credentials, namespace, ResourceKind.VIRTUAL_SERVICE).stream()
                        .filter(vs -> !IstioRoutes.routedServices(vs, services, namespace).isEmpty())
                        .toList();
        return new Routing(services, virtualServices);
    }

    /**
     * Gateways referenciados por los VirtualServices del deployment. Suelen estar en otro namespace (p. ej.
     * istio-ingress): si no se pueden leer (RBAC) o no existen, se muestran igual con el motivo.
     */
    private List<KubeResource> gatewaysOf(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                          Routing routing) {
        Map<IstioRoutes.GatewayRef, List<String>> usedBy = new LinkedHashMap<>();
        for (Map<String, Object> vs : routing.virtualServices()) {
            for (IstioRoutes.GatewayRef ref : IstioRoutes.gatewayRefs(vs, namespace)) {
                usedBy.computeIfAbsent(ref, r -> new ArrayList<>()).add(name(vs));
            }
        }

        List<KubeResource> result = new ArrayList<>();
        Map<String, List<IstioRoutes.GatewayRef>> byNamespace = new LinkedHashMap<>();
        usedBy.keySet().forEach(ref -> byNamespace.computeIfAbsent(ref.namespace(), ns -> new ArrayList<>()).add(ref));
        for (var group : byNamespace.entrySet()) {
            String gatewayNamespace = group.getKey();
            Map<String, Map<String, Object>> gateways = new LinkedHashMap<>();
            String problem = null;
            try {
                listManifests(kubeConfig, credentials, gatewayNamespace, ResourceKind.GATEWAY)
                        .forEach(gateway -> gateways.put(name(gateway), gateway));
            } catch (Exception e) {
                log.warn("No se pudieron leer los Gateways del namespace {}", gatewayNamespace, e);
                problem = e.getMessage();
            }
            for (IstioRoutes.GatewayRef ref : group.getValue()) {
                Map<String, Object> gateway = gateways.get(ref.name());
                if (gateway != null) {
                    result.add(mapper.toGateway(gateway, usedBy.get(ref)));
                } else {
                    String reason = problem != null ? problem : "no existe en el namespace " + gatewayNamespace;
                    result.add(mapper.unavailableGateway(ref, usedBy.get(ref), reason));
                }
            }
        }
        return result;
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
            case VIRTUAL_SERVICE, GATEWAY ->
                    throw new IllegalArgumentException(kind.k8sKind() + " se asocia vía los Services del deployment");
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
