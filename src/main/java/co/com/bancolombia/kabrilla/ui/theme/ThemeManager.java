package co.com.bancolombia.kabrilla.ui.theme;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

/**
 * Aplica las hojas de estilo a todas las ventanas de la app (principal, diálogos, alertas, popups).
 * <ul>
 *   <li>{@code kabrilla-base.css}: siempre (fuente monoespaciada, resaltado de búsqueda).</li>
 *   <li>{@code kabrilla-dark.css}: solo con el tema personalizado activo; sin él se ve el tema base de JavaFX.</li>
 * </ul>
 * Las vistas no llevan estilos en línea: usan clases (kb-*) definidas en esas hojas.
 * Se controla con la propiedad {@code kabrilla.ui.styles-enabled} (ver README).
 */
@Slf4j
public final class ThemeManager {

    static final String BASE_STYLESHEET = "/css/kabrilla-base.css";
    static final String DARK_THEME_STYLESHEET = "/css/kabrilla-dark.css";

    private ThemeManager() {
    }

    /**
     * Llamar una vez en el hilo de JavaFX antes de mostrar la primera ventana.
     */
    public static void install(boolean customThemeEnabled) {
        List<String> stylesheets = stylesheets(customThemeEnabled);
        log.info("Tema {}: {}", customThemeEnabled ? "oscuro de Kabrilla" : "base de JavaFX", stylesheets);
        Window.getWindows().forEach(window -> watch(window, stylesheets));
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(window -> watch(window, stylesheets));
            }
        });
    }

    /**
     * URLs de las hojas a aplicar, en orden (el tema va después para poder redefinir la base).
     */
    static List<String> stylesheets(boolean customThemeEnabled) {
        List<String> urls = new ArrayList<>();
        urls.add(resource(BASE_STYLESHEET));
        if (customThemeEnabled) {
            urls.add(resource(DARK_THEME_STYLESHEET));
        }
        return urls;
    }

    /**
     * Agrega las hojas que falten a la escena (idempotente).
     */
    static void apply(Scene scene, List<String> stylesheets) {
        for (String url : stylesheets) {
            if (!scene.getStylesheets().contains(url)) {
                scene.getStylesheets().add(url);
            }
        }
    }

    private static void watch(Window window, List<String> stylesheets) {
        if (window.getScene() != null) {
            apply(window.getScene(), stylesheets);
        }
        window.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                apply(scene, stylesheets);
            }
        });
    }

    private static String resource(String path) {
        URL url = Objects.requireNonNull(ThemeManager.class.getResource(path), "No existe la hoja de estilos " + path);
        return url.toExternalForm();
    }
}
