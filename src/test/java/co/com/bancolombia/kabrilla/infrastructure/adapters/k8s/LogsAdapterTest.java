package co.com.bancolombia.kabrilla.infrastructure.adapters.k8s;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;

import org.junit.jupiter.api.Test;

class LogsAdapterTest {

    @Test
    void parsesKubeletTimestampPrefix() {
        LogLine line = LogsAdapter.parseTimestamped("2026-11-01T14:32:10.123456789Z 2026-11-01 INFO  hola mundo");

        assertEquals(Instant.parse("2026-11-01T14:32:10.123456789Z"), line.timestamp());
        assertEquals("2026-11-01 INFO  hola mundo", line.text());
    }

    @Test
    void keepsWholeLineWhenThereIsNoTimestampPrefix() {
        LogLine line = LogsAdapter.parseTimestamped("\tat co.com.Foo.bar(Foo.java:10)");

        assertEquals("\tat co.com.Foo.bar(Foo.java:10)", line.text());
    }
}
