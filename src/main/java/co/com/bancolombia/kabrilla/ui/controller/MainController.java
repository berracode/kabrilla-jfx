package co.com.bancolombia.kabrilla.ui.controller;

import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.LayoutState;
import co.com.bancolombia.kabrilla.ui.viewmodel.terminal.TerminalPanelViewModel;

import javafx.application.Platform;
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

    // Ancho del panel de clusters en píxeles
    private static final double SIDEBAR_WIDTH = 250.0;

    private final TerminalPanelViewModel terminalViewModel;
    private final LayoutState layoutState;
    private double workDivider = DEFAULT_WORK_DIVIDER;

    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private SplitPane debugSplitPane;
    @FXML
    private SplitPane workSplitPane;
    // Raíces de terminal/terminal-panel.fxml y sidebar/sidebar-view.fxml (fx:include con fx:id)
    @FXML
    private Node terminalPanel;
    @FXML
    private Node sidebar;

    public MainController(TerminalPanelViewModel terminalViewModel, LayoutState layoutState) {
        this.terminalViewModel = terminalViewModel;
        this.layoutState = layoutState;
    }

    @FXML
    public void initialize() {
        fixSidebarWidth();
        mainSplitPane.widthProperty().addListener((obs, oldW, newW) -> fixSidebarWidth());
        // Menú Ver → Panel de clusters
        layoutState.sidebarVisibleProperty().addListener((obs, was, visible) -> {
            if (visible && !mainSplitPane.getItems().contains(sidebar)) {
                mainSplitPane.getItems().addFirst(sidebar);
                fixSidebarWidth();
            } else if (!visible) {
                mainSplitPane.getItems().remove(sidebar);
            }
        });

        debugSplitPane.setDividerPositions(0.9); // más alto para el contenido

        debugSplitPane.setDividerPosition(0, 1.0 - (250.0 / debugSplitPane.getHeight()));
        debugSplitPane.heightProperty().addListener((obs, oldH, newH) -> {
            debugSplitPane.setDividerPosition(0, 1.0 - (250.0 / newH.doubleValue()));
        });

        initializeTerminalPanel();
    }

    private void fixSidebarWidth() {
        if (mainSplitPane.getItems().contains(sidebar) && mainSplitPane.getWidth() > 0) {
            mainSplitPane.setDividerPosition(0, SIDEBAR_WIDTH / mainSplitPane.getWidth());
        }
    }

    /**
     * El panel de terminal empieza oculto. Se muestra/oculta desde el menú Terminal, la barra de estado o Ctrl+ñ / Ctrl+`
     * (como VS Code),
     * también con el foco dentro de una terminal.
     */
    private void initializeTerminalPanel() {
        workSplitPane.getItems().remove(terminalPanel);
        terminalViewModel.visibleProperty().addListener((obs, was, visible) -> {
            if (visible && !workSplitPane.getItems().contains(terminalPanel)) {
                workSplitPane.getItems().add(terminalPanel);
                workSplitPane.setDividerPosition(0, workDivider);
                // Y otra vez tras el primer layout: fijado antes, SplitPane puede descartarlo y dejar el panel sin alto
                Platform.runLater(() -> workSplitPane.setDividerPosition(0, workDivider));
            } else if (!visible && workSplitPane.getItems().contains(terminalPanel)) {
                workDivider = usableDivider(workSplitPane.getDividerPositions()[0]);
                workSplitPane.getItems().remove(terminalPanel);
            }
        });
        workSplitPane.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                registerTerminalShortcut(scene);
            }
        });
    }

    /**
     * Si el usuario dejó el panel casi cerrado (o casi a pantalla completa) al ocultarlo, al volver se abre con el
     * alto por defecto en vez de reaparecer invisible.
     */
    static double usableDivider(double position) {
        return position > 0.9 || position < 0.1 ? DEFAULT_WORK_DIVIDER : position;
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
