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

    @Test
    void virtualServicesAreMatchedByRoutesToDeploymentServices() throws Exception {
        // orders-api: su VS (host FQDN). catalog-api: el suyo y el de orders (ruta /api/v1/catalog). inventory: ninguno
        assertEquals(List.of("orders-api"), names(find("orders-api", ResourceKind.VIRTUAL_SERVICE)));
        List<KubeResource> catalog = find("catalog-api", ResourceKind.VIRTUAL_SERVICE);
        assertEquals(List.of("catalog-api", "orders-api"), names(catalog));

        KubeResource shared = catalog.get(1);
        assertEquals("catalog-api", shared.summary().get("Services del deployment"));
        assertEquals("istio-ingress/public-gateway", shared.summary().get("Gateways"));
        String ordersRoute = shared.entries().get(0).value();
        String catalogRoute = shared.entries().get(1).value();
        assertTrue(catalogRoute.contains("→ catalog-api:80   ← este deployment"), catalogRoute);
        assertTrue(!ordersRoute.contains("este deployment"), "la ruta a otro deployment no se marca: " + ordersRoute);
        assertTrue(catalogRoute.startsWith("Si: uri prefix=/api/v1/catalog"), catalogRoute);

        String canary = catalog.getFirst().entries().getFirst().value();
        assertTrue(canary.contains("(subset v2) · peso 90%"), canary);
    }

    @Test
    void gatewaysComeFromVirtualServicesIncludingOtherNamespaces() throws Exception {
        List<KubeResource> gateways = find("catalog-api", ResourceKind.GATEWAY);

        // "mesh" no es un Gateway; public-gateway vive en istio-ingress
        assertEquals(List.of("public-gateway", "ventas-internal"), names(gateways));
        KubeResource publicGateway = gateways.getFirst();
        assertEquals("istio-ingress", publicGateway.summary().get("Namespace"));
        assertEquals("istio=ingressgateway", publicGateway.summary().get("Pods de entrada"));
        assertEquals("orders-api", publicGateway.summary().get("Usado por"));
        assertTrue(publicGateway.entries().getFirst().value().startsWith("HTTPS 443 (https)\nHosts: *.qa.mock"),
                   publicGateway.entries().getFirst().value());
    }

    @Test
    void missingGatewayIsShownWithReason() throws Exception {
        List<KubeResource> gateways = adapter.findByDeployment(MOCK_QA, null, "pagos-qa", "payments-api",
                                                               ResourceKind.GATEWAY);

        assertEquals(List.of("pagos-gateway"), names(gateways));
        assertEquals("⚠ no existe en el namespace pagos-qa", gateways.getFirst().summary().get("Estado"));
        assertTrue(adapter.findByDeployment(MOCK_QA, null, "pagos-qa", "payments-worker", ResourceKind.GATEWAY)
                           .isEmpty(), "sin Service no hay ruta de Istio");
    }

    private List<KubeResource> find(String deployment, ResourceKind kind) throws Exception {
        return adapter.findByDeployment(MOCK_QA, null, "ventas-qa", deployment, kind);
    }

    private static List<String> names(List<KubeResource> resources) {
        return resources.stream().map(KubeResource::name).toList();
    }
}
