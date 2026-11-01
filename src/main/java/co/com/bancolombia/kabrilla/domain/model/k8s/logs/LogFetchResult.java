package co.com.bancolombia.kabrilla.domain.model.k8s.logs;

import java.util.List;

/**
 * @param lines     líneas del rango en orden cronológico
 * @param truncated {@code true} si el rango tenía más líneas que el máximo pedido (se devuelven las primeras)
 */
public record LogFetchResult(List<LogLine> lines, boolean truncated) {
}
