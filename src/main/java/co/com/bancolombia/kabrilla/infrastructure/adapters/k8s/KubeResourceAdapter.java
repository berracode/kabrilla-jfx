package co.com.bancolombia.kabrilla.infrastructure.adapters.k8s;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;
import co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.ManifestResourceRepository;
import co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests;
import co.com.bancolombia.kabrilla.infrastructure.config.k8s.KubeClientFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV2Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import lombok.extern.slf4j.Slf4j;

/**
 * Lee Services, HPAs, ConfigMaps y políticas de Istio desde la API de Kubernetes (EKS).
 * Requiere permisos RBAC de lectura (get/list) sobre esos recursos en el namespace.
 */
@Slf4j
public class KubeResourceAdapter extends ManifestResourceRepository {

    private static final String ISTIO_SECURITY_GROUP = "security.istio.io";
    // Istio >= 1.22 expone v1; versiones anteriores solo v1beta1
    private static final List<String> ISTIO_VERSIONS = List.of("v1", "v1beta1");

    private final KubeClientFactory kubeClientFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KubeResourceAdapter(KubeClientFactory kubeClientFactory) {
        this.kubeClientFactory = kubeClientFactory;
    }

    @Override
    protected Map<String, Object> findDeployment(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                                 String deploymentName) throws Exception {
        ApiClient client = kubeClientFactory.forCluster(kubeConfig, credentials);
        try {
            return toMap(new AppsV1Api(client).readNamespacedDeployment(deploymentName, namespace).execute(),
                         "apps/v1", "Deployment");
        } catch (ApiException e) {
            throw describe(e, "Deployment " + deploymentName);
        }
    }

    @Override
    protected List<Map<String, Object>> listManifests(KubeConfig kubeConfig, AwsCredentials credentials,
                                                      String namespace, ResourceKind kind) throws Exception {
        ApiClient client = kubeClientFactory.forCluster(kubeConfig, credentials);
        try {
            return switch (kind) {
                case SERVICE -> new CoreV1Api(client).listNamespacedService(namespace).execute().getItems()
                        .stream().map(s -> toMap(s, "v1", "Service")).toList();
                case CONFIG_MAP -> new CoreV1Api(client).listNamespacedConfigMap(namespace).execute().getItems()
                        .stream().map(c -> toMap(c, "v1", "ConfigMap")).toList();
                case HPA -> new AutoscalingV2Api(client).listNamespacedHorizontalPodAutoscaler(namespace).execute()
                        .getItems().stream().map(h -> toMap(h, "autoscaling/v2", "HorizontalPodAutoscaler"))
                        .toList();
                case AUTHORIZATION_POLICY -> listIstio(client, namespace, "authorizationpolicies", kind);
                case REQUEST_AUTHENTICATION -> listIstio(client, namespace, "requestauthentications", kind);
            };
        } catch (ApiException e) {
            throw describe(e, kind.k8sKind());
        }
    }

    private List<Map<String, Object>> listIstio(ApiClient client, String namespace, String plural,
                                                ResourceKind kind) throws ApiException {
        CustomObjectsApi api = new CustomObjectsApi(client);
        for (String version : ISTIO_VERSIONS) {
            try {
                Object response = api.listNamespacedCustomObject(ISTIO_SECURITY_GROUP, version, namespace, plural)
                        .execute();
                String apiVersion = ISTIO_SECURITY_GROUP + "/" + version;
                return Manifests.list(toMap(response, null, null), "items").stream()
                        .map(item -> withTypeMeta(Manifests.map(item), apiVersion, kind.k8sKind()))
                        .toList();
            } catch (ApiException e) {
                if (e.getCode() != 404) {
                    throw e;
                }
                log.info("{}/{} no disponible en el cluster, probando la siguiente versión", plural, version);
            }
        }
        log.info("Istio ({}) no está instalado en el cluster", plural);
        return List.of();
    }

    /**
     * Modelo del cliente → Map con el mismo formato de un YAML (vía el JSON del propio cliente).
     * Los items de un list no traen apiVersion/kind, así que se agregan para que el YAML copiado sea aplicable.
     */
    private Map<String, Object> toMap(Object model, String apiVersion, String kind) {
        try {
            Map<String, Object> map = objectMapper.readValue(JSON.serialize(model), new TypeReference<>() {
            });
            return apiVersion == null ? map : withTypeMeta(map, apiVersion, kind);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo convertir " + model.getClass().getSimpleName(), e);
        }
    }

    private static Map<String, Object> withTypeMeta(Map<String, Object> manifest, String apiVersion, String kind) {
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("apiVersion", apiVersion);
        ordered.put("kind", kind);
        manifest.forEach((k, v) -> {
            if (!"apiVersion".equals(k) && !"kind".equals(k)) {
                ordered.put(k, v);
            }
        });
        return ordered;
    }

    private static RuntimeException describe(ApiException e, String what) {
        return K8sErrors.from(e, "leer " + what);
    }
}
