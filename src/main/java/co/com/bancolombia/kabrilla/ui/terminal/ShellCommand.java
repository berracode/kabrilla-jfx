package co.com.bancolombia.kabrilla.ui.terminal;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte {@code kabrilla.terminal.shell} en comando + argumentos. Separa por espacios y respeta comillas dobles,
 * para rutas con espacios: {@code "C:/Program Files/Git/bin/bash.exe" --login -i}.
 */
public final class ShellCommand {

    public static final String DEFAULT = "powershell.exe -NoLogo";

    private ShellCommand() {
    }

    public static List<String> parse(String value) {
        String text = value == null || value.isBlank() ? DEFAULT : value.trim();
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean hasToken = false;
        for (char c : text.toCharArray()) {
            if (c == '"') {
                quoted = !quoted;
                hasToken = true;
            } else if (Character.isWhitespace(c) && !quoted) {
                if (hasToken) {
                    parts.add(current.toString());
                    current.setLength(0);
                    hasToken = false;
                }
            } else {
                current.append(c);
                hasToken = true;
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("kabrilla.terminal.shell tiene comillas sin cerrar: " + value);
        }
        if (hasToken) {
            parts.add(current.toString());
        }
        return parts;
    }

    /**
     * Nombre corto para la pestaña: "powershell", "pwsh", "bash"...
     */
    public static String displayName(List<String> command) {
        String exe = command.getFirst().replace('\\', '/');
        String name = exe.substring(exe.lastIndexOf('/') + 1);
        return name.toLowerCase().endsWith(".exe") ? name.substring(0, name.length() - 4) : name;
    }
}
