package co.com.bancolombia.kabrilla.ui.viewmodel.terminal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import co.com.bancolombia.kabrilla.ui.terminal.KubeContext;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Estado del panel de terminal (compartido por la ventana principal, el sidebar y el panel): si se ve y en qué
 * contexto de kubectl están las terminales. El contexto se relee cuando cambia el kubeconfig, p. ej. tras
 * {@code kubectl config use-context} en una de ellas.
 */
@Slf4j
@Component
public class TerminalPanelViewModel implements DisposableBean {

    private static final long CHECK_EVERY_SECONDS = 2;

    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final StringProperty context = new SimpleStringProperty("…");
    private final Path kubeconfig;
    private ScheduledExecutorService watcher;
    private FileTime lastModified;

    public TerminalPanelViewModel(@Value("${kabrilla.kubeconfig:}") String configuredKubeconfig) {
        this.kubeconfig = KubeContext.kubeconfigPath(configuredKubeconfig, System.getenv("KUBECONFIG"),
                                                     Path.of(System.getProperty("user.home")));
    }

    public BooleanProperty visibleProperty() {
        return visible;
    }

    public ReadOnlyStringProperty contextProperty() {
        return context;
    }

    public Path kubeconfig() {
        return kubeconfig;
    }

    public void toggle() {
        visible.set(!visible.get());
    }

    public void show() {
        visible.set(true);
    }

    public void hide() {
        visible.set(false);
    }

    /**
     * Empieza a seguir el kubeconfig (al abrir la primera terminal; antes no hace falta leerlo).
     */
    public synchronized void startWatchingContext() {
        if (watcher != null) {
            return;
        }
        watcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kubeconfig-watcher");
            t.setDaemon(true);
            return t;
        });
        watcher.scheduleWithFixedDelay(this::refreshIfChanged, 0, CHECK_EVERY_SECONDS, TimeUnit.SECONDS);
        log.info("Terminal: contexto de kubectl desde {}", kubeconfig);
    }

    private void refreshIfChanged() {
        try {
            FileTime modified = Files.exists(kubeconfig) ? Files.getLastModifiedTime(kubeconfig) : null;
            if (lastModified != null && Objects.equals(modified, lastModified)) {
                return;
            }
            lastModified = modified;
            String text = KubeContext.read(kubeconfig).describe();
            Platform.runLater(() -> context.set(text));
        } catch (Exception e) {
            log.warn("No se pudo leer el contexto de kubectl de {}", kubeconfig, e);
        }
    }

    @Override
    public synchronized void destroy() {
        if (watcher != null) {
            watcher.shutdownNow();
            watcher = null;
        }
    }
}
