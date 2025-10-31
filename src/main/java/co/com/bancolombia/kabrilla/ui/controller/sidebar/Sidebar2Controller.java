package co.com.bancolombia.kabrilla.ui.controller.sidebar;

import co.com.bancolombia.kabrilla.ui.viewmodel.cluster.SidebarViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentStateViewModel;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sidebar2Controller {
    private static final Logger log = LoggerFactory.getLogger(Sidebar2Controller.class);
    private final SidebarViewModel sidebarViewModel;
    private final DeploymentStateViewModel deploymentStateViewModel;
    @FXML
    private TreeView<String> treeView;
    @FXML
    private Button createNamespaceButton;
    @FXML
    private Button addCredentialsButton;


    public Sidebar2Controller(SidebarViewModel sidebarViewModel, DeploymentStateViewModel deploymentStateViewModel) {
        this.sidebarViewModel = sidebarViewModel;
        this.deploymentStateViewModel = deploymentStateViewModel;
    }

    @FXML
    public void initialize() {
        log.info("Aqui 1.1");
        treeView.setRoot(sidebarViewModel.getRoot());
        treeView.setShowRoot(false);

        sidebarViewModel.loadClustersAsync(sidebarViewModel.getRoot());

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && sidebarViewModel.isDeploymentItem(
                    selected) && selected.isLeaf() && selected.getValue().startsWith("deploy")) {
                deploymentStateViewModel.setSelectedDeployment(sidebarViewModel.resolveDeployment(selected));
            }
        });

        createNamespaceButton.setOnAction(e -> sidebarViewModel.openNamespaceDialog());

        addCredentialsButton.setOnAction(e -> sidebarViewModel.openAddCredentialsTab());

        sidebarViewModel.bindTreeViewToState(sidebarViewModel.getRoot());
    }


}
