package co.com.bancolombia.kabrilla.ui.viewmodel.logs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLevel;

/**
 * Filtro de logs inmutable: texto (literal o regex) + nivel mínimo.
 * Una entrada pasa si su nivel es suficiente y alguna de sus líneas contiene el texto.
 */
public final class LogFilter {

    private static final LogFilter NONE = new LogFilter(null, null, null);

    private final Pattern pattern;
    private final LogLevel minLevel;
    private final String error;

    private LogFilter(Pattern pattern, LogLevel minLevel, String error) {
        this.pattern = pattern;
        this.minLevel = minLevel;
        this.error = error;
    }

    public static LogFilter none() {
        return NONE;
    }

    /**
     * @param query         texto a buscar; vacío = sin filtro de texto
     * @param regex         interpreta {@code query} como expresión regular
     * @param caseSensitive distingue mayúsculas/minúsculas
     * @param minLevel      nivel mínimo; {@code null} = todos (incluye líneas sin nivel)
     */
    public static LogFilter of(String query, boolean regex, boolean caseSensitive, LogLevel minLevel) {
        if (query == null || query.isEmpty()) {
            return new LogFilter(null, minLevel, null);
        }
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        try {
            return new LogFilter(Pattern.compile(regex ? query : Pattern.quote(query), flags), minLevel, null);
        } catch (PatternSyntaxException e) {
            // Regex inválida mientras el usuario escribe: no se filtra por texto y se informa el error
            return new LogFilter(null, minLevel, "Regex inválida: " + e.getDescription());
        }
    }

    public boolean matches(LogEntry entry) {
        if (minLevel != null && !entry.level().isAtLeast(minLevel)) {
            return false;
        }
        if (pattern == null) {
            return true;
        }
        for (String line : entry.lines()) {
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rangos [inicio, fin) del texto buscado dentro de la línea, para resaltarlos.
     */
    public List<int[]> highlights(String line) {
        if (pattern == null) {
            return List.of();
        }
        List<int[]> ranges = new ArrayList<>();
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            if (matcher.end() > matcher.start()) {
                ranges.add(new int[]{matcher.start(), matcher.end()});
            }
        }
        return ranges;
    }

    public boolean isActive() {
        return pattern != null || minLevel != null;
    }

    public Optional<String> error() {
        return Optional.ofNullable(error);
    }
}
