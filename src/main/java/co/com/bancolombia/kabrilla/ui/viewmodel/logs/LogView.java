package co.com.bancolombia.kabrilla.ui.viewmodel.logs;

import java.util.List;

/**
 * Lo que el {@link LogsViewModel} necesita de la vista de logs. Se invoca siempre en el hilo de JavaFX.
 */
public interface LogView {

    /**
     * Agrega líneas al final (stream en curso).
     */
    void append(List<String> lines);

    /**
     * Reemplaza todo lo mostrado (cambió el filtro o se limpió).
     */
    void reset(List<String> lines);
}
