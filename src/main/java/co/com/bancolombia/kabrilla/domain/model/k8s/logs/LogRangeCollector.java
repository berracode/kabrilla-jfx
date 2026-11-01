package co.com.bancolombia.kabrilla.domain.model.k8s.logs;

import java.util.ArrayList;
import java.util.List;

/**
 * Acumula las líneas (en orden cronológico) que caen dentro de un {@link LogRange}, con un máximo.
 * La API de Kubernetes no tiene parámetro "hasta" ni "desde fecha exacta" (solo sinceSeconds): los adaptadores
 * piden de más y recortan aquí.
 */
public final class LogRangeCollector {

    private final LogRange range;
    private final int maxLines;
    private final List<LogLine> lines = new ArrayList<>();
    private boolean truncated;

    public LogRangeCollector(LogRange range, int maxLines) {
        this.range = range;
        this.maxLines = maxLines;
    }

    /**
     * @return {@code false} cuando ya no hace falta seguir leyendo (pasó el "hasta" o se llenó el máximo)
     */
    public boolean offer(LogLine line) {
        if (range.since() != null && line.timestamp().isBefore(range.since())) {
            return true;
        }
        if (range.until() != null && line.timestamp().isAfter(range.until())) {
            return false;
        }
        if (lines.size() >= maxLines) {
            truncated = true;
            return false;
        }
        lines.add(line);
        return true;
    }

    public LogFetchResult result() {
        return new LogFetchResult(List.copyOf(lines), truncated);
    }
}
