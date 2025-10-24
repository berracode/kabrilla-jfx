package co.com.bancolombia.kabrilla.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

public class ContentController {

    private static ContentController instance;
    @FXML
    private TabPane tabPane;

    public static void addTab(String title) {
        if (instance == null) {
            return;
        }

        var tabList = instance.tabPane.getTabs();
        var tabOptional = tabList.stream().filter(t -> t.getText().equals(title)).findFirst();
        if (tabOptional.isPresent()) {
            instance.tabPane.getSelectionModel().select(tabOptional.get());
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(ContentController.class.getResource("/fxml/tab-content.fxml"));
            VBox tabContent = loader.load();
            TabContentController controller = loader.getController();
            controller.setDeploymentName(title);

            Tab tab = new Tab(title, tabContent);
            instance.tabPane.getTabs().add(tab);
            instance.tabPane.getSelectionModel().select(tab);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        instance = this;
    }
}
