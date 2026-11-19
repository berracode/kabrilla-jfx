package co.com.bancolombia.kabrilla.infrastructure.adapters.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.IstioRoutes.GatewayRef;

import org.junit.jupiter.api.Test;

class IstioRoutesTest {

    @Test
    void recognizesAllKubernetesFormsOfAServiceHost() {
        assertEquals("orders-api", IstioRoutes.serviceOfHost("orders-api", "ventas-qa"));
        assertEquals("orders-api", IstioRoutes.serviceOfHost("orders-api.ventas-qa", "ventas-qa"));
        assertEquals("orders-api", IstioRoutes.serviceOfHost("orders-api.ventas-qa.svc", "ventas-qa"));
        assertEquals("orders-api", IstioRoutes.serviceOfHost("orders-api.ventas-qa.svc.cluster.local", "ventas-qa"));
    }

    @Test
    void ignoresOtherNamespacesExternalHostsAndWildcards() {
        assertNull(IstioRoutes.serviceOfHost("orders-api.pagos-qa.svc.cluster.local", "ventas-qa"));
        assertNull(IstioRoutes.serviceOfHost("api.example.com", "ventas-qa"));
        assertNull(IstioRoutes.serviceOfHost("*.ventas-qa.svc.cluster.local", "ventas-qa"));
    }

    @Test
    void findsServicesInHttpTcpAndTlsRoutes() {
        Map<String, Object> vs = Map.of("spec", Map.of(
                "http", List.of(Map.of("route", List.of(destination("orders-api"), destination("otro")))),
                "tcp", List.of(Map.of("route", List.of(destination("orders-grpc.ventas-qa")))),
                "tls", List.of(Map.of("route", List.of(destination("externo.example.com"))))));

        assertEquals(Set.of("orders-api", "orders-grpc"),
                     IstioRoutes.routedServices(vs, Set.of("orders-api", "orders-grpc", "nada"), "ventas-qa"));
    }

    @Test
    void resolvesGatewayReferencesWithoutMesh() {
        Map<String, Object> vs = Map.of(
                "metadata", Map.of("namespace", "ventas-qa"),
                "spec", Map.of("gateways", List.of("mesh", "local-gw", "istio-ingress/public-gw",
                                                   "otro-gw.istio-system.svc.cluster.local")));

        assertEquals(List.of(new GatewayRef("ventas-qa", "local-gw"),
                             new GatewayRef("istio-ingress", "public-gw"),
                             new GatewayRef("istio-system", "otro-gw")),
                     IstioRoutes.gatewayRefs(vs, "ventas-qa"));
    }

    private static Map<String, Object> destination(String host) {
        return Map.of("destination", Map.of("host", host));
    }
}
