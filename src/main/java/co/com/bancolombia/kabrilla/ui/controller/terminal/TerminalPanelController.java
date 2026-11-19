package co.com.bancolombia.kabrilla.ui.controller.terminal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.com.bancolombia.kabrilla.ui.services.ErrorMessages;
import co.com.bancolombia.kabrilla.ui.terminal.KabrillaTerminalSettings;
import co.com.bancolombia.kabrilla.ui.terminal.PtyTtyConnector;
import co.com.bancolombia.kabrilla.ui.terminal.ShellCommand;
import co.com.bancolombia.kabrilla.ui.terminal.ShellLauncher;
import co.com.bancolombia.kabrilla.ui.viewmodel.terminal.TerminalPanelViewModel;

import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Panel inferior de terminales (estilo VS Code), transversal a las pestañas de deployment. Cada pestaña es un shell
 * propio en una pseudo-terminal; todas usan el kubeconfig del usuario, como una terminal externa.
 */
@Slf4j
@Component
public class TerminalPanelController implements DisposableBean {

    private static final int INITIAL_COLUMNS = 120;
    private static final int INITIAL_ROWS = 30;

    private final TerminalPanelViewModel viewModel;
    private final List<String> shell;
    private final Map<String, String> extraEnvironment;
    private final List<Session> sessions = new ArrayList<>();
    private int counter;

    @FXML
    private Label contextLabel;
    @FXML
    private TabPane terminalTabs;
    // Etiquetas invisibles: el tema (CSS) les da los colores que luego usa el emulador
    @FXML
    private Label colorProbe;
    @FXML
    private Label selectionProbe;

    public TerminalPanelController(TerminalPanelViewModel viewModel,
                                   @Value("${kabrilla.terminal.shell:}") String shell,
                                   @Value("${kabrilla.kubeconfig:}") String kubeconfig) {
        this.viewModel = viewModel;
        this.shell = parseShell(shell);
        // Si Kabrilla usa un kubeconfig propio, la terminal también; si no, hereda el entorno tal cual
        this.extraEnvironment = kubeconfig.isBlank() ? Map.of() : Map.of("KUBECONFIG", kubeconfig.trim());
    }

    @FXML
    public void initialize() {
        contextLabel.textProperty().bind(Bindings.concat("kubectl: ", viewModel.contextProperty()));
        contextLabel.setTooltip(new Tooltip("Contexto actual del kubeconfig (" + viewModel.kubeconfig() + ").\n"
                                                    + "Se actualiza si lo cambias, p. ej. con kubectl config "
                                                    + "use-context."));
        viewModel.visibleProperty().addListener((obs, was, visible) -> {
            if (visible) {
                // Después de que la ventana agregue el panel a la escena (los colores salen del CSS aplicado)
                Platform.runLater(() -> {
                    if (sessions.isEmpty()) {
                        openTerminal();
                    } else {
                        focusSelected();
                    }
                });
            }
        });
        terminalTabs.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> focusSelected());
    }

    @FXML
    private void onNewTerminal() {
        openTerminal();
    }

    @FXML
    private void onHide() {
        viewModel.hide();
    }

    private void openTerminal() {
        viewModel.startWatchingContext();
        String name = ShellCommand.displayName(shell) + " " + (++counter);
        try {
            PtyTtyConnector connector = new ShellLauncher(shell, Path.of(System.getProperty("user.home")))
                    .start(name, extraEnvironment, INITIAL_COLUMNS, INITIAL_ROWS);
            JediTermFxWidget widget = new JediTermFxWidget(INITIAL_COLUMNS, INITIAL_ROWS, settings());
            widget.setTtyConnector(connector);
            widget.start();

            Tab tab = new Tab(name, widget.getPane());
            Session session = new Session(tab, widget, connector);
            sessions.add(session);
            tab.setOnClosed(e -> close(session));
            // "exit" en el shell cierra su pestaña
            connector.getProcess().onExit().thenRun(() -> Platform.runLater(() -> {
                if (sessions.contains(session)) {
                    log.info("Terminal \"{}\" terminó", name);
                    terminalTabs.getTabs().remove(tab);
                    close(session);
                }
            }));
            terminalTabs.getTabs().add(tab);
            terminalTabs.getSelectionModel().select(tab);
            focusSelected();
        } catch (Exception e) {
            log.error("No se pudo abrir la terminal {}", String.join(" ", shell), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Terminal");
            alert.setHeaderText("No se pudo abrir " + String.join(" ", shell));
            alert.setContentText(ErrorMessages.describe(e)
                                         + "\n\nRevisa kabrilla.terminal.shell en %USERPROFILE%\\.kabrilla\\"
                                         + "application.properties.");
            alert.show();
            if (sessions.isEmpty()) {
                viewModel.hide();
            }
        }
    }

    /**
     * Widget de la pestaña (para tests).
     */
    JediTermFxWidget widgetOf(Tab tab) {
        return sessions.stream().filter(s -> s.tab() == tab).map(Session::widget).findFirst().orElse(null);
    }

    private void close(Session session) {
        if (!sessions.remove(session)) {
            return;
        }
        session.close();
        if (sessions.isEmpty()) {
            viewModel.hide();
        }
    }

    private void focusSelected() {
        Tab selected = terminalTabs.getSelectionModel().getSelectedItem();
        sessions.stream()
                .filter(s -> s.tab() == selected)
                .findFirst()
                .ifPresent(s -> Platform.runLater(() -> s.widget().getPreferredFocusableNode().requestFocus()));
    }

    private KabrillaTerminalSettings settings() {
        colorProbe.applyCss();
        selectionProbe.applyCss();
        return new KabrillaTerminalSettings(textColor(colorProbe, Color.BLACK), backgroundColor(colorProbe, Color.WHITE),
                                            textColor(selectionProbe, Color.WHITE),
                                            backgroundColor(selectionProbe, Color.STEELBLUE));
    }

    private static Color textColor(Label probe, Color fallback) {
        Paint paint = probe.getTextFill();
        return paint instanceof Color color ? color : fallback;
    }

    private static Color backgroundColor(Label probe, Color fallback) {
        Background background = probe.getBackground();
        if (background == null || background.getFills().isEmpty()) {
            return fallback;
        }
        return background.getFills().getFirst().getFill() instanceof Color color ? color : fallback;
    }

    private static List<String> parseShell(String value) {
        try {
            return ShellCommand.parse(value);
        } catch (IllegalArgumentException e) {
            log.error("{}. Se usa {}", e.getMessage(), ShellCommand.DEFAULT);
            return ShellCommand.parse(null);
        }
    }

    /**
     * Al cerrar la app: termina todos los shells (y lo que corra en ellos).
     */
    @Override
    public void destroy() {
        new ArrayList<>(sessions).forEach(session -> {
            sessions.remove(session);
            session.close();
        });
    }

    private record Session(Tab tab, JediTermFxWidget widget, PtyTtyConnector connector) {
        void close() {
            try {
                widget.close();
            } finally {
                connector.close();
            }
        }
    }
}
