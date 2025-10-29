package co.com.bancolombia.kabrilla.ui.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.application.service.CredentialService;
import co.com.bancolombia.kabrilla.bootstrap.AppBootstrap;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.ClusterInfo;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.ui.controller.tabs.TabContentController;
import co.com.bancolombia.kabrilla.ui.controller.usecases.AddCredentialsController;
import co.com.bancolombia.kabrilla.ui.controller.usecases.CreateNamespaceController;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceState;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterViewModel;

import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML
    private TreeView<String> treeView;
    @FXML
    private TabPane deploymentTabPane;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private Button createNamespaceButton;
    @FXML
    private Button addCredentialsButton;

    private ClusterService clusterService;
    private ClusterNamespaceState clusterNamespaceState;

    private CredentialService credentialService;


    @FXML
    public void initialize() {
        clusterService = AppBootstrap.getInstance().getClusterService();
        credentialService = AppBootstrap.getInstance().getCredentialService();
        clusterNamespaceState = AppBootstrap.getInstance().getClusterNamespaceState();
        log.info("Aqui 1");
        TreeItem<String> root = new TreeItem<>();
        loadClustersIntoTreeView3(root);
        log.info("Aqui 2");
        bindTreeViewToState(root); // 👈 Aquí conectas el estado con la vista
        log.info("Aqui 3");


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

        addCredentialsButton.setOnAction(e -> openAddCredentialsTab());


    }

    private void bindTreeViewToState(TreeItem<String> root) {
        root.setExpanded(true);
        log.info("Aqui 2.1");

        treeView.setRoot(root);
        log.info("Aqui 2.2");

        treeView.setShowRoot(false);
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

    private void loadClustersIntoTreeView3(TreeItem<String> root) {
        log.info("Aqui 1.1");

        Task<ClusterInfo> task = new Task<>() {
            @Override
            protected ClusterInfo call() throws Exception {
                return clusterService.getClusterInfo();
            }
        };

        task.setOnSucceeded(event -> {
            ClusterInfo clusterInfo = task.getValue();
            populateTreeWithClusters(root, clusterInfo);
        });

        task.setOnFailed(event -> {
            root.getChildren().add(new TreeItem<>("❌ Error al cargar clusters"));
            task.getException().printStackTrace();
        });

        log.info("Aqui 1.2");

        new Thread(task).start();
        log.info("Aqui 1.3");

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

    private void addDeploymentTab(String deploymentName) {
        for (Tab tab : deploymentTabPane.getTabs()) {
            if (tab.getText().equals(deploymentName)) {
                deploymentTabPane.getSelectionModel().select(tab);
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tabs/tab-content.fxml"));
            AnchorPane content = loader.load();

            //VBox vbox = loader.load();

            TabContentController controller = loader.getController();
            loader.setControllerFactory(param -> new CreateNamespaceController(clusterService, clusterNamespaceState));
            controller.setDeploymentName(deploymentName);

            Tab tab = new Tab(deploymentName, content);
            deploymentTabPane.getTabs().add(tab);
            deploymentTabPane.getSelectionModel().select(tab);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void openAddCredentialsTab() {
        try {
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
        }
    }
}
