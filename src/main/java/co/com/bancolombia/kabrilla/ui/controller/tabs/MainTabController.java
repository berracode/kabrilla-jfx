package co.com.bancolombia.kabrilla.ui.controller.tabs;

import java.io.IOException;
import java.util.List;

import co.com.bancolombia.kabrilla.application.service.CredentialService;
import co.com.bancolombia.kabrilla.application.service.KubeResourceService;
import co.com.bancolombia.kabrilla.application.service.LogsService;
import co.com.bancolombia.kabrilla.application.service.PodService;
import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;
import co.com.bancolombia.kabrilla.ui.controller.usecases.AddCredentialsController;
import co.com.bancolombia.kabrilla.ui.viewmodel.cluster.SidebarStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.credentials.CredentialsStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.AppContext;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.tabs.DeploymentTabItemViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.tabs.MainTabViewModel;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MainTabController {
    private final MainTabViewModel mainTabViewModel;
    private final SidebarStateViewModel sidebarViewModel;
    private final AppContext context;
    private final ClusterNamespaceStateViewModel clusterNamespaceStateViewModel;
    private final CredentialService credentialService;
    private final PodService podService;
    private final LogsService logsService;
    private final KubeResourceService kubeResourceService;

    @FXML
    private TabPane deploymentTabPane;

    public MainTabController(MainTabViewModel mainTabViewModel, SidebarStateViewModel sidebarViewModel,
                             AppContext context, ClusterNamespaceStateViewModel clusterNamespaceStateViewModel,
                             CredentialService credentialService, PodService podService, LogsService logsService,
                             KubeResourceService kubeResourceService) {
        this.mainTabViewModel = mainTabViewModel;
        this.sidebarViewModel = sidebarViewModel;
        this.context = context;
        this.clusterNamespaceStateViewModel = clusterNamespaceStateViewModel;
        this.credentialService = credentialService;
        this.podService = podService;
        this.logsService = logsService;
        this.kubeResourceService = kubeResourceService;
    }

    @FXML
    public void initialize() {
        mainTabViewModel.deploymentStateViewModel().selectedDeploymentProperty()
                .addListener((obs, old, selected) -> {
                    if (selected != null) {
                        log.info("Abriendo el deploymentStateView model con el nuevo deployment seleccionado: " +
                                         "{}", selected.getName());

                        openDeploymentTab(selected);
                    }
                });

        deploymentTabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, selectedTab) -> {
            if (selectedTab != null) {
                Object userData = selectedTab.getUserData();
                if (userData instanceof Deployment deployment) {
                    mainTabViewModel.deploymentStateViewModel().setSelectedDeployment(deployment);
                    log.info("🟢 Estado actualizado desde tab seleccionada: {}", deployment.getName());
                }
            }
        });

        sidebarViewModel.openTabRequestProperty().addListener((obs, old, req) -> {
            if (req == null) {
                return;
            }
            switch (req) {
                case ADD_CREDENTIALS -> openAddCredentialsTab();
            }
            sidebarViewModel.clearOpenTabRequest();
        });

        sidebarViewModel.closeNamespaceTabsRequestProperty().addListener((obs, old, ref) -> {
            if (ref != null) {
                closeNamespaceTabs(ref.clusterName(), ref.namespace());
                sidebarViewModel.clearCloseNamespaceTabsRequest();
            }
        });


    }

    private void openDeploymentTab(Deployment deployment) {
        var namespace = deployment.getNamespace();
        String suffix = namespace.substring(namespace.lastIndexOf('-') + 1);
        var existing = deploymentTabPane.getTabs().stream()
                .filter(t -> t.getText().startsWith(suffix + "/" + deployment.getName()))
                .filter(t -> ((Deployment) t.getUserData()).getNamespace().equals(deployment.getNamespace()))
                .findFirst();
        if (existing.isPresent()) {
            deploymentTabPane.getSelectionModel().select(existing.get());
            log.info("La pestaña [{}] ya esta abierta no se carga nuevamente desde cero.", existing.get().getText());
            return;
        }

        log.info("Instanciando DeploymentTabItemViewModel");

        DeploymentTabItemViewModel deploymentTabItemViewModel =
                new DeploymentTabItemViewModel(deployment.getClusterName(),
                                               deployment.getNamespace(),
                                               deployment.getName());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tabs/deployment-tab-content.fxml"));

            loader.setControllerFactory(type -> {
                if (type == DeploymentTabController.class) {
                    log.info("Instanciando deploymentTabController");
                    return new DeploymentTabController(deploymentTabItemViewModel, context, podService,
                                                       logsService, kubeResourceService);
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("No puedo instanciar " + type, e);
                }
            });

            AnchorPane content = loader.load();
            Tab tab = new Tab(suffix + "/" + deployment.getName(), content);
            tab.setUserData(deployment); // ✅ guardar el deployment real
            DeploymentTabController tabController = loader.getController();
            tab.setOnClosed(e -> tabController.dispose());

            deploymentTabPane.getTabs().add(tab);
            deploymentTabPane.getSelectionModel().select(tab);

        } catch (IOException e) {
            throw new RuntimeException("Error cargando /fxml/tabs/deployment-tab-content.fxml", e);
        }
    }

    /**
     * Cierra las pestañas de deployments de un namespace que se renombró o quitó (liberando sus streams de logs).
     */
    private void closeNamespaceTabs(String clusterName, String namespace) {
        List<Tab> toClose = deploymentTabPane.getTabs().stream()
                .filter(tab -> tab.getUserData() instanceof Deployment d
                        && namespace.equals(d.getNamespace())
                        && clusterName.equals(shortClusterName(d.getClusterName())))
                .toList();
        for (Tab tab : toClose) {
            log.info("Cerrando la pestaña {}: su namespace {} ya no está registrado", tab.getText(), namespace);
            deploymentTabPane.getTabs().remove(tab);
            // Quitarla por código no dispara onClosed: se dispara a mano para que libere el stream
            Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
        }
    }

    private static String shortClusterName(String clusterName) {
        return clusterName == null ? null : clusterName.substring(clusterName.lastIndexOf('/') + 1);
    }

    private void openAddCredentialsTab() {
        // Evitar duplicados
        for (Tab tab : deploymentTabPane.getTabs()) {
            if ("Credenciales AWS".equals(tab.getText())) {
                deploymentTabPane.getSelectionModel().select(tab);
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/usecase/add-credentials/add-credentials" +
                                                                              ".fxml"));
            loader.setControllerFactory(type -> {
                if (type == AddCredentialsController.class) {
                    return new AddCredentialsController(
                            new CredentialsStateViewModel(
                                    context.getAppState(), credentialService, clusterNamespaceStateViewModel));
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("No puedo instanciar " + type, e);
                }
            });

            Parent content = loader.load();

            Tab tab = new Tab("Credenciales AWS", content);
            tab.setClosable(true);
            deploymentTabPane.getTabs().add(tab);
            deploymentTabPane.getSelectionModel().select(tab);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando vista add-credentials.fxml", e);
        }
    }
}
