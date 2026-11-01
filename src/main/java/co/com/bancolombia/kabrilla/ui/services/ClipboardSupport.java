package co.com.bancolombia.kabrilla.ui.services;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public final class ClipboardSupport {

    private ClipboardSupport() {
    }

    /**
     * Copia texto al portapapeles del sistema. Llamar en el hilo de JavaFX.
     */
    public static void copy(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text == null ? "" : text);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
