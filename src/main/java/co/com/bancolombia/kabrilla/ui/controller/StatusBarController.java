package co.com.bancolombia.kabrilla.ui.controller;

import co.com.bancolombia.kabrilla.ui.viewmodel.terminal.TerminalPanelViewModel;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Barra de estado inferior: quién abrió la app, contexto actual de kubectl, acceso a la terminal y versión.
 */
@Component
public class StatusBarController {

    private final TerminalPanelViewModel terminal;
    private final String version;

    @FXML
    private Label userLabel;
    @FXML
    private Label contextLabel;
    @FXML
    private Button terminalButton;
    @FXML
    private Label versionLabel;

    public StatusBarController(TerminalPanelViewModel terminal, @Value("${kabrilla.version:dev}") String version) {
        this.terminal = terminal;
        this.version = version;
    }

    @FXML
    public void initialize() {
        // Usuario de Windows con el que se inició sesión (no el nombre de la carpeta del perfil, que puede diferir)
        userLabel.setText(System.getProperty("user.name"));
        userLabel.setTooltip(new Tooltip("Usuario de Windows que abrió Kabrilla"));

        contextLabel.textProperty().bind(Bindings.concat("kubectl: ", terminal.contextProperty()));
        contextLabel.setTooltip(new Tooltip("Contexto actual de kubectl en " + terminal.kubeconfig()
                                                    + "\n(el que usan las terminales)"));
        terminal.startWatchingContext();

        terminal.visibleProperty().addListener((obs, was, visible) -> updateTerminalButton(visible));
        updateTerminalButton(terminal.visibleProperty().get());

        versionLabel.setText("v" + version);
    }

    @FXML
    private void onToggleTerminal() {
        terminal.toggle();
    }

    private void updateTerminalButton(boolean panelVisible) {
        terminalButton.getStyleClass().remove("kb-status-active");
        if (panelVisible) {
            terminalButton.getStyleClass().add("kb-status-active");
        }
    }
}
