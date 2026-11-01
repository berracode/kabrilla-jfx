package co.com.bancolombia.kabrilla.ui.viewmodel.logs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLevel;

import org.junit.jupiter.api.Test;

class LogFilterTest {

    @Test
    void detectsLevelInPlainTextAndJson() {
        assertEquals(LogLevel.WARN, LogLevel.detect("2025-10-31T08:40:01Z WARN  [orders] Latencia alta: ERROR?"));
        assertEquals(LogLevel.ERROR, LogLevel.detect("{\"timestamp\":\"x\",\"level\":\"error\",\"message\":\"INFO\"}"));
        assertEquals(LogLevel.ERROR, LogLevel.detect("FATAL boom"));
        assertEquals(LogLevel.UNKNOWN, LogLevel.detect("java.lang.OutOfMemoryError: Java heap space"));
    }

    @Test
    void groupsStackTraceWithItsLogLine() {
        LogHistory history = new LogHistory(100);
        add(history, "10:00 INFO  ok");
        LogEntry error = add(history, "10:01 ERROR fallo llamando a inventory");
        assertSame(error, add(history, "java.net.SocketTimeoutException: Read timed out"));
        assertSame(error, add(history, "\tat co.com.orders.Client.call(Client.java:10)"));
        assertSame(error, add(history, "Caused by: java.io.IOException: reset"));
        add(history, "10:02 INFO  siguiente");

        assertEquals(4, error.lines().size());
        assertEquals(LogLevel.ERROR, error.level());
        assertEquals(6, history.lineCount());
    }

    @Test
    void searchingAStackTraceLineShowsTheWholeEntry() {
        LogHistory history = new LogHistory(100);
        add(history, "10:00 INFO  ok");
        add(history, "10:01 ERROR fallo");
        add(history, "java.net.SocketTimeoutException: Read timed out");

        List<String> visible = visibleLines(history, LogFilter.of("sockettimeout", false, false, null));

        assertEquals(List.of("10:01 ERROR fallo", "java.net.SocketTimeoutException: Read timed out"), visible);
    }

    @Test
    void minimumLevelIncludesHigherLevelsAndExcludesUnknown() {
        LogHistory history = new LogHistory(100);
        add(history, "INFO uno");
        add(history, "WARN dos");
        add(history, "ERROR tres");
        add(history, "sin nivel");

        assertEquals(List.of("WARN dos", "ERROR tres"), visibleLines(history, LogFilter.of("", false, false,
                                                                                            LogLevel.WARN)));
        assertEquals(4, visibleLines(history, LogFilter.of("", false, false, null)).size());
    }

    @Test
    void regexAndCaseSensitivityAndHighlights() {
        LogFilter regex = LogFilter.of("timeout-ms: \\d+", true, false, null);
        assertArrayEquals(new int[]{7, 23}, regex.highlights("orders timeout-ms: 3000").getFirst());

        LogFilter literal = LogFilter.of("a.b", false, true, null);
        assertTrue(literal.highlights("axb").isEmpty(), "En modo literal el punto no es comodín");
        assertTrue(LogFilter.of("Error", false, true, null).highlights("error").isEmpty());
    }

    @Test
    void invalidRegexReportsErrorAndDoesNotFilterByText() {
        LogFilter filter = LogFilter.of("([", true, false, null);

        assertTrue(filter.error().isPresent());
        assertFalse(filter.isActive());
    }

    @Test
    void historyEvictsWholeOldestEntries() {
        LogHistory history = new LogHistory(3);
        add(history, "ERROR viejo");
        add(history, "\tat stack");
        add(history, "INFO a");
        add(history, "INFO b");

        assertEquals(List.of("INFO a", "INFO b"), visibleLines(history, LogFilter.none()));
    }

    private static LogEntry add(LogHistory history, String line) {
        return history.add(line, line);
    }

    private static List<String> visibleLines(LogHistory history, LogFilter filter) {
        List<String> lines = new ArrayList<>();
        for (LogEntry entry : history.entries()) {
            if (filter.matches(entry)) {
                lines.addAll(entry.lines());
            }
        }
        return lines;
    }
}
