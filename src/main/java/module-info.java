module co.com.bancolombia.kabrilla {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.databind;
    requires org.yaml.snakeyaml;
    requires io.kubernetes.client.java;
    requires org.slf4j;

    // Paquete raíz (si tienes clases principales)
    exports co.com.bancolombia.kabrilla;
    opens co.com.bancolombia.kabrilla to javafx.fxml, com.fasterxml.jackson.databind;

    // Controladores
    exports co.com.bancolombia.kabrilla.ui.controller; // opcional si otros módulos lo usan
    opens co.com.bancolombia.kabrilla.ui.controller to javafx.fxml;

    exports co.com.bancolombia.kabrilla.ui.viewmodel.state.base;
    exports co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace;


    // hexa

    // --- Dominio ---
    exports co.com.bancolombia.kabrilla.domain.model;
    opens co.com.bancolombia.kabrilla.domain.model to com.fasterxml.jackson.databind;

    exports co.com.bancolombia.kabrilla.domain.gateway;
    opens co.com.bancolombia.kabrilla.domain.gateway to com.fasterxml.jackson.databind;

    // --- Aplicación ---
    exports co.com.bancolombia.kabrilla.application.service;
    opens co.com.bancolombia.kabrilla.application.service to com.fasterxml.jackson.databind;

    // --- Infraestructura ---
    exports co.com.bancolombia.kabrilla.infrastructure.adapters.fs;
    opens co.com.bancolombia.kabrilla.infrastructure.adapters.fs to com.fasterxml.jackson.databind;

    exports co.com.bancolombia.kabrilla.ui.controller.sidebar; // opcional
    opens co.com.bancolombia.kabrilla.ui.controller.sidebar to javafx.fxml;

}