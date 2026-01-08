package co.com.bancolombia.kabrilla.ui.controller.commands;


import java.util.List;
import java.util.Objects;

import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;
import co.com.bancolombia.kabrilla.ui.context.DeploymentContext;
import co.com.bancolombia.kabrilla.ui.controller.tabs.DeploymentTab;
import co.com.bancolombia.kabrilla.ui.viewmodel.pods.PodsViewModel;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PodsContentController implements DeploymentTab {

    private final PodsViewModel viewModel;
    private DeploymentContext currentCtx;

    @FXML
    private TableView<Pod> podsTable;


    public PodsContentController(PodsViewModel viewModel) {
        this.viewModel = viewModel;
    }


    @FXML
    public void initialize() {
        TableColumn<Pod, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Pod, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<Pod, String> restartsCol = new TableColumn<>("Restarts");
        restartsCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getRestarts())));

        TableColumn<Pod, String> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAge()));

        TableColumn<Pod, String> nodeCol = new TableColumn<>("Node");
        nodeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNode()));

        podsTable.getColumns().setAll(nameCol, statusCol, restartsCol, ageCol, nodeCol);

        podsTable.setItems(viewModel.pods());

        podsTable.setFixedCellSize(35);
        podsTable.prefHeightProperty().bind(
                Bindings.size(viewModel.pods()).multiply(podsTable.getFixedCellSize()).add(28.0) // header aprox
        );
        podsTable.minHeightProperty().bind(podsTable.prefHeightProperty());
        podsTable.maxHeightProperty().bind(podsTable.prefHeightProperty());

        // Recalcular cuando cambian los elementos de pods del vm, es decir observabmos la lista de pdos
        viewModel.pods().addListener((ListChangeListener<Pod>) change -> {
            Platform.runLater(() -> autoResizeNameColumn(nameCol));
        });

        // También recalcular al final del initialize, por si ya hay ítems
        Platform.runLater(() -> autoResizeNameColumn(nameCol));
    }

    private void autoResizeNameColumn(TableColumn<Pod, String> nameCol) {
        Font font = Font.getDefault();
        double headerWidth = computeTextWidth(font, nameCol.getText());
        double maxCellWidth = headerWidth;

        // Margen/padding aproximado de las celdas + espacio
        double padding = 24.0;

        for (Pod pod : viewModel.pods()) {
            String text = pod.getName() != null ? pod.getName() : "";
            double w = computeTextWidth(font, text);
            if (w > maxCellWidth) {
                maxCellWidth = w;
            }
        }

        // Ancho mínimo por si hay lista vacía
        double minWidth = 80.0;
        nameCol.setPrefWidth(Math.max(minWidth, maxCellWidth + padding));
    }

    /**
     * Mide el ancho del texto con una fuente dada, usando un nodo Text.
     */
    private double computeTextWidth(Font font, String text) {
        Text t = new Text(text);
        t.setFont(font);
        // t.getStyleClass().add("table-cell");
        return t.getBoundsInLocal().getWidth();
    }


    @Override
    public void setDeploymentName(String deploymentName) {
        log.info("Deployment name: {}", deploymentName);
        if (currentCtx != null && !Objects.equals(currentCtx.deploymentName(), deploymentName)) {
            currentCtx = new DeploymentContext(
                    currentCtx.clusterName(),
                    currentCtx.namespace(),
                    deploymentName,
                    currentCtx.credentials(),
                    currentCtx.kubeConfig()
            );
        }
        if (currentCtx != null) {
            viewModel.loadPods(currentCtx);
        }
    }

    public void setDeploymentContext(DeploymentContext ctx) {
        this.currentCtx = ctx;
        log.info("Context deployment: {}", ctx.deploymentName());
        if (ctx.deploymentName() != null) {
            viewModel.loadPods(ctx);
        }
    }


    /**
     * En desuso, eliminar cuanto antes
     *
     * @param deploymentName
     */
    public void setDeploymentName2(String deploymentName) {
        List<Pod> mockPods = List.of(
                new Pod(deploymentName + "-a1b2c", "Running", 0, "4m", "node-1"),
                new Pod(deploymentName + "-d3e4f", "Running", 0, "3m", "node-2"),
                new Pod(deploymentName + "-g5h6i", "CrashLoopBackOff", 5, "2m", "node-3")
        );

        podsTable.setItems(FXCollections.observableList(mockPods));

        // lo uso pa ajustar la altura al numero exacto de filas
        podsTable.setFixedCellSize(28);
        podsTable.prefHeightProperty().bind(
                podsTable.fixedCellSizeProperty()
                        .multiply(FXCollections.observableList(mockPods).size() + 1.01) // + header
        );
        podsTable.minHeightProperty().bind(podsTable.prefHeightProperty());
        podsTable.maxHeightProperty().bind(podsTable.prefHeightProperty());
    }

    public record PodRow(String name, String status, int restarts, String age, String node) {
    }
}
