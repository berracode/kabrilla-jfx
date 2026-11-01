package co.com.bancolombia.kabrilla.domain.model.k8s;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class ContainerSelectorTest {

    @Test
    void skipsKnownSidecarsEvenWhenDeclaredFirst() {
        assertEquals("orders-api", ContainerSelector.pickDefault(List.of("istio-proxy", "orders-api"), null));
    }

    @Test
    void annotationWinsWhenItNamesAnExistingContainer() {
        List<String> containers = List.of("app", "worker", "istio-proxy");

        assertEquals("worker", ContainerSelector.pickDefault(containers, "worker"));
        assertEquals("app", ContainerSelector.pickDefault(containers, "does-not-exist"));
    }

    @Test
    void fallsBackToFirstContainerOrNull() {
        assertEquals("istio-proxy", ContainerSelector.pickDefault(List.of("istio-proxy"), null));
        assertEquals("only", ContainerSelector.pickDefault(List.of("only"), null));
        assertNull(ContainerSelector.pickDefault(List.of(), null));
    }
}
