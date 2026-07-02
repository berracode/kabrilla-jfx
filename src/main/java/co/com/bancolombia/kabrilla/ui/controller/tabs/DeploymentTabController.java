package co.com.bancolombia.kabrilla.ui.controller.tabs;

import java.io.IOException;

import co.com.bancolombia.kabrilla.bootstrap.AppBootstrap;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.ui.context.DeploymentContext;
import co.com.bancolombia.kabrilla.ui.controller.commands.LogsContentController;
import co.com.bancolombia.kabrilla.ui.controller.commands.PodsContentController;
import co.com.bancolombia.kabrilla.ui.viewmodel.logs.LogsViewModel;
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
    private PodsViewModel podsViewModel;
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
        log.info("INITIALIZE DEPLOYMENT TAB CONTROLLER");
        commandTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            log.info("tab: {}", newTab.getText());
            if (newTab != null) {
                log.info("tab {}", newTab.getText());
                //handleTabSelection(newTab);
            }
        });

        initializeCommandsTabsView();
        Platform.runLater(() -> selectTabByText("Pods"));


    }

    private void loadCommandView(String fxmlPath, String tabTitle) {
        try {
            log.info("path: {}", fxmlPath);
            var fullPath = getClass().getResource("/fxml/commands/" + fxmlPath);
            log.info("full path {}", fullPath);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/commands/" + fxmlPath));

            // Ejemplo de controller factory manual:
            loader.setControllerFactory(type -> {
                if (podsViewModel == null) {
                    podsViewModel = new PodsViewModel(AppBootstrap.getInstance().getPodService());
                }
                if (type == PodsContentController.class) {
                    return new PodsContentController(podsViewModel);
                } else if (type == LogsContentController.class) {
                    var logViewModel = new LogsViewModel(AppBootstrap.getInstance().getLogsService());
                    return new LogsContentController(podsViewModel, logViewModel);
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


            DeploymentContext ctx =
                    buildContextFromAppState(deploymentTabItemViewModel);

            if (controller instanceof PodsContentController podsCtrl) {
                podsCtrl.setDeploymentContext(ctx);
            } else if (controller instanceof LogsContentController logsContentController) {
                logsContentController.setDeploymentContext(ctx);

            }

            var tabOptional =
                    commandTabPane.getTabs().stream().filter(tab -> tab.getText().equals(tabTitle))
                            .findFirst();//esto no estaba
            if (tabOptional.isEmpty()) {//esto no estaba
                throw new RuntimeException("No tab");
            }
            tabOptional.get().setContent(view);//esto no estaba
            commandTabPane.getSelectionModel().select(tabOptional.get()); //esto no estaba


//            commandContent.getChildren().setAll(view);
//            AnchorPane.setTopAnchor(view, 0.0);
//            AnchorPane.setBottomAnchor(view, 0.0);
//            AnchorPane.setLeftAnchor(view, 0.0);
//            AnchorPane.setRightAnchor(view, 0.0);
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


    private void initializeCommandsTabsView() {
        log.info("Iniciando todas las vistas tab");
        for (Tab tab : commandTabPane.getTabs()) {
            log.info("La tab '{}' inicia carga manualmente", tab.getText());
            handleTabSelection(tab); // <-- fuerza la carga
        }
    }

    private void handleTabSelection(Tab newTab) {
        switch (newTab.getText()) {
            case "Logs" -> loadCommandView("logs/logs-content.fxml", newTab.getText());
            case "Pods" -> loadCommandView("pods/pods-content.fxml", newTab.getText());
            case "ConfigMap" -> loadCommandView("configmap/configmap-content.fxml", newTab.getText());
            case "RequestPolicy" ->
                    loadCommandView("requestauthentication/requestpolicy-content.fxml", newTab.getText());
            case "Service" -> loadCommandView("services/service-content.fxml", newTab.getText());
            case "AuthorizationPolicy" ->
                    loadCommandView("authorizationpolicy/authorizationpolicy-content.fxml", newTab.getText());
            case "HPA" -> loadCommandView("hpa/hpa-content.fxml", newTab.getText());
        }
    }


}
