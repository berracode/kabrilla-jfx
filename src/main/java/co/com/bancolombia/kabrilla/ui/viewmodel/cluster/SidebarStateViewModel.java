package co.com.bancolombia.kabrilla.ui.viewmodel.cluster;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.application.service.DeploymentService;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.ClusterInfo;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SidebarStateViewModel {
    private final AppState appState;
    private final ClusterNamespaceStateViewModel clusterNamespaceStateViewModel;
    private final ClusterService clusterService;
    private final DeploymentService deploymentService;
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
            removeLoadingPlaceholder(rootItem);
            Throwable cause = task.getException();
            log.error("No se pudieron cargar los clusters", cause);
            rootItem.getChildren().add(new TreeItem<>("No se pudo leer ~/.kube/config: " + cause.getMessage()));
        });


        new Thread(task).start();

    }

    public void bindTreeViewToState(TreeItem<String> root) {
        root.setExpanded(true);

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

        removeLoadingPlaceholder(root);

        // Solo clusters EKS (la app se autentica con "aws eks get-token"). Un cluster local (docker-desktop, minikube)
        // o una entrada inválida se omite con un aviso, sin impedir que se carguen los demás.
        for (var c : kubeConfigModel.getClusters()) {
            String arn = c.getName();
            if (arn == null || !arn.contains(":cluster/")) {
                log.info("Se omite el cluster '{}' del kubeconfig: no es un cluster EKS", arn);
                continue;
            }
            byte[] caCert;
            try {
                caCert = Base64.getDecoder().decode(c.getCluster().getCertificateAuthorityData());
            } catch (RuntimeException e) {
                log.warn("Se omite el cluster '{}': certificate-authority-data ausente o inválido", arn);
                continue;
            }
            addKubeConfig(new KubeClusterItemViewModel(c.getCluster().getServer(), arn, caCert));

            String clusterName = arn.substring(arn.lastIndexOf('/') + 1);
            Cluster cluster = clustersFromJson.stream()
                    .filter(existing -> existing.getName().equals(clusterName))
                    .findFirst()
                    .orElse(new Cluster(clusterName));
            clusterNamespaceStateViewModel.getObservableClusters().add(new ClusterStateViewModel(cluster));
        }

        if (clusterNamespaceStateViewModel.getObservableClusters().isEmpty()) {
            root.getChildren().add(new TreeItem<>("No hay clusters EKS en ~/.kube/config"));
        }
    }

    private static void removeLoadingPlaceholder(TreeItem<String> root) {
        root.getChildren().removeIf(item -> "Cargando clusters...".equals(item.getValue()));
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
            log.info("Lo expandieron ns: {}", ns.getName());
            if (isNowExpanded && nsItem.getChildren().size() == 1 &&
                    nsItem.getChildren().getFirst().getValue().isEmpty() || (nsItem.getChildren().getFirst().getValue()
                    .contains("Error"))) {
                log.info("Lo expandieron ns y este es el if para cargar deployments: {}",
                         nsItem.getChildren().getFirst().getValue());
                loadDeploymentsAsync(nsItem);
            }
        });

        return nsItem;
    }

    private void loadDeploymentsAsync(TreeItem<String> nsItem) {
        TreeItem<String> loadingItem = new TreeItem<>("🔄 Cargando deployments...");
        nsItem.getChildren().setAll(loadingItem);

        TreeItem<String> clusterItem = nsItem.getParent();
        String clusterName = clusterItem.getValue();
        String namespaceName = nsItem.getValue();

        KubeConfig kubeConfig = appState.getKubeConfig().stream()
                .filter(c -> c.clusterName().contains(clusterName))
                .map(c -> KubeConfig.builder()
                        .clusterName(c.clusterName())
                        .serverUrl(c.serverUrl())
                        .caCertBytes(c.caCertBytes())
                        .build())
                .findFirst()
                .orElse(null);


        var credentials = appState.getCredentials().stream()
                .filter(c -> c.clusterName().equals(clusterName))
                .map(c -> AwsCredentials.builder().accessKey(c.accessKey()).secretKey(c.secretKey())
                        .sessionToken(c.sessionToken()).clusterName(clusterName).build()).findFirst().orElse(null);


        Task<List<Deployment>> task = new Task<>() {
            @Override
            protected List<Deployment> call() throws Exception {
                return deploymentService.getDeployments(kubeConfig, credentials, namespaceName);
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
            nsItem.getChildren().setAll(new TreeItem<>("Error al cargar deployments"));
            task.getException().printStackTrace();
        });

        // Ejecutar la tarea en un hilo separado
        new Thread(task).start();
    }

    //private void loadPodsByNamespace

    /**
     * Un nodo es deployment si cuelga de cluster/namespace y existe en el estado cargado desde el cluster
     * (así no se confunde con placeholders como "Cargando..." ni depende del nombre del deployment).
     */
    public boolean isDeploymentItem(TreeItem<String> selected) {
        TreeItem<String> nsItem = selected.getParent();
        TreeItem<String> clusterItem = nsItem != null ? nsItem.getParent() : null;
        if (clusterItem == null) {
            return false;
        }
        return clusterNamespaceStateViewModel.getObservableClusters().stream()
                .filter(c -> c.getName().equals(clusterItem.getValue()))
                .flatMap(c -> c.getNamespaces().stream())
                .filter(ns -> ns.getName().equals(nsItem.getValue()))
                .flatMap(ns -> ns.getDeployments().stream())
                .anyMatch(d -> d.getName().equals(selected.getValue()));
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
