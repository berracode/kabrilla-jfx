package co.com.bancolombia.kabrilla.ui.viewmodel.cluster;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.ClusterInfo;
import co.com.bancolombia.kabrilla.domain.model.Deployment;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.ui.controller.usecases.CreateNamespaceController;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceState;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterViewModel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class SidebarViewModel {
    private static final Logger log = LoggerFactory.getLogger(SidebarViewModel.class);

    private final ClusterNamespaceState clusterNamespaceState;
    private final ClusterService clusterService;
    private final ObjectProperty<TreeItem<String>> root = new SimpleObjectProperty<>(new TreeItem<>());

    public TreeItem<String> getRoot() {
        return root.get();
    }

    public ObjectProperty<TreeItem<String>> rootProperty() {
        return root;
    }


    public void loadClustersAsync(TreeItem<String> rootItem) {
        log.info("Aqui 1.1");
        rootItem.setExpanded(true);


        Task<ClusterInfo> task = new Task<>() {
            @Override
            protected ClusterInfo call() throws Exception {
                return clusterService.getClusterInfo();
            }
        };

        task.setOnSucceeded(event -> {
            ClusterInfo clusterInfo = task.getValue();
            populateTreeWithClusters(rootItem, clusterInfo);
        });

        task.setOnFailed(event -> {
            rootItem.getChildren().add(new TreeItem<>("❌ Error al cargar clusters"));
            task.getException().printStackTrace();
        });

        log.info("Aqui 1.2");

        new Thread(task).start();
        log.info("Aqui 1.3");

    }

    public void bindTreeViewToState(TreeItem<String> root) {
        root.setExpanded(true);
        log.info("Aqui 2.1");

        log.info("Aqui 2.3");
        root.getChildren().add(new TreeItem<>("Cargando clusters..."));
        log.info("2.4: clusterNamespaceState {}", clusterNamespaceState.getObservableClusters().size());

        for (ClusterViewModel clusterVM : clusterNamespaceState.getObservableClusters()) {
            TreeItem<String> clusterItem = createClusterTreeItem(clusterVM.getName());


            for (Namespace ns : clusterVM.getNamespaces()) {
                log.info("ns {} para cluster: {}", ns.getName(), clusterVM.getName());
                clusterItem.getChildren().add(createNamespaceTreeItem(ns));
            }

            bindNamespaceListener(clusterVM, clusterItem);

            root.getChildren().add(clusterItem);
        }


        clusterNamespaceState.getObservableClusters().addListener((ListChangeListener<ClusterViewModel>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ClusterViewModel clusterVM : change.getAddedSubList()) {
                        TreeItem<String> clusterItem = createClusterTreeItem(clusterVM.getName());

                        log.info("clusterVM.getNamespaces() size: {}", clusterVM.getNamespaces().size());
                        for (Namespace ns : clusterVM.getNamespaces()) {
                            log.info("ns {} para cluster: {}", ns.getName(), clusterVM.getName());
                            clusterItem.getChildren().add(createNamespaceTreeItem(ns));
                        }
                        bindNamespaceListener(clusterVM, clusterItem);

                        root.getChildren().add(clusterItem);
                    }
                }
            }
        });
    }

    private void bindNamespaceListener(ClusterViewModel clusterVM, TreeItem<String> clusterItem) {
        TreeItem<String> emptyItem = new TreeItem<>("No hay namespaces registrados");

        if (clusterVM.getNamespaces().isEmpty()) {
            clusterItem.getChildren().add(emptyItem);
        }

        clusterVM.getNamespaces().addListener((ListChangeListener<Namespace>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    // Si estaba el mensaje de vacío, lo quitamos
                    clusterItem.getChildren().remove(emptyItem);
                    for (Namespace ns : change.getAddedSubList()) {
                        clusterItem.getChildren().add(createNamespaceTreeItem(ns));
                    }
                }
                if (change.wasRemoved()) {
                    // Si ya no hay namespaces, mostramos el mensaje
                    if (clusterVM.getNamespaces().isEmpty()) {
                        clusterItem.getChildren().clear();
                        clusterItem.getChildren().add(emptyItem);
                    }
                }
            }
        });
    }

    private void populateTreeWithClusters(TreeItem<String> root, ClusterInfo clusterInfo) {
        var kubeConfigModel = clusterInfo.getKubeConfigModel();
        var clustersFromJson = clusterInfo.getClustersWithNamespace();
        log.info("Aqui 1.4");

        var firstTreeItem = root.getChildren().getFirst();
        if (firstTreeItem.getValue().equals("Cargando clusters...")) {
            root.getChildren().clear();
            log.info("Aqui 1.5");

        }

        List<String> clusterNames = kubeConfigModel.getClusters().stream()
                .map(c -> c.getName().split("/")[1])
                .toList();

        for (String clusterName : clusterNames) {
            Cluster cluster = clustersFromJson.stream()
                    .filter(c -> c.getName().equals(clusterName))
                    .findFirst()
                    .orElse(new Cluster(clusterName));

            clusterNamespaceState.getObservableClusters().add(new ClusterViewModel(cluster));
        }

    }

    private TreeItem<String> createClusterTreeItem(String clusterName) {
        ImageView icon =
                new ImageView(Objects.requireNonNull(getClass().getResource("/icons/folder-ns.png")).toExternalForm());
        icon.setFitWidth(12);
        icon.setFitHeight(12);
        icon.setPreserveRatio(true);
        return new TreeItem<>(clusterName, icon);
    }

    private TreeItem<String> createNamespaceTreeItem(Namespace ns) {
        TreeItem<String> nsItem = new TreeItem<>(ns.getName());
        nsItem.getChildren().add(new TreeItem<>("")); // placeholder

        nsItem.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
            if (isNowExpanded && nsItem.getChildren().size() == 1 &&
                    nsItem.getChildren().getFirst().getValue().isEmpty()) {
                loadDeploymentsAsync(nsItem);
            }
        });

        return nsItem;
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
                            "deploy-" + nsItem.getValue() + "product-backend",
                            "deploy-" + nsItem.getValue() + "retreive-frontend",
                            "deploy-" + nsItem.getValue() + "trasmision-worker"
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

    public boolean isDeploymentItem(TreeItem<String> selected) {
        return selected.getValue().startsWith("deploy");
    }

    public Deployment resolveDeployment(TreeItem<String> selected) {
        log.info("El tree item selected: {}", selected.getValue());
        clusterNamespaceState.getObservableClusters().forEach(clusterViewModel -> log.info("clustername: {} - ",
                                                                                           clusterViewModel.getName()));
        return new Deployment(selected.getValue(), selected.getValue(), selected.getValue());
    }

    public void openNamespaceDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/fxml/usecase/create-namespaces/create-namespace-dialog.fxml"));
            loader.setControllerFactory(param -> new CreateNamespaceController(clusterService, clusterNamespaceState));
            DialogPane dialogPane = loader.load();
            CreateNamespaceController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();

            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Crear Namespace");

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    String clusterName = controller.getClusterName();
                    String namespace = controller.getNamespace();
                    if (clusterName == null || clusterName.isBlank() || namespace.isBlank()) {
                        // Mostrar alerta o mensaje de error
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Validación");
                        alert.setHeaderText(null);
                        alert.setContentText("Debes seleccionar un cluster y escribir un namespace.");

                        DialogPane alertPane = alert.getDialogPane();
                        alertPane.getStylesheets()
                                .add(getClass().getResource("/css/demo.css").toExternalForm());
                        alertPane.setStyle("-fx-background-color: #2B2B2B;");
                        alertPane.lookup(".content.label").setStyle("-fx-text-fill: white;");

                        alert.showAndWait();
                        return null;
                    }

                    controller.saveCluster();
                    return ButtonType.OK;
                }
                return null;
            });

            dialog.showAndWait();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openAddCredentialsTab() {
      /*  try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/fxml/usecase/add-credentials/add-credentials.fxml"));
            loader.setControllerFactory(
                    param -> new AddCredentialsController(clusterNamespaceState, credentialService));
            Parent content = loader.load();

            Tab tab = new Tab("Credenciales AWS");
            tab.setContent(content);
            tab.setClosable(true);
            deploymentTabPane.getTabs().add(tab);
            deploymentTabPane.getSelectionModel().select(tab);
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/
    }
}
