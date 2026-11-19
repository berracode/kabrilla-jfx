package co.com.bancolombia.kabrilla.ui.terminal;

import com.techsenger.jeditermfx.core.TerminalColor;
import com.techsenger.jeditermfx.core.TextStyle;
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider;
import javafx.scene.paint.Color;

/**
 * Apariencia de la terminal. El emulador dibuja en un Canvas (no usa CSS), así que los colores se leen del tema
 * activo (clases {@code kb-terminal-colors} y {@code kb-terminal-selection}) y se le pasan aquí.
 */
public class KabrillaTerminalSettings extends DefaultSettingsProvider {

    private static final float FONT_SIZE = 13f;

    private final TerminalColor foreground;
    private final TerminalColor background;
    private final TextStyle selection;

    public KabrillaTerminalSettings(Color foreground, Color background, Color selectionForeground,
                                    Color selectionBackground) {
        this.foreground = toTerminal(foreground);
        this.background = toTerminal(background);
        this.selection = new TextStyle(toTerminal(selectionForeground), toTerminal(selectionBackground));
    }

    @Override
    public TerminalColor getDefaultForeground() {
        return foreground;
    }

    @Override
    public TerminalColor getDefaultBackground() {
        return background;
    }

    @Override
    public TextStyle getSelectionColor() {
        return selection;
    }

    @Override
    public float getTerminalFontSize() {
        return FONT_SIZE;
    }

    private static TerminalColor toTerminal(Color color) {
        return TerminalColor.rgb((int) Math.round(color.getRed() * 255), (int) Math.round(color.getGreen() * 255),
                                 (int) Math.round(color.getBlue() * 255));
    }
}
