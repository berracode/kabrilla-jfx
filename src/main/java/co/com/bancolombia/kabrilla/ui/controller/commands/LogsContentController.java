package co.com.bancolombia.kabrilla.ui.controller.commands;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLevel;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogRange;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.InlineCssTextArea;


@Slf4j
public class LogsContentController implements DeploymentTab, LogView {

    private static final String HIGHLIGHT_STYLE = "-rtfx-background-color: #FFE066;";
    private static final String ALL_LEVELS = "Todos";
    // Espera a que el usuario deje de escribir antes de refiltrar todo el historial
    private static final Duration SEARCH_DEBOUNCE = Duration.millis(250);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String RANGE_LIVE = "En vivo";
    private static final String RANGE_15_MIN = "Últimos 15 min";
    private static final String RANGE_1_HOUR = "Última hora";
    private static final String RANGE_6_HOURS = "Últimas 6 h";
    private static final String RANGE_ALL = "Todo lo disponible";
    private static final String RANGE_CUSTOM = "Personalizado";

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
    @FXML
    private Label infoLabel;
    @FXML
    private ComboBox<String> rangeCombo;
    @FXML
    private HBox customRangeBox;
    @FXML
    private DatePicker fromDate;
    @FXML
    private TextField fromTime;
    @FXML
    private DatePicker toDate;
    @FXML
    private TextField toTime;
    @FXML
    private CheckBox previousCheck;
    @FXML
    private Button queryButton;

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

        initializeRangeControls();
        initializePodCombo();
        initializeFilterControls();

        // Estado/errores e indicador de seguimiento o de consulta
        errorLabel.textProperty().bind(logsVM.errorProperty());
        logsVM.infoProperty().addListener((obs, old, info) -> infoLabel.setText(info == null ? "" : info));
        progressFollowing.visibleProperty().bind(logsVM.followingProperty().or(logsVM.loadingProperty()));
        btnToggleStream.textProperty().bind(
                Bindings.when(logsVM.followingProperty()).then("Detener").otherwise("Reanudar"));
    }

    private void initializeRangeControls() {
        rangeCombo.getItems().setAll(RANGE_LIVE, RANGE_15_MIN, RANGE_1_HOUR, RANGE_6_HOURS, RANGE_ALL, RANGE_CUSTOM);
        rangeCombo.setValue(RANGE_LIVE);

        var custom = rangeCombo.valueProperty().isEqualTo(RANGE_CUSTOM);
        var live = rangeCombo.valueProperty().isEqualTo(RANGE_LIVE);
        customRangeBox.visibleProperty().bind(custom);
        customRangeBox.managedProperty().bind(custom);
        queryButton.visibleProperty().bind(live.not());
        queryButton.managedProperty().bind(live.not());
        previousCheck.disableProperty().bind(live);
        btnToggleStream.disableProperty().bind(live.not());

        // Personalizado arranca en "la última hora" para que solo haya que ajustar
        LocalDateTime now = LocalDateTime.now();
        fromDate.setValue(now.minusHours(1).toLocalDate());
        fromTime.setText(now.minusHours(1).format(TIME_FORMAT));
        toDate.setValue(now.toLocalDate());
        toTime.setText(now.format(TIME_FORMAT));

        rangeCombo.valueProperty().addListener((obs, old, range) -> {
            if (RANGE_LIVE.equals(range)) {
                previousCheck.setSelected(false);
            }
            if (!RANGE_CUSTOM.equals(range)) {
                reload(); // en Personalizado se espera a que el usuario ajuste fechas y pulse Consultar
            }
        });
        previousCheck.selectedProperty().addListener((obs, old, selected) -> {
            if (!isLive()) {
                reload();
            }
        });
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
            reload();
        });

        // Al cambiar de container (p. ej. ver istio-proxy) se reabre el stream del mismo pod
        containerCombo.valueProperty().addListener((obs, old, container) -> {
            if (!updatingContainers && container != null) {
                reload();
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
        reload();
    }

    /**
     * Vuelve a cargar según el rango elegido: stream en vivo o consulta histórica del pod/container actual.
     */
    private void reload() {
        if (isLive()) {
            startLive();
        } else {
            runRangeQuery();
        }
    }

    private boolean isLive() {
        return RANGE_LIVE.equals(rangeCombo.getValue());
    }

    /**
     * Abre el stream del pod y container seleccionados (si ya hay contexto y pod).
     */
    private void startLive() {
        Pod pod = podCombo.getSelectionModel().getSelectedItem();
        if (pod == null || ctx == null) {
            return;
        }
        String container = containerCombo.getValue();
        infoLabel.setText("");
        logsVM.clear();
        logsVM.startFollowing(
                ctx.kubeConfig(), ctx.awsCredentials(),
                ctx.clusterName(), ctx.namespace(), ctx.deploymentName(),
                pod.getName(),
                container, // null solo si el pod no reporta containers: la API usa el único que haya
                true,      // timestamps: hora real del pod (kubelet) en vez de la hora de recepción
                200,       // tail inicial para contexto
                null       // sinceSeconds
        );
        log.info("Siguiendo logs de pod {} container {}", pod.getName(), container);
    }

    private void runRangeQuery() {
        Pod pod = podCombo.getSelectionModel().getSelectedItem();
        if (pod == null || ctx == null) {
            return;
        }
        LogRange range;
        try {
            range = buildRange();
        } catch (IllegalArgumentException e) {
            logsVM.clearMessages(); // que no quede el error de una consulta anterior junto a la validación
            infoLabel.setText(e.getMessage());
            return;
        }
        log.info("Consultando logs de pod {} container {} rango {}", pod.getName(), containerCombo.getValue(), range);
        logsVM.loadRange(ctx.kubeConfig(), ctx.awsCredentials(), ctx.namespace(), pod.getName(),
                         containerCombo.getValue(), range);
    }

    private LogRange buildRange() {
        boolean previous = previousCheck.isSelected();
        Instant now = Instant.now();
        return switch (rangeCombo.getValue()) {
            case RANGE_15_MIN -> new LogRange(now.minus(java.time.Duration.ofMinutes(15)), null, previous);
            case RANGE_1_HOUR -> new LogRange(now.minus(java.time.Duration.ofHours(1)), null, previous);
            case RANGE_6_HOURS -> new LogRange(now.minus(java.time.Duration.ofHours(6)), null, previous);
            case RANGE_ALL -> new LogRange(null, null, previous);
            case RANGE_CUSTOM -> {
                Instant since = toInstant(fromDate.getValue(), fromTime.getText(), "Desde");
                Instant until = toInstant(toDate.getValue(), toTime.getText(), "Hasta");
                if (!since.isBefore(until)) {
                    throw new IllegalArgumentException("\"Desde\" debe ser anterior a \"Hasta\".");
                }
                yield new LogRange(since, until, previous);
            }
            default -> throw new IllegalStateException("Rango desconocido: " + rangeCombo.getValue());
        };
    }

    /**
     * Fecha + hora "HH:mm" en la zona horaria del equipo.
     */
    private static Instant toInstant(LocalDate date, String time, String field) {
        if (date == null) {
            throw new IllegalArgumentException("Falta la fecha de \"" + field + "\".");
        }
        try {
            LocalTime localTime = LocalTime.parse(time.trim(), TIME_FORMAT);
            return date.atTime(localTime).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Hora inválida en \"" + field + "\": usa HH:mm (p. ej. 09:30).");
        }
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
        startLive();
    }

    @FXML
    private void onQueryClicked() {
        runRangeQuery();
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
