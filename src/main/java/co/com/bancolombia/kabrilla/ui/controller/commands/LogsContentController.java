package co.com.bancolombia.kabrilla.ui.controller.commands;

import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;
import co.com.bancolombia.kabrilla.ui.controller.tabs.DeploymentTab;
import co.com.bancolombia.kabrilla.ui.viewmodel.pods.PodsViewModel;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogsContentController implements DeploymentTab {

    private final PodsViewModel viewModel;

    @FXML
    private ComboBox<Pod> comboBox;
    @FXML
    private TextArea logsArea;

    public LogsContentController(PodsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {

        // Enlazar la lista del combo con la lista del ViewModel
        comboBox.setItems(viewModel.pods());

        // Mostrar solo el nombre del pod en cada celda
        comboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Pod item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        // Mostrar nombre también cuando está seleccionado (botón del combo)
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Pod item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Seleccionar el primero cuando lleguen datos (si aún no hay selección)
        viewModel.pods().addListener((ListChangeListener<Pod>) c -> {
            if (!viewModel.pods().isEmpty() && comboBox.getSelectionModel().isEmpty()) {
                comboBox.getSelectionModel().selectFirst();
            }
        });

        // Listener de selección: aquí podrás disparar la carga de logs del pod elegido
        comboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldPod, newPod) -> {
            if (newPod != null) {
                log.info("Pod seleccionado: {}", newPod.getName());
                // TODO: Llama a tu servicio de logs con newPod.getName() y el DeploymentContext
                // logsArea.setText(servicioLogs.getLogs(ctx, newPod.getName()));
            }
        });

    }

    @Override
    public void setDeploymentName(String name) {
        logsArea.setText("Logs para " + name + "=# _ 2W".repeat(900));
    }
}