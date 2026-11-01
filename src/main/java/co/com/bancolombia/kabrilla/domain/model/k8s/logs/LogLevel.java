package co.com.bancolombia.kabrilla.domain.model.k8s.logs;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Severidad de una línea de log, detectada a partir de su texto (formato texto plano o JSON).
 */
public enum LogLevel {
    TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4), UNKNOWN(-1);

    // JSON: "level":"warn", "severity":"ERROR", "lvl":"info"
    private static final Pattern JSON_LEVEL =
            Pattern.compile("\"(?:level|severity|lvl)\"\\s*:\\s*\"(\\w+)\"", Pattern.CASE_INSENSITIVE);
    // Texto: la primera palabra en mayúsculas que sea un nivel (Spring, Logback, Log4j...)
    private static final Pattern TEXT_LEVEL =
            Pattern.compile("\\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE|CRITICAL)\\b");

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public boolean isAtLeast(LogLevel minimum) {
        return this != UNKNOWN && severity >= minimum.severity;
    }

    public static LogLevel detect(String text) {
        if (text == null || text.isEmpty()) {
            return UNKNOWN;
        }
        Matcher json = JSON_LEVEL.matcher(text);
        if (json.find()) {
            return fromName(json.group(1));
        }
        Matcher plain = TEXT_LEVEL.matcher(text);
        return plain.find() ? fromName(plain.group(1)) : UNKNOWN;
    }

    private static LogLevel fromName(String name) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "TRACE" -> TRACE;
            case "DEBUG" -> DEBUG;
            case "INFO" -> INFO;
            case "WARN", "WARNING" -> WARN;
            case "ERROR", "FATAL", "SEVERE", "CRITICAL" -> ERROR;
            default -> UNKNOWN;
        };
    }
}
