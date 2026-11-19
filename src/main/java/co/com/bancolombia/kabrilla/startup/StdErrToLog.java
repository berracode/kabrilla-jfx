package co.com.bancolombia.kabrilla.startup;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Envía al log (logger {@code stderr}, nivel WARN) todo lo que se escriba en {@code System.err}.
 * Sin consola ({@code Kabrilla.exe}) stderr se pierde: trazas de librerías, avisos de JavaFX (CSS, FXML) o un
 * {@code printStackTrace} olvidado. Cada línea es una entrada del log.
 */
final class StdErrToLog extends OutputStream {

    private static final Logger log = LoggerFactory.getLogger("stderr");

    private final ByteArrayOutputStream line = new ByteArrayOutputStream();
    private final Consumer<String> sink;
    private final PrintStream fallback;
    // Si el logger escribiera en stderr (p. ej. un error interno de logback) se evita un ciclo infinito
    private final ThreadLocal<Boolean> inSink = ThreadLocal.withInitial(() -> false);

    StdErrToLog(Consumer<String> sink, PrintStream fallback) {
        this.sink = sink;
        this.fallback = fallback;
    }

    /**
     * Reemplaza {@code System.err}. Llamar una sola vez, al arrancar.
     */
    static void install() {
        PrintStream original = System.err;
        System.setErr(new PrintStream(new StdErrToLog(log::warn, original), true, StandardCharsets.UTF_8));
    }

    @Override
    public synchronized void write(int b) {
        if (inSink.get()) {
            fallback.write(b);
            return;
        }
        if (b == '\n') {
            emit();
        } else {
            line.write(b);
        }
    }

    @Override
    public synchronized void write(byte[] bytes, int off, int len) {
        for (int i = off; i < off + len; i++) {
            write(bytes[i]);
        }
    }

    @Override
    public synchronized void close() {
        if (line.size() > 0) {
            emit();
        }
    }

    private void emit() {
        String text = line.toString(StandardCharsets.UTF_8);
        line.reset();
        if (text.endsWith("\r")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.isBlank()) {
            return;
        }
        inSink.set(true);
        try {
            sink.accept(text);
        } finally {
            inSink.set(false);
        }
    }
}
