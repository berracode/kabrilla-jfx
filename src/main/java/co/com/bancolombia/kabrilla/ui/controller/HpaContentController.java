package co.com.bancolombia.kabrilla.ui.controller;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class HpaContentController implements DeploymentTab {

    @FXML
    private Label currentReplicasLabel;
    @FXML
    private Label desiredReplicasLabel;
    @FXML
    private Label targetCpuLabel;
    @FXML
    private Label currentCpuLabel;
    @FXML
    private TableView<PodMetricRow> metricsTable;

    @FXML
    public void initialize() {
        TableColumn<PodMetricRow, String> podCol = new TableColumn<>("Pod");
        podCol.setCellValueFactory(data ->
                                           new javafx.beans.property.SimpleStringProperty(data.getValue().pod()));

        TableColumn<PodMetricRow, String> cpuCol = new TableColumn<>("CPU %");
        cpuCol.setCellValueFactory(data ->
                                           new javafx.beans.property.SimpleStringProperty(
                                                   data.getValue().cpuUsage()));

        TableColumn<PodMetricRow, String> memCol = new TableColumn<>("Memory (MiB)");
        memCol.setCellValueFactory(data ->
                                           new javafx.beans.property.SimpleStringProperty(
                                                   data.getValue().memoryUsage()));

        TableColumn<PodMetricRow, String> restartsCol = new TableColumn<>("Restarts");
        restartsCol.setCellValueFactory(data ->
                                                new javafx.beans.property.SimpleStringProperty(
                                                        String.valueOf(data.getValue().restarts())));

        TableColumn<PodMetricRow, String> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(data ->
                                           new javafx.beans.property.SimpleStringProperty(data.getValue().age()));

        metricsTable.getColumns().setAll(podCol, cpuCol, memCol, restartsCol, ageCol);
    }


    @Override
    public void setDeploymentName(String deploymentName) {
        currentReplicasLabel.setText("3");
        desiredReplicasLabel.setText("5");
        targetCpuLabel.setText("70%");
        currentCpuLabel.setText("58%");

        List<PodMetricRow> metrics = List.of(
                new PodMetricRow(deploymentName + "-a1b2c", "60%", "245", 0, "4m"),
                new PodMetricRow(deploymentName + "-d3e4f", "57%", "230", 0, "3m"),
                new PodMetricRow(deploymentName + "-g5h6i", "45%", "260", 1, "2m")
        );

        metricsTable.setItems(FXCollections.observableList(metrics));

        metricsTable.setFixedCellSize(28);
        metricsTable.prefHeightProperty().bind(
                metricsTable.fixedCellSizeProperty()
                        .multiply(metrics.size() + 1.01)
        );
        metricsTable.minHeightProperty().bind(metricsTable.prefHeightProperty());
        metricsTable.maxHeightProperty().bind(metricsTable.prefHeightProperty());
    }

    public record PodMetricRow(String pod, String cpuUsage, String memoryUsage, int restarts, String age) {
    }
}
