package co.com.bancolombia.kabrilla.controller.home;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import co.com.bancolombia.kabrilla.Namespace;
import co.com.bancolombia.kabrilla.services.KubeService;
import co.com.bancolombia.kabrilla.services.NamespaceService;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class HomeController implements Initializable {

    @FXML
    private TextField txtNameSpace;

    @FXML
    private TextField txtKubePath;

    @FXML
    private Button btnAddNameSpace;
    private final NamespaceService namespaceService = NamespaceService.getInstance();

    private final KubeService kubeService = new KubeService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        txtKubePath.setText(kubeService.loadKubePath());

        btnAddNameSpace.setDisable(true);

        txtNameSpace.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 4) {
                btnAddNameSpace.setDisable(false);
            } else {
                btnAddNameSpace.setDisable(true);
            }
        });


    }

    @FXML
    private void onAddKubePathClick() {
        String kubePath = txtKubePath.getText();
        kubeService.saveKubePath(kubePath);
    }

    @FXML
    protected void onAddNameSpaceClick() {
        String newNamespaceString = txtNameSpace.getText();
        if (!newNamespaceString.isEmpty()) {
            var newNamespace = new Namespace(newNamespaceString, new ArrayList<>());
            System.out.println("1. inica en el hilo: "+Thread.currentThread().threadId());
            namespaceService.saveNamespace(newNamespace);
        }
    }


}