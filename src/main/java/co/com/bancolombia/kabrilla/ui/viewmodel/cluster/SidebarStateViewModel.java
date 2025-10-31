package co.com.bancolombia.kabrilla.ui.viewmodel.cluster;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.ClusterInfo;
import co.com.bancolombia.kabrilla.domain.model.Deployment;
import co.com.bancolombia.kabrilla.ui.controller.usecases.CreateNamespaceController;
import co.com.bancolombia.kabrilla.ui.enums.TabCommand;
import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentItemViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.AppState;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.NamespaceStateViewModel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
public class SidebarStateViewModel {
    private static final Logger log = LoggerFactory.getLogger(SidebarStateViewModel.class);
    private final AppState appState;
    private final ClusterNamespaceStateViewModel clusterNamespaceStateViewModel;
    private final ClusterService clusterService;
    private final ObjectProperty<TreeItem<String>> root = new SimpleObjectProperty<>(new TreeItem<>());

    private final ObjectProperty<TabCommand> openTabRequest = new SimpleObjectProperty<>();

    public ReadOnlyObjectProperty<TabCommand> openTabRequestProperty() {
        return openTabRequest;
    }

    public void openAddCredentialsTab() {
        openTabRequest.set(TabCommand.ADD_CREDENTIALS);
    }

    public void clearOpenTabRequest() {
        openTabRequest.set(null);
    }


    public TreeItem<String> getRoot() {
        return root.get();
    }

    public ObjectProperty<TreeItem<String>> rootProperty() {
        return root;
    }


    public ObservableList<KubeClusterItemViewModel> getKubeConfig() {
        return appState.getKubeConfig();
    }

    public void addKubeConfig(KubeClusterItemViewModel kubeClusterItemViewModel) {
        appState.getKubeConfig().add(kubeClusterItemViewModel);
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
        log.info("2.4: clusterNamespaceState {}", clusterNamespaceStateViewModel.getObservableClusters().size());

        for (ClusterStateViewModel clusterVM : clusterNamespaceStateViewModel.getObservableClusters()) {
            TreeItem<String> clusterItem = createClusterTreeItem(clusterVM.getName());


            for (NamespaceStateViewModel ns : clusterVM.getNamespaces()) {
                log.info("ns {} para cluster: {}", ns.getName(), clusterVM.getName());
                clusterItem.getChildren().add(createNamespaceTreeItem(ns));
            }

            bindNamespaceListener(clusterVM, clusterItem);

            root.getChildren().add(clusterItem);
        }


        clusterNamespaceStateViewModel.getObservableClusters()
                .addListener((ListChangeListener<ClusterStateViewModel>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (ClusterStateViewModel clusterVM : change.getAddedSubList()) {
                                TreeItem<String> clusterItem = createClusterTreeItem(clusterVM.getName());

                                log.info("clusterVM.getNamespaces() size: {}", clusterVM.getNamespaces().size());
                                for (NamespaceStateViewModel ns : clusterVM.getNamespaces()) {
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

    private void bindNamespaceListener(ClusterStateViewModel clusterVM, TreeItem<String> clusterItem) {
        TreeItem<String> emptyItem = new TreeItem<>("No hay namespaces registrados");

        if (clusterVM.getNamespaces().isEmpty()) {
            clusterItem.getChildren().add(emptyItem);
        }

        clusterVM.getNamespaces().addListener((ListChangeListener<NamespaceStateViewModel>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    // Si estaba el mensaje de vacío, lo quitamos
                    clusterItem.getChildren().remove(emptyItem);
                    for (NamespaceStateViewModel ns : change.getAddedSubList()) {
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
        kubeConfigModel.getClusters().forEach(c -> {
            KubeClusterItemViewModel item = new KubeClusterItemViewModel(
                    c.getCluster().getServer(),
                    c.getName(),
                    Base64.getDecoder().decode(c.getCluster().getCertificateAuthorityData())
            );
            addKubeConfig(item);
        });


        List<String> clusterNames = kubeConfigModel.getClusters().stream()
                .map(c -> c.getName().split("/")[1])
                .toList();

        for (String clusterName : clusterNames) {
            Cluster cluster = clustersFromJson.stream()
                    .filter(c -> c.getName().equals(clusterName))
                    .findFirst()
                    .orElse(new Cluster(clusterName));

            clusterNamespaceStateViewModel.getObservableClusters().add(new ClusterStateViewModel(cluster));
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

    private TreeItem<String> createNamespaceTreeItem(NamespaceStateViewModel ns) {
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

        TreeItem<String> clusterItem = nsItem.getParent();
        String clusterName = clusterItem.getValue();
        String namespaceName = nsItem.getValue();

        // Simular trabajo en segundo plano
        /*
         * para traer los deployments necesito claves de aws, clustername y ns
         * tengo el clustername y ns
         * ahora debo obtener desde un state de credenciales, las credenciales donde el clustername sea este
         * y también debo obtener de state el kubeconfig (ca, serverUrl y ya)
         * sería correcto entonces guardar o el kubeconfigmodel en state o simplemente guardar otro objeto a partir
         * del kubeconfig (domain).
         * */
        Task<List<Deployment>> task = new Task<>() {
            @Override
            protected List<Deployment> call() throws Exception {
                Thread.sleep(2000);
                if (Math.random() > 0.5) {
                    return List.of(
                            new Deployment(clusterName, namespaceName, "deploy-product-backend-" + namespaceName),
                            new Deployment(clusterName, namespaceName, "deploy-retreive-frontend-" + namespaceName),
                            new Deployment(clusterName, namespaceName, "deploy-trasmision-worker-" + namespaceName)
                    );
                } else {
                    return List.of(); // sin deployments
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<Deployment> deployments = task.getValue();
            nsItem.getChildren().clear();

            clusterNamespaceStateViewModel.addDeploymentsToNamespace(clusterName, namespaceName, deployments);


            if (deployments.isEmpty()) {
                nsItem.getChildren().add(new TreeItem<>("⚠️ No hay deployments"));
            } else {
                for (Deployment dep : deployments) {
                    ImageView depIcon = new ImageView(getClass().getResource("/icons/deployment.png").toExternalForm());
                    depIcon.setFitWidth(12);
                    depIcon.setFitHeight(12);
                    depIcon.setPreserveRatio(true);

                    TreeItem<String> depItem = new TreeItem<>(dep.getName(), depIcon);
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
        clusterNamespaceStateViewModel.getObservableClusters().forEach(
                clusterStateViewModel -> log.info("clustername: {} - ",
                                                  clusterStateViewModel.getName()));
        return new Deployment(selected.getValue(), selected.getValue(), selected.getValue());
    }

    public Deployment resolveDeployment2(TreeItem<String> selected) {
        TreeItem<String> nsItem = selected.getParent();
        TreeItem<String> clusterItem = nsItem != null ? nsItem.getParent() : null;
        String clusterName = clusterItem != null ? clusterItem.getValue() : null;
        String namespaceName = nsItem != null ? nsItem.getValue() : null;
        String deploymentName = selected.getValue();

        for (ClusterStateViewModel clusterVM : clusterNamespaceStateViewModel.getObservableClusters()) {
            if (clusterVM.getName().equals(clusterName)) {
                for (NamespaceStateViewModel nsVM : clusterVM.getNamespaces()) {
                    if (nsVM.getName().equals(namespaceName)) {
                        for (DeploymentItemViewModel depVM : nsVM.getDeployments()) {
                            if (depVM.getName().equals(deploymentName)) {
                                return depVM.domainDeployment();
                            }
                        }
                    }
                }
            }
        }
        return new Deployment(clusterName, namespaceName, deploymentName);
    }


    public void openNamespaceDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/fxml/usecase/create-namespaces/create-namespace-dialog.fxml"));
            loader.setControllerFactory(param -> new CreateNamespaceController(clusterService,
                                                                               clusterNamespaceStateViewModel));
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
}
