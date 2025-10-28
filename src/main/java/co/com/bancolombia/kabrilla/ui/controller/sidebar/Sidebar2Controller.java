package co.com.bancolombia.kabrilla.ui.controller.sidebar;

import co.com.bancolombia.kabrilla.ui.controller.ContentController;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class Sidebar2Controller {

    @FXML
    private TreeView<String> treeView;

    @FXML
    public void initialize() {
        // Crear raíz invisible (para permitir múltiples clusters)
        TreeItem<String> root = new TreeItem<>();
        root.setExpanded(true);

        // Cluster DEV
        TreeItem<String> clusterDev = new TreeItem<>("cluster-name-dev");
        TreeItem<String> nsDev1 = new TreeItem<>("namespace-xyz-1-dev");
        TreeItem<String> nsDev2 = new TreeItem<>("namespace-xyz-2-dev");

        nsDev1.getChildren().addAll(
                new TreeItem<>("my-app-1-deployment"),
                new TreeItem<>("my-app-2-deployment")
        );

        nsDev2.getChildren().addAll(
                new TreeItem<>("my-app-3-deployment"),
                new TreeItem<>("my-app-4-deployment")
        );

        clusterDev.getChildren().addAll(nsDev1, nsDev2);

        // Cluster QA
        TreeItem<String> clusterQA = new TreeItem<>("cluster-name-qa");
        TreeItem<String> nsQA = new TreeItem<>("namespace-xyz-qa");
        nsQA.getChildren().add(new TreeItem<>("my-app-qa-deployment"));
        clusterQA.getChildren().add(nsQA);

        // Agregar clusters al root
        root.getChildren().addAll(clusterDev, clusterQA);

        treeView.setRoot(root);
        treeView.setShowRoot(false); // Oculta el root vacío

        // Evento: abrir tab solo si es deployment (hoja)
        treeView.setOnMouseClicked(event -> {
            TreeItem<String> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isLeaf()) {
                ContentController.addTab(selected.getValue());
            }
        });
    }


}
