package co.com.bancolombia.kabrilla.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.infrastructure.persistence.FileClusterRepository;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;


public class MainController {

    @FXML
    private TreeView<String> treeView;

    @FXML
    private TabPane deploymentTabPane;

    @FXML
    private SplitPane mainSplitPane;

    @FXML
    private Button createNamespaceButton;
    private ClusterService clusterService;

    @FXML
    public void initialize() {

        clusterService = new ClusterService(new FileClusterRepository());

        loadClustersIntoTreeView();

        treeView.setOnMouseClicked(event -> {
            TreeItem<String> selected = treeView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isLeaf() && selected.getValue().startsWith("deploy")) {
                addDeploymentTab(selected.getValue());
            }
        });

        mainSplitPane.setDividerPosition(0, 250.0 / mainSplitPane.getWidth());

        mainSplitPane.widthProperty().addListener((obs, oldW, newW) -> {
            mainSplitPane.setDividerPosition(0, 250.0 / newW.doubleValue());
        });

        createNamespaceButton.setOnAction(e -> openNamespaceDialog());

    }

    private void loadClustersIntoTreeView() {
        TreeItem<String> root = new TreeItem<>();
        root.setExpanded(true);

        List<Cluster> clusters = clusterService.getAllClusters();
        if (clusters.isEmpty()) {
            root.getChildren().add(new TreeItem<>("⚠️ No hay clusters registrados"));
        } else {
            for (Cluster cluster : clusters) {
                //TreeItem<String> clusterItem = new TreeItem<>(cluster.getName());
                ImageView icon = new ImageView(getClass().getResource("/icons/folder-ns.png").toExternalForm());
                icon.setFitWidth(12);
                icon.setFitHeight(12);
                icon.setPreserveRatio(true);
                TreeItem<String> clusterItem = new TreeItem<>(cluster.getName(), icon);

                for (Namespace ns : cluster.getNamespaces()) {
                    TreeItem<String> nsItem = new TreeItem<>(ns.getName());

                    // Si más adelante agregas deployments, podrías cargarlos aquí
                    //nsItem.getChildren().add(new TreeItem<>("(sin deployments)"));
                    // ⬇️ Listener que se activa cuando se expande el namespace
                    nsItem.getChildren().add(new TreeItem<>("")); // placeholder

                    nsItem.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                        if (isNowExpanded) {
                            // Solo cargar si aún tiene el placeholder
                            if (nsItem.getChildren().size() == 1 &&
                                    nsItem.getChildren().getFirst().getValue().isEmpty()) {
                                loadDeploymentsAsync(nsItem);
                            }
                        }
                    });

                    clusterItem.getChildren().add(nsItem);
                }
                clusterItem.setExpanded(true);
                root.getChildren().add(clusterItem);
            }
        }

        treeView.setRoot(root);
        treeView.setShowRoot(false);
    }

    private void loadDeploymentsAsync(TreeItem<String> nsItem) {
        // Mostrar mensaje temporal mientras "carga"
        TreeItem<String> loadingItem = new TreeItem<>("🔄 Cargando deployments...");
        nsItem.getChildren().setAll(loadingItem);

        // Simular trabajo en segundo plano
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                // Simula una llamada lenta a la API (2 segundos)
                Thread.sleep(2000);

                // Aquí podrías hacer una llamada real con tu FileClusterRepository o K8s API
                // Ejemplo: return k8sService.getDeployments(nsItem.getValue());
                if (Math.random() > 0.5) { // Simula que a veces hay resultados
                    return List.of(
                            "deploy-" + nsItem.getValue() + "chikamaru-bien-largo-porqueavacespasa-backend",
                            "deploy-" + nsItem.getValue() + "chikamaru-bien-largo-porqueavacespasa-frontend",
                            "deploy-" + nsItem.getValue() + "-worker"
                    );
                } else {
                    return List.of(); // sin deployments
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<String> deployments = task.getValue();
            nsItem.getChildren().clear();

            if (deployments.isEmpty()) {
                nsItem.getChildren().add(new TreeItem<>("⚠️ No hay deployments"));
            } else {
                for (String dep : deployments) {
                    ImageView depIcon = new ImageView(getClass().getResource("/icons/deployment.png").toExternalForm());
                    depIcon.setFitWidth(12);
                    depIcon.setFitHeight(12);
                    depIcon.setPreserveRatio(true);

                    TreeItem<String> depItem = new TreeItem<>(dep, depIcon);
                    nsItem.getChildren().add(depItem);
                }
            }
        });

        task.setOnFailed(e -> {
            nsItem.getChildren().setAll(new TreeItem<>("❌ Error al cargar deployments"));
            task.getException().printStackTrace();
        });

        // Ejecutar la tarea en un hilo separado
        new Thread(task).start();
    }


    private void openNamespaceDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-namespace-dialog.fxml"));
            DialogPane dialogPane = loader.load();
            CreateNamespaceController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                controller.saveCluster();
                loadClustersIntoTreeView(); // ✅ refresca el árbol

            }


        } catch (IOException e) {
            e.printStackTrace();
        }
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
