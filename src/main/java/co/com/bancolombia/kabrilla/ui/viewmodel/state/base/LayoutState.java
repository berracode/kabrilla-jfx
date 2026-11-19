package co.com.bancolombia.kabrilla.ui.viewmodel.state.base;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * Qué partes de la ventana se ven (menú Ver). El panel de terminal tiene su propio estado en
 * {@code TerminalPanelViewModel}.
 */
@Component
public class LayoutState {

    private final BooleanProperty sidebarVisible = new SimpleBooleanProperty(true);

    public BooleanProperty sidebarVisibleProperty() {
        return sidebarVisible;
    }
}
