package co.com.bancolombia.kabrilla.ui.controller.tabs;

import java.io.IOException;

import co.com.bancolombia.kabrilla.bootstrap.AppBootstrap;
import co.com.bancolombia.kabrilla.domain.model.Deployment;
import co.com.bancolombia.kabrilla.ui.controller.usecases.AddCredentialsController;
import co.com.bancolombia.kabrilla.ui.viewmodel.cluster.SidebarStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.tabs.DeploymentTabItemViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.tabs.MainTabViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTabController {
    private static final Logger log = LoggerFactory.getLogger(MainTabController.class);

    private final MainTabViewModel mainTabViewModel;
    private final SidebarStateViewModel sidebarViewModel; // 👈 agregado

    @FXML
    private TabPane deploymentTabPane;

    public MainTabController(MainTabViewModel mainTabViewModel, SidebarStateViewModel sidebarViewModel) {
        this.mainTabViewModel = mainTabViewModel;
        this.sidebarViewModel = sidebarViewModel;
    }

    @FXML
    public void initialize() {
        mainTabViewModel.deploymentStateViewModel().selectedDeploymentProperty()
                .addListener((obs, old, selected) -> {
                    log.info("selected deployment para deployment tab: {}", selected.getName());
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


    }

    private void openDeploymentTab(Deployment deployment) {
        var existing = deploymentTabPane.getTabs().stream()
                .filter(t -> t.getText().equals(deployment.getName()))
                .findFirst();
        if (existing.isPresent()) {
            deploymentTabPane.getSelectionModel().select(existing.get());
            return;
        }
        DeploymentTabItemViewModel deploymentTabItemViewModel =
                new DeploymentTabItemViewModel(deployment.getClusterName(),
                                               deployment.getNamespace(),
                                               deployment.getName());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tabs/deployment-tab-content.fxml"));

            loader.setControllerFactory(type -> {
                if (type == DeploymentTabController.class) {
                    return new DeploymentTabController(deploymentTabItemViewModel); // inyección del VM
                }
                try {
                    return type.getDeclaredConstructor().newInstance(); // otros controladores con no-args
                } catch (Exception e) {
                    throw new RuntimeException("No puedo instanciar " + type, e);
                }
            });

            AnchorPane content = loader.load();
            Tab tab = new Tab(deployment.getName(), content);
            tab.setUserData(deployment); // ✅ guardar el deployment real

            deploymentTabPane.getTabs().add(tab);
            deploymentTabPane.getSelectionModel().select(tab);

        } catch (IOException e) {
            throw new RuntimeException("Error cargando /fxml/tabs/deployment-tab-content.fxml", e);
        }
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
                            AppBootstrap.getInstance().getClusterNamespaceStateViewModel(),
                            AppBootstrap.getInstance().getCredentialService()
                    );
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
