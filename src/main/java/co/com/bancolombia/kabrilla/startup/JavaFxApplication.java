package co.com.bancolombia.kabrilla.startup;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;
    // Error al iniciar Spring: sin consola visible, se muestra en un diálogo en vez de cerrar en silencio
    private Exception startupError;

    @Override
    public void init() {
        // Errores no controlados (incluido el hilo de JavaFX) van al log; sin esto solo salían por consola
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> log.error("Error no controlado en el hilo {}", thread.getName(), error));
        try {
            this.context = new SpringApplicationBuilder()
                    .sources(MainApplication.class)
                    .run(getParameters().getRaw().toArray(new String[0]));
        } catch (Exception e) {
            log.error("No se pudo iniciar Kabrilla", e);
            startupError = e;
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        if (startupError != null) {
            showStartupErrorAndExit();
            return;
        }
        // Notificamos a Spring que la Stage está lista
        context.publishEvent(new StageReadyEvent(stage));
    }

    @Override
    public void stop() {
        if (this.context != null) {
            this.context.close();
        }
        Platform.exit();
    }

    private void showStartupErrorAndExit() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Kabrilla");
        alert.setHeaderText("No se pudo iniciar Kabrilla");
        alert.setContentText(rootMessage(startupError)
                                     + "\n\nDetalle en: %USERPROFILE%\\.kabrilla\\logs\\kabrilla.log");
        alert.showAndWait();
        Platform.exit();
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}
