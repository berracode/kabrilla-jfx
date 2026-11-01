package co.com.bancolombia.kabrilla.ui.viewmodel.logs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLevel;

/**
 * Un evento de log: la línea principal más sus continuaciones (p. ej. el stack trace de una excepción).
 * El filtro decide sobre la entrada completa para no separar un error de su stack trace.
 */
public final class LogEntry {

    private final LogLevel level;
    private final List<String> lines = new ArrayList<>();
    private boolean visible;

    LogEntry(LogLevel level, String firstLine) {
        this.level = level;
        this.lines.add(firstLine);
    }

    void addLine(String line) {
        lines.add(line);
    }

    public LogLevel level() {
        return level;
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Si la entrada está pintada en la vista con el filtro actual.
     */
    boolean isVisible() {
        return visible;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
    }
}
