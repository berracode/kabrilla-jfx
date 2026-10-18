package co.com.bancolombia.kabrilla.ui.controller.sidebar;

import co.com.bancolombia.kabrilla.ui.viewmodel.cluster.SidebarStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentStateViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SidebarController {
    private final SidebarStateViewModel sidebarStateViewModel;
    private final DeploymentStateViewModel deploymentStateViewModel;
    @FXML
    private TreeView<String> treeView;
    @FXML
    private Button createNamespaceButton;
    @FXML
    private Button addCredentialsButton;


    public SidebarController(SidebarStateViewModel sidebarStateViewModel,
                             DeploymentStateViewModel deploymentStateViewModel) {
        this.sidebarStateViewModel = sidebarStateViewModel;
        this.deploymentStateViewModel = deploymentStateViewModel;
    }

    @FXML
    public void initialize() {
        log.info("Aqui 1.1");
        treeView.setRoot(sidebarStateViewModel.getRoot());
        treeView.setShowRoot(false);

        sidebarStateViewModel.loadClustersAsync(sidebarStateViewModel.getRoot());

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && sidebarStateViewModel.isDeploymentItem(
                    selected) && selected.isLeaf()) {
                deploymentStateViewModel.setSelectedDeployment(sidebarStateViewModel.resolveDeployment2(selected));
            }
        });

        createNamespaceButton.setOnAction(e -> sidebarStateViewModel.openNamespaceDialog());

        addCredentialsButton.setOnAction(e -> sidebarStateViewModel.openAddCredentialsTab());

        sidebarStateViewModel.bindTreeViewToState(sidebarStateViewModel.getRoot());
    }


}
