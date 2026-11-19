package co.com.bancolombia.kabrilla.ui.terminal;

import static co.com.bancolombia.kabrilla.ui.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import co.com.bancolombia.kabrilla.ui.FxTestSupport;

import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Prueba de punta a punta: PowerShell real en una pseudo-terminal (ConPTY) dibujado por el emulador JavaFX.
 */
@Tag("ui")
@EnabledOnOs(OS.WINDOWS)
class ShellInTerminalTest {

    @BeforeAll
    static void startFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @Test
    void powershellRunsInsideTheWidget() throws Exception {
        ShellLauncher launcher = new ShellLauncher(List.of("powershell.exe", "-NoLogo", "-NoProfile"),
                                                   Path.of(System.getProperty("user.home")));
        PtyTtyConnector connector = launcher.start("prueba", Map.of("KABRILLA_TEST", "hola-desde-kabrilla"), 120, 30);
        JediTermFxWidget widget = onFx(() -> new JediTermFxWidget(120, 30, new DefaultSettingsProvider()));
        try {
            onFx(() -> {
                widget.setTtyConnector(connector);
                widget.start();
            });

            // La variable extra llega al shell y su salida se dibuja en el buffer del emulador
            connector.write("Write-Output \"valor=$env:KABRILLA_TEST\"\r");

            String screen = waitForScreen(widget, "valor=hola-desde-kabrilla");
            assertTrue(screen.contains("valor=hola-desde-kabrilla"), screen);
        } finally {
            onFx(widget::close);
            connector.close();
        }
    }

    private static String waitForScreen(JediTermFxWidget widget, String expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        String screen = "";
        while (System.nanoTime() < deadline) {
            screen = widget.getTerminalTextBuffer().getScreenLines();
            // Aparece dos veces: el comando escrito y su resultado
            if (screen.indexOf(expected) != screen.lastIndexOf(expected) || screen.contains("\n" + expected)) {
                return screen;
            }
            Thread.sleep(200);
        }
        return screen;
    }
}
