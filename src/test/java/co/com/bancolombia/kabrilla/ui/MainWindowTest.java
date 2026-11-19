package co.com.bancolombia.kabrilla.ui;

import static co.com.bancolombia.kabrilla.ui.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;
import co.com.bancolombia.kabrilla.startup.MainApplication;
import co.com.bancolombia.kabrilla.startup.StageReadyEvent;
import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.LayoutState;
import co.com.bancolombia.kabrilla.ui.viewmodel.terminal.TerminalPanelViewModel;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Ventana completa en modo mock (Spring + main.fxml): menú, barra de estado, sidebar sin botones de acciones,
 * pestaña de deployment y panel de terminal. Deja una captura en target/main-window.png.
 */
@Tag("ui")
@EnabledOnOs(OS.WINDOWS)
class MainWindowTest {

    // En target/ y no en @TempDir: logback deja abierto logs/kabrilla.log y Windows no permite borrarlo
    private static final Path DATA_DIR = Path.of("target", "main-window-test-data").toAbsolutePath();

    @BeforeAll
    static void startFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @Test
    void mainWindowHasMenuStatusBarAndTerminal() throws Exception {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(MainApplication.class)
                .run("--spring.profiles.active=mock", "--kabrilla.data-dir=" + DATA_DIR,
                     "--kabrilla.terminal.shell=powershell.exe -NoLogo -NoProfile",
                     "--kabrilla.mock.line-delay-ms=20");
        Stage stage = onFx(() -> {
            Stage s = new Stage();
            context.publishEvent(new StageReadyEvent(s));
            return s;
        });
        try {
            Parent root = stage.getScene().getRoot();
            TreeView<?> tree = onFx(() -> (TreeView<?>) root.lookup(".tree-view"));
            waitFor(() -> onFxQuiet(() -> tree.getRoot().getChildren().size() >= 2), "clusters mock en el sidebar");

            // Menú superior con las 4 secciones y sidebar sin los botones de antes
            MenuBar menu = onFx(() -> (MenuBar) root.lookup(".menu-bar"));
            assertEquals(4, menu.getMenus().size());
            assertTrue(onFx(() -> root.lookupAll(".button").stream()
                    .noneMatch(n -> n instanceof Button b && "Agregar credenciales".equals(b.getText()))));

            // Barra de estado: usuario de Windows. Todo acceso a la escena en el hilo de JavaFX: el stream de logs
            // la modifica mientras tanto
            assertTrue(onFx(() -> root.lookup(".kb-status-bar").lookupAll(".label").stream()
                    .anyMatch(n -> System.getProperty("user.name").equals(((Label) n).getText()))));

            onFx(() -> context.getBean(DeploymentStateViewModel.class)
                    .setSelectedDeployment(new Deployment("mock-qa", "ventas-qa", "orders-api")));
            TerminalPanelViewModel terminal = context.getBean(TerminalPanelViewModel.class);
            onFx(terminal::show);
            TabPane terminalTabs = onFx(() -> (TabPane) root.lookup(".kb-terminal-tabs"));
            waitFor(() -> onFxQuiet(() -> terminalTabs.getTabs().size() == 1), "terminal abierta");
            // Ver → Panel de clusters, justo después de abrir la terminal (antes del primer layout del panel)
            LayoutState layout = context.getBean(LayoutState.class);
            onFx(() -> layout.sidebarVisibleProperty().set(false));
            assertFalse(onFx(() -> tree.getScene() != null), "el sidebar se quita de la ventana");
            onFx(() -> layout.sidebarVisibleProperty().set(true));

            Thread.sleep(2500); // que el stream mock y PowerShell dibujen algo
            Node panel = onFx(() -> root.lookup(".kb-terminal-panel"));
            assertTrue(onFx(() -> panel.getScene() != null && panel.getLayoutBounds().getHeight() > 150),
                       "el panel de terminal sigue visible con alto útil: "
                               + onFx(() -> panel.getLayoutBounds().getHeight()));
            snapshot(stage, new File("target/main-window.png"));
        } finally {
            onFx(stage::close);
            context.close();
        }
    }

    private static boolean onFxQuiet(Supplier<Boolean> check) {
        try {
            return onFx(check);
        } catch (Exception e) {
            return false;
        }
    }

    private static void waitFor(Supplier<Boolean> condition, String what) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!condition.get()) {
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
