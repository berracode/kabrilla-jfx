package co.com.bancolombia.kabrilla.ui.theme;

import static co.com.bancolombia.kabrilla.ui.FxTestSupport.onFx;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.com.bancolombia.kabrilla.ui.FxTestSupport;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

class StyleToggleTest {

    @Test
    void stripsStylesheetsAndInlineStylesIncludingLaterNodes() throws Exception {
        FxTestSupport.startToolkit();

        boolean ok = onFx(() -> {
            StyleToggle.disableCustomStyles();

            Label label = new Label("hola");
            label.setStyle("-fx-text-fill: white;");
            VBox root = new VBox(label);
            root.getStylesheets().add("css/demo.css");
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 100, 100));
            stage.show();

            // Contenido agregado después de mostrar la ventana (como una pestaña nueva)
            Label later = new Label("después");
            later.setStyle("-fx-background-color: red;");
            root.getChildren().add(later);
            // setStyle en un nodo ya visible (como los Alert de la app)
            label.setStyle("-fx-font-size: 30;");

            boolean result = root.getStylesheets().isEmpty()
                    && label.getStyle().isEmpty()
                    && later.getStyle().isEmpty();
            stage.close();
            return result;
        });

        assertTrue(ok, "Con estilos desactivados no deben quedar hojas CSS ni estilos en línea");
    }
}
