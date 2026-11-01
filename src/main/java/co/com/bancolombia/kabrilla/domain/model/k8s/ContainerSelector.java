package co.com.bancolombia.kabrilla.domain.model.k8s;

import java.util.List;
import java.util.Set;

/**
 * Elige el container cuyos logs mostrar por defecto en un pod con varios containers (p. ej. app + istio-proxy).
 */
public final class ContainerSelector {

    /**
     * Anotación estándar que también usa kubectl para elegir el container por defecto.
     */
    public static final String DEFAULT_CONTAINER_ANNOTATION = "kubectl.kubernetes.io/default-container";

    // Sidecars de infraestructura: casi nunca son los logs que se buscan
    private static final Set<String> KNOWN_SIDECARS = Set.of(
            "istio-proxy", "linkerd-proxy", "envoy", "envoy-sidecar", "aws-xray-daemon", "xray-daemon",
            "datadog-agent", "fluent-bit", "fluentd", "otel-collector", "cloudwatch-agent");

    private ContainerSelector() {
    }

    /**
     * 1) el indicado por la anotación (si existe en el pod), 2) el primero que no sea un sidecar conocido,
     * 3) el primero. {@code null} si el pod no declara containers.
     */
    public static String pickDefault(List<String> containers, String annotatedDefault) {
        if (containers == null || containers.isEmpty()) {
            return null;
        }
        if (annotatedDefault != null && containers.contains(annotatedDefault)) {
            return annotatedDefault;
        }
        return containers.stream()
                .filter(name -> !KNOWN_SIDECARS.contains(name))
                .findFirst()
                .orElse(containers.getFirst());
    }
}
