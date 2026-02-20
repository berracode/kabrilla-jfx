package co.com.bancolombia.kabrilla.domain.model.k8s.logs;

import java.time.Instant;

public record LogLine(Instant timestamp, String text) {
    public static LogLine now(String text) {
        return new LogLine(Instant.now(), text);
    }
}


