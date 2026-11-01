package co.com.bancolombia.kabrilla.ui.theme;

import static co.com.bancolombia.kabrilla.ui.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.List;

import co.com.bancolombia.kabrilla.ui.FxTestSupport;

import javafx.css.CssParser;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

class ThemeManagerTest {

    @Test
    void customThemeToggleChoosesStylesheets() {
        List<String> base = ThemeManager.stylesheets(false);
        List<String> themed = ThemeManager.stylesheets(true);

        assertEquals(1, base.size());
        assertTrue(base.getFirst().endsWith("kabrilla-base.css"));
        // El tema va después de la base para poder redefinirla
        assertEquals(2, themed.size());
        assertTrue(themed.get(1).endsWith("kabrilla-dark.css"));
    }

    @Test
    void applyIsIdempotent() throws Exception {
        FxTestSupport.startToolkit();
        List<String> sheets = ThemeManager.stylesheets(true);

        int count = onFx(() -> {
            Scene scene = new Scene(new VBox());
            ThemeManager.apply(scene, sheets);
            ThemeManager.apply(scene, sheets);
            return scene.getStylesheets().size();
        });

        assertEquals(2, count);
    }

    @Test
    void stylesheetsHaveNoSyntaxErrors() throws Exception {
        CssParser.errorsProperty().clear();
        for (String url : ThemeManager.stylesheets(true)) {
            new CssParser().parse(new URL(url));
        }
        assertTrue(CssParser.errorsProperty().isEmpty(), "Errores de CSS: " + CssParser.errorsProperty());
    }
}
