package co.com.bancolombia.kabrilla.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class StdErrToLogTest {

    private final List<String> logged = new ArrayList<>();
    private final ByteArrayOutputStream fallback = new ByteArrayOutputStream();

    private PrintStream stream(java.util.function.Consumer<String> sink) {
        return new PrintStream(new StdErrToLog(sink, new PrintStream(fallback, true, StandardCharsets.UTF_8)), true,
                               StandardCharsets.UTF_8);
    }

    @Test
    void stackTraceGoesToLogLineByLine() {
        PrintStream err = stream(logged::add);

        new IllegalStateException("falló la carga").printStackTrace(err);

        assertEquals("java.lang.IllegalStateException: falló la carga", logged.getFirst());
        assertEquals(true, logged.get(1).startsWith("\tat "), "segunda línea debe ser un frame: " + logged.get(1));
    }

    @Test
    void handlesWindowsLineEndingsAndUtf8AndSkipsBlankLines() {
        PrintStream err = stream(logged::add);

        err.print("versión inválida\r\n\r\n");
        err.print("sin salto");

        assertEquals(List.of("versión inválida"), logged);
        err.close();
        assertEquals(List.of("versión inválida", "sin salto"), logged);
    }

    @Test
    void writesToFallbackWhenLoggerItselfWritesToStderr() {
        PrintStream[] self = new PrintStream[1];
        self[0] = stream(text -> {
            logged.add(text);
            self[0].println("error interno del logger"); // evitaría un ciclo infinito
        });

        self[0].println("mensaje");

        assertEquals(List.of("mensaje"), logged);
        assertEquals("error interno del logger" + System.lineSeparator(), fallback.toString(StandardCharsets.UTF_8));
    }
}
