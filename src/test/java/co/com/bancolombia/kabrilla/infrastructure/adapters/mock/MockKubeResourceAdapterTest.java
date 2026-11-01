package co.com.bancolombia.kabrilla.infrastructure.adapters.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.KubeResource;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;

import org.junit.jupiter.api.Test;

/**
 * Verifica la asociación recurso ↔ deployment (lógica compartida con el adaptador real) sobre mock-data/resources.
 */
class MockKubeResourceAdapterTest {

    private static final KubeConfig MOCK_QA = KubeConfig.builder()
            .clusterName("arn:aws:eks:us-east-1:000000000000:cluster/mock-qa")
            .build();

    private final MockKubeResourceAdapter adapter = new MockKubeResourceAdapter(Path.of("mock-data", "resources"));

    @Test
    void servicesAreMatchedBySelector() throws Exception {
        assertEquals(List.of("orders-api"), names(find("orders-api", ResourceKind.SERVICE)));
    }

    @Test
    void configMapsAreMatchedByPodTemplateReferences() throws Exception {
        List<KubeResource> configMaps = find("orders-api", ResourceKind.CONFIG_MAP);

        // envFrom, env.valueFrom y volumen; kube-root-ca.crt no está referenciado
        assertEquals(List.of("feature-flags", "orders-api-env", "orders-api-files"), names(configMaps));
        String yaml = configMaps.get(2).yaml();
        assertTrue(yaml.contains("application-qa.yaml: |"), "Los archivos multilínea se muestran como bloque:\n" + yaml);
    }

    @Test
    void hpaIsMatchedByScaleTargetRefAndSummarizesMetrics() throws Exception {
        KubeResource hpa = find("orders-api", ResourceKind.HPA).getFirst();

        assertEquals("2 – 6", hpa.summary().get("Réplicas mín – máx"));
        assertEquals(new KubeResource.Entry("cpu", "actual 43% · objetivo 70%"), hpa.entries().getFirst());
        assertTrue(find("catalog-api", ResourceKind.HPA).isEmpty());
    }

    @Test
    void istioPoliciesIncludeNamespaceWideOnes() throws Exception {
        List<KubeResource> policies = find("catalog-api", ResourceKind.AUTHORIZATION_POLICY);

        assertEquals(List.of("deny-other-namespaces"), names(policies));
        assertEquals("Todo el namespace", policies.getFirst().summary().get("Aplica a"));
        assertEquals(List.of("deny-other-namespaces", "orders-api-allow"),
                     names(find("orders-api", ResourceKind.AUTHORIZATION_POLICY)));
    }

    private List<KubeResource> find(String deployment, ResourceKind kind) throws Exception {
        return adapter.findByDeployment(MOCK_QA, null, "ventas-qa", deployment, kind);
    }

    private static List<String> names(List<KubeResource> resources) {
        return resources.stream().map(KubeResource::name).toList();
    }
}
