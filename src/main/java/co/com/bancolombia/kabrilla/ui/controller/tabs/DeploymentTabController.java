package co.com.bancolombia.kabrilla.ui.controller.tabs;

import java.io.IOException;

import co.com.bancolombia.kabrilla.bootstrap.AppBootstrap;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.ui.context.DeploymentContext;
import co.com.bancolombia.kabrilla.ui.controller.commands.PodsContentController;
import co.com.bancolombia.kabrilla.ui.viewmodel.pods.PodsViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.AppContext;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.tabs.DeploymentTabItemViewModel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentTabController {
    private static final Logger log = LoggerFactory.getLogger(DeploymentTabController.class);
    private final DeploymentTabItemViewModel deploymentTabItemViewModel;
    private final AppContext context;

    @FXML
    private TabPane commandTabPane;
    @FXML
    private AnchorPane commandContent;

    public DeploymentTabController(DeploymentTabItemViewModel deploymentTabItemViewModel, AppContext context) {
        this.deploymentTabItemViewModel = deploymentTabItemViewModel;
        this.context = context;
    }


    @FXML
    public void initialize() {
        log.info("aqui");
        commandTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            log.info("tab: {}", newTab.getText());
            if (newTab != null) {
                log.info("tab {}", newTab.getText());
                handleTabSelection(newTab);
            }
        });


        Platform.runLater(() -> selectTabByText("Logs"));


    }

    private void loadCommandView(String fxmlPath) {
        try {
            log.info("path: {}", fxmlPath);
            var fullPath = getClass().getResource("/fxml/commands/" + fxmlPath);
            log.info("full path {}", fullPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/commands/" + fxmlPath));

            // Ejemplo de controller factory manual:
            loader.setControllerFactory(type -> {
                if (type == PodsContentController.class) {
                    PodsViewModel podsVM = new PodsViewModel(AppBootstrap.getInstance().getPodService());
                    return new PodsContentController(podsVM);
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DeploymentTab) {
                ((DeploymentTab) controller).setDeploymentName(deploymentTabItemViewModel.deploymentName());
            }


            if (controller instanceof PodsContentController podsCtrl) {
                DeploymentContext ctx = buildContextFromAppState(deploymentTabItemViewModel);
                podsCtrl.setDeploymentContext(ctx);
            }


            commandContent.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private DeploymentContext buildContextFromAppState(DeploymentTabItemViewModel vm) {
        var clusterName = vm.clusterName();
        var namespace = vm.namespace();
        var creds = context.getAppState().getCredentials().stream()
                .filter(c -> c.clusterName().equals(clusterName.split("/")[1])).findFirst().orElse(null);

        KubeConfig kubeConfig = context.getAppState().getKubeConfig().stream()
                .filter(c -> c.clusterName().contains(clusterName))
                .map(c -> KubeConfig.builder()
                        .clusterName(c.clusterName())
                        .serverUrl(c.serverUrl())
                        .caCertBytes(c.caCertBytes())
                        .build())
                .findFirst()
                .orElse(null);
        return new DeploymentContext(clusterName, namespace, vm.deploymentName(), creds, kubeConfig);
    }


    private void selectTabByText(String title) {
        log.info("Buscando tab: {}", title);
        for (Tab tab : commandTabPane.getTabs()) {
            if (title.equals(tab.getText())) {
                var sm = commandTabPane.getSelectionModel();
                if (sm.getSelectedItem() == tab) {
                    log.info("La tab '{}' ya estaba seleccionada, cargo manualmente", title);
                    handleTabSelection(tab); // <-- fuerza la carga
                } else {
                    log.info("Seleccionando tab '{}'", title);
                    sm.select(tab); // disparará el listener
                }
                return;
            }
        }
        log.warn("No encontré una Tab con título '{}'", title);
    }

    private void handleTabSelection(Tab newTab) {
        switch (newTab.getText()) {
            case "Logs" -> loadCommandView("logs/logs-content.fxml");
            case "Pods" -> loadCommandView("pods/pods-content.fxml");
            case "ConfigMap" -> loadCommandView("configmap/configmap-content.fxml");
            case "RequestPolicy" -> loadCommandView("requestauthentication/requestpolicy-content.fxml");
            case "Service" -> loadCommandView("services/service-content.fxml");
            case "AuthorizationPolicy" -> loadCommandView("authorizationpolicy/authorizationpolicy-content.fxml");
            case "HPA" -> loadCommandView("hpa/hpa-content.fxml");
        }
    }


}
