package co.com.bancolombia.kabrilla.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;

public class MainController {

    @FXML
    private TreeView<String> treeView;

    @FXML
    private TabPane deploymentTabPane;

    @FXML
    public void initialize() {
        TreeItem<String> root = new TreeItem<>();
        root.setExpanded(true);

        TreeItem<String> clusterDev = new TreeItem<>("cluster-name-dev");
        TreeItem<String> nsDev1 = new TreeItem<>("namespace-xyz-1-dev");
        nsDev1.getChildren().addAll(
                new TreeItem<>("my-app-1-deployment"),
                new TreeItem<>("my-app-2-deployment")
        );
        clusterDev.getChildren().add(nsDev1);

        TreeItem<String> clusterQA = new TreeItem<>("cluster-name-qa");
        TreeItem<String> nsQA = new TreeItem<>("namespace-xyz-qa");
        nsQA.getChildren().add(new TreeItem<>("my-app-qa-deployment"));
        clusterQA.getChildren().add(nsQA);

        root.getChildren().addAll(clusterDev, clusterQA);
        treeView.setRoot(root);
        treeView.setShowRoot(false);

        treeView.setOnMouseClicked(event -> {
            TreeItem<String> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isLeaf()) {
                addDeploymentTab(selected.getValue());
            }
        });
    }

    private void addDeploymentTab(String deploymentName) {
        for (Tab tab : deploymentTabPane.getTabs()) {
            if (tab.getText().equals(deploymentName)) {
                deploymentTabPane.getSelectionModel().select(tab);
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tab-content.fxml"));
            AnchorPane content = loader.load();

            //VBox vbox = loader.load();

            TabContentController controller = loader.getController();
            controller.setDeploymentName(deploymentName);

            Tab tab = new Tab(deploymentName, content);
            deploymentTabPane.getTabs().add(tab);
            deploymentTabPane.getSelectionModel().select(tab);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
