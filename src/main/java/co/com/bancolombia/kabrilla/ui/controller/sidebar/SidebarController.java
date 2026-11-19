package co.com.bancolombia.kabrilla.ui.controller.sidebar;

import java.util.Optional;

import co.com.bancolombia.kabrilla.ui.services.ErrorMessages;
import co.com.bancolombia.kabrilla.ui.viewmodel.cluster.SidebarStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentStateViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
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
        treeView.setRoot(sidebarStateViewModel.getRoot());
        treeView.setShowRoot(false);
        treeView.setCellFactory(tree -> new SidebarCell());

        sidebarStateViewModel.loadClustersAsync(sidebarStateViewModel.getRoot());

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && sidebarStateViewModel.isDeploymentItem(
                    selected) && selected.isLeaf()) {
                deploymentStateViewModel.setSelectedDeployment(sidebarStateViewModel.resolveDeployment2(selected));
            }
        });

        // Atajos sobre el namespace seleccionado
        treeView.setOnKeyPressed(event -> {
            TreeItem<String> selected = treeView.getSelectionModel().getSelectedItem();
            if (!sidebarStateViewModel.isNamespaceItem(selected)) {
                return;
            }
            if (event.getCode() == KeyCode.F2) {
                renameNamespace(selected);
                event.consume();
            } else if (event.getCode() == KeyCode.DELETE) {
                removeNamespace(selected);
                event.consume();
            }
        });

        createNamespaceButton.setOnAction(e -> sidebarStateViewModel.openNamespaceDialog());

        addCredentialsButton.setOnAction(e -> sidebarStateViewModel.openAddCredentialsTab());

        sidebarStateViewModel.bindTreeViewToState(sidebarStateViewModel.getRoot());
    }

    /**
     * Pide el nombre nuevo hasta que sea válido o el usuario cancele (conserva lo escrito si hay error).
     */
    private void renameNamespace(TreeItem<String> nsItem) {
        String clusterName = nsItem.getParent().getValue();
        String current = nsItem.getValue();
        String proposal = current;
        while (true) {
            TextInputDialog dialog = new TextInputDialog(proposal);
            dialog.setTitle("Renombrar namespace");
            dialog.setHeaderText("Namespace " + current + " del cluster " + clusterName);
            dialog.setContentText("Nuevo nombre:");
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }
            proposal = result.get();
            try {
                sidebarStateViewModel.renameNamespace(nsItem, proposal);
                return;
            } catch (IllegalArgumentException e) {
                showError("No se puede renombrar", e.getMessage());
            } catch (RuntimeException e) {
                log.error("No se pudo renombrar el namespace {} de {}", current, clusterName, e);
                showError("No se pudo renombrar", ErrorMessages.describe(e));
                return;
            }
        }
    }

    private void removeNamespace(TreeItem<String> nsItem) {
        String clusterName = nsItem.getParent().getValue();
        String namespace = nsItem.getValue();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Quitar namespace");
        confirm.setHeaderText("¿Quitar " + namespace + " de la lista de " + clusterName + "?");
        confirm.setContentText("Solo deja de mostrarse en Kabrilla: no se borra nada en el cluster. "
                                       + "Se cerrarán las pestañas abiertas de sus deployments.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            sidebarStateViewModel.removeNamespace(nsItem);
        } catch (RuntimeException e) {
            log.error("No se pudo quitar el namespace {} de {}", namespace, clusterName, e);
            showError("No se pudo quitar el namespace", ErrorMessages.describe(e));
        }
    }

    private static void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Namespaces");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Celda del árbol: muestra texto e ícono del TreeItem y, en los namespaces, el menú contextual.
     */
    private final class SidebarCell extends TreeCell<String> {
        private final ContextMenu namespaceMenu = new ContextMenu();

        SidebarCell() {
            MenuItem rename = new MenuItem("Renombrar…  (F2)");
            rename.setOnAction(e -> renameNamespace(getTreeItem()));
            MenuItem remove = new MenuItem("Quitar de la lista…  (Supr)");
            remove.setOnAction(e -> removeNamespace(getTreeItem()));
            namespaceMenu.getItems().addAll(rename, remove);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            setText(item);
            setGraphic(getTreeItem().getGraphic());
            setContextMenu(sidebarStateViewModel.isNamespaceItem(getTreeItem()) ? namespaceMenu : null);
        }
    }
}
