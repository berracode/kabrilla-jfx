package co.com.bancolombia.kabrilla.ui.controller.commands;


import java.util.List;

import co.com.bancolombia.kabrilla.ui.controller.tabs.DeploymentTab;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class PodsContentController implements DeploymentTab {

    @FXML
    private TableView<PodRow> podsTable;

    @FXML
    public void initialize() {
        TableColumn<PodRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                                            new javafx.beans.property.SimpleStringProperty(data.getValue().name()));

        TableColumn<PodRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                                              new javafx.beans.property.SimpleStringProperty(data.getValue().status()));

        TableColumn<PodRow, String> restartsCol = new TableColumn<>("Restarts");
        restartsCol.setCellValueFactory(data ->
                                                new javafx.beans.property.SimpleStringProperty(
                                                        String.valueOf(data.getValue().restarts())));

        TableColumn<PodRow, String> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(data ->
                                           new javafx.beans.property.SimpleStringProperty(data.getValue().age()));

        TableColumn<PodRow, String> nodeCol = new TableColumn<>("Node");
        nodeCol.setCellValueFactory(data ->
                                            new javafx.beans.property.SimpleStringProperty(data.getValue().node()));

        podsTable.getColumns().setAll(nameCol, statusCol, restartsCol, ageCol, nodeCol);
    }

    @Override
    public void setDeploymentName(String deploymentName) {
        List<PodRow> mockPods = List.of(
                new PodRow(deploymentName + "-a1b2c", "Running", 0, "4m", "node-1"),
                new PodRow(deploymentName + "-d3e4f", "Running", 0, "3m", "node-2"),
                new PodRow(deploymentName + "-g5h6i", "CrashLoopBackOff", 5, "2m", "node-3")
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
