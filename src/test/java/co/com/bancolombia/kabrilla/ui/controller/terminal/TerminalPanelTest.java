package co.com.bancolombia.kabrilla.ui.controller.terminal;

import static co.com.bancolombia.kabrilla.ui.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;

import co.com.bancolombia.kabrilla.ui.FxTestSupport;
import co.com.bancolombia.kabrilla.ui.viewmodel.terminal.TerminalPanelViewModel;

import com.pty4j.PtyProcess;
import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Panel de terminal real (FXML + tema oscuro + PowerShell): abrir, colores del tema, redimensionar y cerrar con exit.
 * Deja una captura en target/terminal-panel.png.
 */
@Tag("ui")
@EnabledOnOs(OS.WINDOWS)
class TerminalPanelTest {

    @BeforeAll
    static void startFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @Test
    void showsWorkingTerminalWithThemeColorsResizesAndClosesOnExit() throws Exception {
        TerminalPanelViewModel viewModel = new TerminalPanelViewModel("");
        TerminalPanelController controller =
                new TerminalPanelController(viewModel, "powershell.exe -NoLogo -NoProfile", "");
        Stage stage = onFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/terminal/terminal-panel.fxml"));
                loader.setControllerFactory(type -> controller);
                Parent root = loader.load();
                Scene scene = new Scene(root, 900, 360);
                scene.getStylesheets().addAll(css("kabrilla-base.css"), css("kabrilla-dark.css"));
                Stage s = new Stage();
                s.setScene(scene);
                s.show();
                return s;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        Parent root = stage.getScene().getRoot();
        TabPane tabs = (TabPane) root.lookup(".kb-terminal-tabs");
        try {
            onFx(viewModel::show);
            waitFor(() -> tabs.getTabs().size() == 1, "se abre una terminal al mostrar el panel");
            JediTermFxWidget widget = onFx(() -> controller.widgetOf(tabs.getTabs().getFirst()));

            // Colores del tema oscuro (-kb-bg) tomados por la etiqueta de muestra
            Label probe = (Label) root.lookup(".kb-terminal-colors");
            assertEquals(Color.web("#0F0F10"), probe.getBackground().getFills().getFirst().getFill());

            widget.getTtyConnector().write("Write-Output \"kabrilla-$((1+1))\"\r");
            waitFor(() -> widget.getTerminalTextBuffer().getScreenLines().contains("kabrilla-2"),
                    "la salida del shell se ve en la terminal");

            PtyProcess process = (PtyProcess) ((com.techsenger.jeditermfx.core.ProcessTtyConnector)
                    widget.getTtyConnector()).getProcess();
            int columnsBefore = process.getWinSize().getColumns();
            onFx(() -> stage.setWidth(1400));
            waitFor(() -> columns(process) > columnsBefore, "el shell recibe más columnas al agrandar");

            Thread.sleep(500);
            snapshot(stage, new File("target/terminal-panel.png"));

            widget.getTtyConnector().write("exit\r");
            waitFor(() -> onFxQuiet(() -> tabs.getTabs().isEmpty()), "exit cierra la pestaña");
            assertFalse(viewModel.visibleProperty().get(), "sin terminales el panel se oculta");
            assertTrue(!process.isAlive());
        } finally {
            onFx(() -> {
                controller.destroy();
                viewModel.destroy();
                stage.close();
            });
        }
    }

    private static int columns(PtyProcess process) {
        try {
            return process.getWinSize().getColumns();
        } catch (Exception e) {
            return -1;
        }
    }

    private String css(String name) {
        return getClass().getResource("/css/" + name).toExternalForm();
    }

    private static boolean onFxQuiet(java.util.function.Supplier<Boolean> check) {
        try {
            return onFx(check);
        } catch (Exception e) {
            return false;
        }
    }

    private static void waitFor(BooleanSupplier condition, String what) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timeout: " + what);
            }
            Thread.sleep(100);
        }
    }

    private static void snapshot(Stage stage, File file) throws Exception {
        WritableImage image = onFx(() -> stage.getScene().snapshot(null));
        PixelReader reader = image.getPixelReader();
        BufferedImage out = new BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                                              BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                out.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        file.getParentFile().mkdirs();
        ImageIO.write(out, "png", file);
    }
}
