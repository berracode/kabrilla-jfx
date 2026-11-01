package co.com.bancolombia.kabrilla.ui.theme;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

/**
 * Interruptor de estilos personalizados. Desactivados, la app usa solo el tema base de JavaFX (Modena):
 * se ignoran las hojas CSS (de FXML y de código) y los estilos en línea (style="..." / setStyle) de todas las
 * ventanas, incluidas pestañas y diálogos creados después. No modifica FXML ni CSS: es solo en tiempo de ejecución.
 * <p>
 * Se controla con la propiedad {@code kabrilla.ui.styles-enabled} (ver README: {@code -Pstyles}).
 */
@Slf4j
public final class StyleToggle {

    private static final String PROCESSED = "kabrilla.styles.stripped";
    private static final String PRESERVED = "kabrilla.styles.preserved";

    private StyleToggle() {
    }

    /**
     * Marca un nodo cuyo estilo es funcional (fuente monoespaciada, resaltado de búsqueda): él y sus hijos
     * conservan sus estilos aunque los personalizados estén desactivados. Llamar antes de agregarlo a la escena.
     */
    public static void preserve(Node node) {
        node.getProperties().put(PRESERVED, Boolean.TRUE);
    }

    /**
     * Debe llamarse en el hilo de JavaFX antes de mostrar la primera ventana.
     */
    public static void disableCustomStyles() {
        log.warn("Estilos personalizados DESACTIVADOS: solo tema base de JavaFX (activar con -Pstyles)");
        Window.getWindows().forEach(StyleToggle::watchWindow);
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(StyleToggle::watchWindow);
            }
        });
    }

    private static void watchWindow(Window window) {
        if (window.getProperties().putIfAbsent(PROCESSED, Boolean.TRUE) != null) {
            return;
        }
        if (window.getScene() != null) {
            watchScene(window.getScene());
        }
        window.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                watchScene(scene);
            }
        });
    }

    private static void watchScene(Scene scene) {
        if (scene.getProperties().putIfAbsent(PROCESSED, Boolean.TRUE) != null) {
            return;
        }
        keepEmpty(scene.getStylesheets());
        if (scene.getRoot() != null) {
            strip(scene.getRoot());
        }
        scene.rootProperty().addListener((obs, old, root) -> {
            if (root != null) {
                strip(root);
            }
        });
    }

    private static void strip(Node node) {
        if (node.getProperties().containsKey(PRESERVED)
                || node.getProperties().putIfAbsent(PROCESSED, Boolean.TRUE) != null) {
            return;
        }
        node.setStyle("");
        node.styleProperty().addListener((obs, old, style) -> {
            if (style != null && !style.isEmpty()) {
                node.setStyle("");
            }
        });

        if (node instanceof Parent parent) {
            keepEmpty(parent.getStylesheets());
            parent.getChildrenUnmodifiable().forEach(StyleToggle::strip);
            parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
                while (change.next()) {
                    change.getAddedSubList().forEach(StyleToggle::strip);
                }
            });
        }
    }

    private static void keepEmpty(ObservableList<String> stylesheets) {
        stylesheets.clear();
        stylesheets.addListener((ListChangeListener<String>) change -> {
            if (!stylesheets.isEmpty()) {
                stylesheets.clear();
            }
        });
    }
}
