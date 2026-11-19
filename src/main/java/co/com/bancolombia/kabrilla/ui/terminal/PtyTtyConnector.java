package co.com.bancolombia.kabrilla.ui.terminal;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.techsenger.jeditermfx.core.ProcessTtyConnector;
import com.techsenger.jeditermfx.core.util.TermSize;

/**
 * Une el emulador (JediTermFX) con un shell que corre en una pseudo-terminal (pty4j, ConPTY en Windows):
 * lo que escribe el usuario va al shell y su salida se dibuja en el widget.
 */
public class PtyTtyConnector extends ProcessTtyConnector {

    private final PtyProcess process;
    private final String name;

    public PtyTtyConnector(PtyProcess process, String name, List<String> commandLine) {
        super(process, StandardCharsets.UTF_8, commandLine);
        this.process = process;
        this.name = name;
    }

    /**
     * El widget avisa su tamaño en columnas/filas; el shell lo necesita para cortar líneas y para programas de
     * pantalla completa (k9s, less...).
     */
    @Override
    public void resize(TermSize size) {
        if (process.isAlive()) {
            process.setWinSize(new WinSize(size.getColumns(), size.getRows()));
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
