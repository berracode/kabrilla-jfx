package co.com.bancolombia.kabrilla.domain.model.k8s.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class LogRangeCollectorTest {

    private static final Instant T0 = Instant.parse("2026-11-01T14:00:00Z");

    @Test
    void keepsOnlyLinesInsideRangeAndStopsAfterUntil() {
        LogRangeCollector collector = new LogRangeCollector(
                new LogRange(T0.plusSeconds(10), T0.plusSeconds(30), false), 100);

        assertTrue(collector.offer(line(0, "antes")));
        assertTrue(collector.offer(line(10, "desde exacto")));
        assertTrue(collector.offer(line(30, "hasta exacto")));
        assertFalse(collector.offer(line(31, "después")), "Pasado el 'hasta' no hace falta seguir leyendo");

        assertEquals(List.of("desde exacto", "hasta exacto"), texts(collector.result()));
        assertFalse(collector.result().truncated());
    }

    @Test
    void marksTruncatedWhenMaxLinesIsExceeded() {
        LogRangeCollector collector = new LogRangeCollector(new LogRange(null, null, false), 2);

        collector.offer(line(0, "a"));
        collector.offer(line(1, "b"));
        assertFalse(collector.offer(line(2, "c")));

        assertEquals(List.of("a", "b"), texts(collector.result()));
        assertTrue(collector.result().truncated());
    }

    private static LogLine line(int seconds, String text) {
        return new LogLine(T0.plusSeconds(seconds), text);
    }

    private static List<String> texts(LogFetchResult result) {
        return result.lines().stream().map(LogLine::text).toList();
    }
}
