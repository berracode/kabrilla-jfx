package co.com.bancolombia.kabrilla.domain.model.k8s.logs;

import java.time.Instant;

/**
 * Consulta histórica de logs (sin follow).
 *
 * @param since    desde (inclusive); {@code null} = todo lo que Kubernetes conserve
 * @param until    hasta (inclusive); {@code null} = hasta ahora
 * @param previous logs del container anterior (el que terminó antes del último reinicio)
 */
public record LogRange(Instant since, Instant until, boolean previous) {
}
