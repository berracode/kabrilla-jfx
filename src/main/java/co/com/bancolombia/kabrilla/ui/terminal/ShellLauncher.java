package co.com.bancolombia.kabrilla.ui.terminal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Arranca el shell de la terminal integrada en una pseudo-terminal.
 * <p>
 * Se comporta como una terminal externa: hereda el entorno del usuario (kubeconfig, ~/.aws, PATH) y solo agrega lo
 * que necesita la emulación ({@code TERM}) y las variables extra que se le pasen (p. ej. {@code KUBECONFIG}).
 */
@Slf4j
public class ShellLauncher {

    private final List<String> command;
    private final Path workingDirectory;

    /**
     * @param command          shell y argumentos, p. ej. {@code [powershell.exe, -NoLogo]}
     * @param workingDirectory carpeta inicial (normalmente el home del usuario)
     */
    public ShellLauncher(List<String> command, Path workingDirectory) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Falta el comando del shell (kabrilla.terminal.shell)");
        }
        this.command = List.copyOf(command);
        this.workingDirectory = workingDirectory;
    }

    public PtyTtyConnector start(String name, Map<String, String> extraEnvironment, int columns, int rows)
            throws IOException {
        Map<String, String> environment = new HashMap<>(System.getenv());
        environment.put("TERM", "xterm-256color");
        environment.putAll(extraEnvironment);

        PtyProcess process = new PtyProcessBuilder(command.toArray(String[]::new))
                .setEnvironment(environment)
                .setDirectory(workingDirectory.toString())
                .setInitialColumns(columns)
                .setInitialRows(rows)
                .setConsole(false)
                .setUseWinConPty(true)
                .start();
        // Nunca loguear el entorno: puede tener credenciales (AWS_SECRET_ACCESS_KEY...)
        log.info("Terminal \"{}\" iniciada: {} (pid {})", name, String.join(" ", command), process.pid());
        return new PtyTtyConnector(process, name, command);
    }

    public List<String> command() {
        return command;
    }
}
