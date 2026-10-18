package co.com.bancolombia.kabrilla.startup;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "co.com.bancolombia.kabrilla")
public class MainApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}

