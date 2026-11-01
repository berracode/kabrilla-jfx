package co.com.bancolombia.kabrilla.startup;


import java.io.IOException;
import java.util.Objects;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    private static final double MIN_WIDTH = 1280.0;
    private static final double MIN_HEIGHT = 720.0;

    private final ApplicationContext context;

    // Inyectamos el contexto de Spring
    public PrimaryStageInitializer(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            Stage stage = event.getStage();

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            fxmlLoader.setControllerFactory(context::getBean);

            Parent parent = fxmlLoader.load();
            Scene scene = new Scene(parent, MIN_WIDTH, MIN_HEIGHT);
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());

            stage.setMinWidth(MIN_WIDTH);
            stage.setMinHeight(MIN_HEIGHT);
            stage.setTitle("Kabrilla kubernetes Dashboard");
            stage.setScene(scene);
            stage.getIcons().addAll(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/kabrilla-icon-16.png"))),
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/kabrilla-icon-32.png"))),
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/kabrilla-icon-64.png"))),
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/kabrilla-icon-128.png")))
            );
            stage.show();

        } catch (IOException e) {
            throw new RuntimeException("Error al inicializar la Stage principal de JavaFX", e);
        }
    }
}
