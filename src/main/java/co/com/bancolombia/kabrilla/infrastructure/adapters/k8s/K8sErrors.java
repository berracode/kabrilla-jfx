package co.com.bancolombia.kabrilla.infrastructure.adapters.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiException;

/**
 * Mensajes uniformes para errores HTTP de la API de Kubernetes (listados, recursos y logs).
 */
final class K8sErrors {

    static final String EXPIRED_CREDENTIALS =
            "credenciales AWS vencidas o inválidas: pega unas nuevas con \"Agregar credenciales\" y reintenta";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private K8sErrors() {
    }

    /**
     * @param action qué se intentaba, p. ej. "listar deployments en pagos-qa"
     */
    static RuntimeException from(ApiException e, String action) {
        return new RuntimeException(message(e.getCode(), e.getResponseBody(), action), e);
    }

    /**
     * "No se pudo listar deployments en pagos-qa (HTTP 401): credenciales AWS vencidas...".
     */
    static String message(int code, String body, String action) {
        String reason = switch (code) {
            case 401 -> EXPIRED_CREDENTIALS;
            case 403 -> "sin permisos (RBAC) en el cluster";
            default -> statusMessage(body);
        };
        return "No se pudo " + action + " (HTTP " + code + "): " + reason;
    }

    /**
     * Kubernetes explica el error en el body (JSON Status), p. ej. "previous terminated container ... not found".
     */
    static String statusMessage(String body) {
        if (body == null || body.isBlank()) {
            return "sin detalle en la respuesta";
        }
        try {
            return MAPPER.readTree(body).path("message").asText(body);
        } catch (Exception e) {
            return body; // body no es JSON: se muestra tal cual
        }
    }
}
