module co.com.bancolombia.kabrilla {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.yaml.snakeyaml;
    requires com.fasterxml.jackson.databind;

    // Paquete raíz (si tienes clases principales)
    exports co.com.bancolombia.kabrilla;
    opens co.com.bancolombia.kabrilla to javafx.fxml, com.fasterxml.jackson.databind;

    // Controladores
    exports co.com.bancolombia.kabrilla.controller; // opcional si otros módulos lo usan
    opens co.com.bancolombia.kabrilla.controller to javafx.fxml;

    exports co.com.bancolombia.kabrilla.controller.sidebar; // opcional
    opens co.com.bancolombia.kabrilla.controller.sidebar to javafx.fxml;

    exports co.com.bancolombia.kabrilla.controller.home; // opcional
    opens co.com.bancolombia.kabrilla.controller.home to javafx.fxml;

    // Servicios
    exports co.com.bancolombia.kabrilla.services;
    opens co.com.bancolombia.kabrilla.services to com.fasterxml.jackson.databind;
}