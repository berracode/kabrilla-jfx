package co.com.bancolombia.kabrilla.ui.viewmodel.logs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;

import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLevel;

/**
 * Historial acotado de logs agrupado en {@link LogEntry}. Es lo que se vuelve a filtrar cuando cambia la búsqueda.
 */
public class LogHistory {

    // Línea de excepción de Java: "java.net.SocketTimeoutException: Read timed out"
    private static final Pattern EXCEPTION_HEADER =
            Pattern.compile("^[a-zA-Z_$][\\w$]*(\\.[\\w$]+)+(Exception|Error|Throwable)\\b.*");

    private final int maxLines;
    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private int lineCount;

    public LogHistory(int maxLines) {
        this.maxLines = maxLines;
    }

    /**
     * Agrega una línea y devuelve la entrada a la que quedó asociada (nueva, o la última si es continuación).
     *
     * @param rawText     texto original del log (se usa para detectar nivel y continuaciones)
     * @param displayText texto tal como se muestra en la vista
     */
    public LogEntry add(String rawText, String displayText) {
        LogEntry last = entries.peekLast();
        LogEntry target;
        if (last != null && isContinuation(rawText)) {
            last.addLine(displayText);
            target = last;
        } else {
            target = new LogEntry(LogLevel.detect(rawText), displayText);
            entries.addLast(target);
        }
        lineCount++;
        evictOverflow();
        return target;
    }

    public Iterable<LogEntry> entries() {
        return entries;
    }

    public int lineCount() {
        return lineCount;
    }

    public int visibleLineCount() {
        int count = 0;
        for (LogEntry entry : entries) {
            if (entry.isVisible()) {
                count += entry.lines().size();
            }
        }
        return count;
    }

    public void clear() {
        entries.clear();
        lineCount = 0;
    }

    static boolean isContinuation(String rawText) {
        if (rawText.isEmpty() || Character.isWhitespace(rawText.charAt(0))) {
            return true; // "\tat ...", líneas en blanco o indentadas dentro de un stack trace
        }
        return rawText.startsWith("Caused by:")
                || rawText.startsWith("Suppressed:")
                || rawText.startsWith("...")
                || EXCEPTION_HEADER.matcher(rawText).matches();
    }

    private void evictOverflow() {
        // Se eliminan entradas completas desde la más antigua, nunca la última (puede seguir recibiendo líneas)
        while (lineCount > maxLines && entries.size() > 1) {
            lineCount -= entries.removeFirst().lines().size();
        }
    }
}
