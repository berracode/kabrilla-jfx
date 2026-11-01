package co.com.bancolombia.kabrilla.ui.controller.commands;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLevel;
import co.com.bancolombia.kabrilla.ui.context.DeploymentContext;
import co.com.bancolombia.kabrilla.ui.controller.tabs.DeploymentTab;
import co.com.bancolombia.kabrilla.ui.theme.StyleToggle;
import co.com.bancolombia.kabrilla.ui.viewmodel.logs.LogFilter;
import co.com.bancolombia.kabrilla.ui.viewmodel.logs.LogView;
import co.com.bancolombia.kabrilla.ui.viewmodel.logs.LogsViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.pods.PodsViewModel;

import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.InlineCssTextArea;


@Slf4j
public class LogsContentController implements DeploymentTab, LogView {

    private static final String HIGHLIGHT_STYLE = "-rtfx-background-color: #FFE066;";
    private static final String ALL_LEVELS = "Todos";
    // Espera a que el usuario deje de escribir antes de refiltrar todo el historial
    private static final Duration SEARCH_DEBOUNCE = Duration.millis(250);

    private final PodsViewModel podsVM;   // ViewModel compartido de Pods
    private final LogsViewModel logsVM;   // ViewModel dedicado a Logs

    @FXML
    private ComboBox<Pod> podCombo;
    @FXML
    private ComboBox<String> containerCombo;
    @FXML
    private CheckBox autoScrollCheck;
    @FXML
    private Label errorLabel;
    @FXML
    private ProgressIndicator progressFollowing;
    @FXML
    private Button btnToggleStream;
    @FXML
    private InlineCssTextArea txtLogArea;
    @FXML
    private TextField txtSearch;
    @FXML
    private CheckBox regexCheck;
    @FXML
    private CheckBox caseSensitiveCheck;
    @FXML
    private ComboBox<String> levelCombo;
    @FXML
    private Label matchCountLabel;

    private DeploymentContext ctx;
    private String filterError;
    // Evita reabrir el stream mientras se reemplazan los containers al cambiar de pod
    private boolean updatingContainers;


    public LogsContentController(PodsViewModel podsVM, LogsViewModel logsVM) {
        this.podsVM = podsVM;
        this.logsVM = logsVM;
    }


    @FXML
    public void initialize() {
        txtLogArea.setEditable(false);
        // Fuente monoespaciada y resaltado son funcionales: se conservan aunque los estilos estén desactivados
        StyleToggle.preserve(txtLogArea);
        logsVM.setView(this);

        initializePodCombo();
        initializeFilterControls();

        // Estado/errores e indicador de seguimiento
        errorLabel.textProperty().bind(logsVM.errorProperty());
        progressFollowing.visibleProperty().bind(logsVM.followingProperty());
        btnToggleStream.textProperty().bind(
                Bindings.when(logsVM.followingProperty()).then("Detener").otherwise("Reanudar"));
    }

    private void initializePodCombo() {
        podCombo.setItems(podsVM.pods());
        podCombo.setCellFactory(cb -> new PodCell());
        podCombo.setButtonCell(new PodCell());

        // Seleccionar el primero cuando lleguen pods
        podsVM.pods().addListener((ListChangeListener<Pod>) c -> {
            if (!podsVM.pods().isEmpty() && podCombo.getSelectionModel().isEmpty()) {
                podCombo.getSelectionModel().selectFirst();
            }
        });

        // Al cambiar de pod: cargar sus containers con el de la app preseleccionado y (re)abrir el stream
        podCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldPod, newPod) -> {
            log.info("Cambio selección ComboBox: {}", newPod != null ? newPod.getName() : "null");
            updatingContainers = true;
            if (newPod == null) {
                containerCombo.getItems().clear();
                updatingContainers = false;
                logsVM.stop();
                return;
            }
            containerCombo.getItems().setAll(newPod.getContainers());
            containerCombo.setValue(newPod.getDefaultContainer());
            containerCombo.setDisable(newPod.getContainers().size() <= 1);
            updatingContainers = false;
            restartStream();
        });

        // Al cambiar de container (p. ej. ver istio-proxy) se reabre el stream del mismo pod
        containerCombo.valueProperty().addListener((obs, old, container) -> {
            if (!updatingContainers && container != null) {
                restartStream();
            }
        });
    }

    private void initializeFilterControls() {
        levelCombo.getItems().add(ALL_LEVELS);
        List.of(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)
                .forEach(level -> levelCombo.getItems().add(level.name()));
        levelCombo.getSelectionModel().selectFirst();

        PauseTransition debounce = new PauseTransition(SEARCH_DEBOUNCE);
        debounce.setOnFinished(e -> applyFilter());
        txtSearch.textProperty().addListener((obs, old, text) -> debounce.playFromStart());
        regexCheck.selectedProperty().addListener((obs, old, selected) -> applyFilter());
        caseSensitiveCheck.selectedProperty().addListener((obs, old, selected) -> applyFilter());
        levelCombo.valueProperty().addListener((obs, old, level) -> applyFilter());

        logsVM.totalLinesProperty().addListener((obs, old, total) -> updateMatchCount());
        logsVM.visibleLinesProperty().addListener((obs, old, visible) -> updateMatchCount());
        updateMatchCount();
    }

    private void applyFilter() {
        String level = levelCombo.getValue();
        LogFilter filter = LogFilter.of(
                txtSearch.getText(),
                regexCheck.isSelected(),
                caseSensitiveCheck.isSelected(),
                level == null || ALL_LEVELS.equals(level) ? null : LogLevel.valueOf(level));
        filterError = filter.error().orElse(null);
        logsVM.setFilter(filter);
        updateMatchCount();
    }

    private void updateMatchCount() {
        if (filterError != null) {
            matchCountLabel.setText(filterError);
        } else if (logsVM.getFilter().isActive()) {
            matchCountLabel.setText(logsVM.visibleLinesProperty().get() + " de " + logsVM.totalLinesProperty().get()
                                            + " líneas");
        } else {
            matchCountLabel.setText(logsVM.totalLinesProperty().get() + " líneas");
        }
    }

    // --- LogView ---

    @Override
    public void append(List<String> lines) {
        // El último párrafo siempre es el vacío que deja el "\n" final: ahí empiezan las líneas nuevas
        int firstParagraph = txtLogArea.getParagraphs().size() - 1;
        txtLogArea.appendText(String.join("\n", lines) + "\n");
        highlight(firstParagraph, lines);
        trimToMaxParagraphs();
        followIfEnabled();
    }

    @Override
    public void reset(List<String> lines) {
        txtLogArea.replaceText(lines.isEmpty() ? "" : String.join("\n", lines) + "\n");
        highlight(0, lines);
        followIfEnabled();
    }

    private void highlight(int firstParagraph, List<String> lines) {
        LogFilter filter = logsVM.getFilter();
        for (int i = 0; i < lines.size(); i++) {
            for (int[] range : filter.highlights(lines.get(i))) {
                txtLogArea.setStyle(firstParagraph + i, range[0], range[1], HIGHLIGHT_STYLE);
            }
        }
    }

    private void trimToMaxParagraphs() {
        // -1: no contar el párrafo vacío final
        int overflow = txtLogArea.getParagraphs().size() - 1 - LogsViewModel.MAX_RENDERED_LINES;
        if (overflow > 0) {
            log.debug("Se procede a limpiar lineas de UI {}", overflow);
            txtLogArea.deleteText(0, txtLogArea.getAbsolutePosition(overflow, 0));
        }
    }

    private void followIfEnabled() {
        if (autoScrollCheck.isSelected()) {
            txtLogArea.moveTo(txtLogArea.getLength());
            txtLogArea.requestFollowCaret();
        }
    }

    // --- Contexto y stream ---

    /**
     * Llamado por el padre (DeploymentTabController) al crear la vista.
     */
    public void setDeploymentContext(DeploymentContext ctx) {
        this.ctx = ctx;
        log.info("setDeploymentContext recibido: cluster={} ns={} deploy={}",
                 ctx.clusterName(), ctx.namespace(), ctx.deploymentName());
        // Si el pod se seleccionó antes de tener contexto, el stream arranca ahora
        restartStream();
    }

    /**
     * Abre el stream del pod y container seleccionados (si ya hay contexto y pod).
     */
    private void restartStream() {
        Pod pod = podCombo.getSelectionModel().getSelectedItem();
        if (pod == null || ctx == null) {
            return;
        }
        String container = containerCombo.getValue();
        logsVM.clear();
        logsVM.startFollowing(
                ctx.kubeConfig(), ctx.awsCredentials(),
                ctx.clusterName(), ctx.namespace(), ctx.deploymentName(),
                pod.getName(),
                container, // null solo si el pod no reporta containers: la API usa el único que haya
                false,     // timestamps: si tu cluster los agrega en el payload y quieres mostrarlos
                200,       // tail inicial para contexto
                null       // sinceSeconds
        );
        log.info("Siguiendo logs de pod {} container {}", pod.getName(), container);
    }


    /**
     * Limpia y/o detiene stream si cierras la Tab.
     */
    public void dispose() {
        logsVM.stop();
    }

    @Override
    public void setDeploymentName(String name) {
        // Conservado por compatibilidad, pero este controller necesita el contexto completo.
        // Prefiere setDeploymentContext(ctx).
    }

    @FXML
    private void onClearClicked() {
        logsVM.clear();
    }

    @FXML
    private void onToggleStreamClicked() {
        if (logsVM.followingProperty().get()) {
            logsVM.stop();
            return;
        }
        // Reanudar: vuelve a abrir el stream del pod seleccionado (trae de nuevo las últimas 200 líneas)
        restartStream();
    }

    @FXML
    private void onRefreshPodsClicked() {
        if (ctx != null) {
            podsVM.loadPods(ctx);
        }
    }

    private static final class PodCell extends ListCell<Pod> {
        @Override
        protected void updateItem(Pod item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.getName());
        }
    }
}
