package co.com.bancolombia.kabrilla.ui.controller;

import co.com.bancolombia.kabrilla.ui.viewmodel.terminal.TerminalPanelViewModel;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MainController {

    // Alto del panel de terminal la primera vez (fracción del área de trabajo); luego se recuerda el del usuario
    private static final double DEFAULT_WORK_DIVIDER = 0.62;

    private final TerminalPanelViewModel terminalViewModel;
    private double workDivider = DEFAULT_WORK_DIVIDER;

    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private SplitPane debugSplitPane;
    @FXML
    private SplitPane workSplitPane;
    // Raíz de terminal/terminal-panel.fxml (fx:include con fx:id)
    @FXML
    private Node terminalPanel;

    public MainController(TerminalPanelViewModel terminalViewModel) {
        this.terminalViewModel = terminalViewModel;
    }

    @FXML
    public void initialize() {
        mainSplitPane.setDividerPosition(0, 250.0 / mainSplitPane.getWidth());

        mainSplitPane.widthProperty().addListener((obs, oldW, newW) -> {
            mainSplitPane.setDividerPosition(0, 250.0 / newW.doubleValue());
        });

        debugSplitPane.setDividerPositions(0.9); // más alto para el contenido

        debugSplitPane.setDividerPosition(0, 1.0 - (250.0 / debugSplitPane.getHeight()));
        debugSplitPane.heightProperty().addListener((obs, oldH, newH) -> {
            debugSplitPane.setDividerPosition(0, 1.0 - (250.0 / newH.doubleValue()));
        });

        initializeTerminalPanel();
    }

    /**
     * El panel de terminal empieza oculto. Se muestra/oculta con el botón del sidebar o Ctrl+ñ / Ctrl+` (como VS Code),
     * también con el foco dentro de una terminal.
     */
    private void initializeTerminalPanel() {
        workSplitPane.getItems().remove(terminalPanel);
        terminalViewModel.visibleProperty().addListener((obs, was, visible) -> {
            if (visible && !workSplitPane.getItems().contains(terminalPanel)) {
                workSplitPane.getItems().add(terminalPanel);
                workSplitPane.setDividerPosition(0, workDivider);
            } else if (!visible && workSplitPane.getItems().contains(terminalPanel)) {
                workDivider = workSplitPane.getDividerPositions()[0];
                workSplitPane.getItems().remove(terminalPanel);
            }
        });
        workSplitPane.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerTerminalShortcut(scene);
            }
        });
    }

    private void registerTerminalShortcut(Scene scene) {
        // Filtro (no handler): la terminal consume las teclas, y el atajo debe funcionar también desde ella
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isTerminalShortcut(event)) {
                terminalViewModel.toggle();
                event.consume();
            }
        });
    }

    /**
     * Ctrl+` en teclado inglés; Ctrl+ñ en teclado español (donde ` es tecla muerta), igual que VS Code.
     */
    static boolean isTerminalShortcut(KeyEvent event) {
        if (!event.isShortcutDown() || event.isAltDown() || event.isShiftDown()) {
            return false;
        }
        return event.getCode() == KeyCode.BACK_QUOTE || "ñ".equalsIgnoreCase(event.getText());
    }
}
