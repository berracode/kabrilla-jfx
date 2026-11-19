package co.com.bancolombia.kabrilla.infrastructure.adapters.k8s;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class K8sErrorsTest {

    @Test
    void unauthorizedTellsUserToRenewCredentials() {
        assertEquals("No se pudo listar los deployments de pagos-qa (HTTP 401): " + K8sErrors.EXPIRED_CREDENTIALS,
                     K8sErrors.message(401, "{\"message\":\"Unauthorized\"}", "listar los deployments de pagos-qa"));
    }

    @Test
    void otherErrorsKeepKubernetesExplanation() {
        String body = "{\"kind\":\"Status\",\"message\":\"previous terminated container \\\"app\\\" not found\"}";

        assertEquals("No se pudo leer los logs del pod p1 (HTTP 400): previous terminated container \"app\" not found",
                     K8sErrors.message(400, body, "leer los logs del pod p1"));
    }

    @Test
    void nonJsonBodyIsShownAsIs() {
        assertEquals("bad gateway", K8sErrors.statusMessage("bad gateway"));
        assertEquals("sin detalle en la respuesta", K8sErrors.statusMessage(""));
    }
}
