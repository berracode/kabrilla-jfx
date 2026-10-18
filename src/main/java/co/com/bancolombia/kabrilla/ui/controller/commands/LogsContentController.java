package co.com.bancolombia.kabrilla.ui.controller.commands;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;
import co.com.bancolombia.kabrilla.ui.context.DeploymentContext;
import co.com.bancolombia.kabrilla.ui.controller.tabs.DeploymentTab;
import co.com.bancolombia.kabrilla.ui.viewmodel.logs.LogsViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.pods.PodsViewModel;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.Paragraph;
import org.reactfx.collection.LiveList;


@Slf4j
public class LogsContentController implements DeploymentTab {

    private static final int MAX_VISIBLE_PARAGRAPHS = 5000;

    private final PodsViewModel podsVM;   // ViewModel compartido de Pods
    private final LogsViewModel logsVM;   // ViewModel dedicado a Logs

    @FXML
    private ComboBox<Pod> podCombo;
    //@FXML
    //private TableView<LogLine> logsTable;
    //@FXML
    //private TableColumn<LogLine, String> colLinea;
    @FXML
    private CheckBox autoScrollCheck;
    @FXML
    private Label errorLabel;
    @FXML
    private ProgressIndicator progressFollowing;

    @FXML
    private Button btnStart;
    @FXML
    private Button btnStop;
    @FXML
    private InlineCssTextArea txtLogArea;
    @FXML
    private TextField txtSearch;


    private DeploymentContext ctx;


    private Pod pendingPodSelection; // NUEVO


    public LogsContentController(PodsViewModel podsVM, LogsViewModel logsVM) {
        this.podsVM = podsVM;
        this.logsVM = logsVM;
    }


    @FXML
    public void initialize() {

        //iniciando text area
        // 2. Hacer el área de texto de RichTextFX de solo lectura
        txtLogArea.setEditable(false);


        // 3. Registrar el Consumer (callback) en el ViewModel
        logsVM.setOnLinesAppended(textBatch -> {
            // 1. Insertar el nuevo lote de logs al final
            txtLogArea.appendText(textBatch);

            // 2. Verificar si excedimos el límite máximo de párrafos permitidos
            LiveList<Paragraph<String, String, String>> currentParagraphsLiveList = txtLogArea.getParagraphs();
            var currentParagraphs = (long) currentParagraphsLiveList.size();
            log.info("Cantidad de lineas {}", currentParagraphs);

            if (currentParagraphs > MAX_VISIBLE_PARAGRAPHS) {
                int linesToRemove = Math.toIntExact(currentParagraphs - MAX_VISIBLE_PARAGRAPHS);
                log.info("Se procede a limpiar lineas de UI {}", linesToRemove);

                int endPosition = 0;
                for (int i = 0; i < linesToRemove; i++) {
                    endPosition += txtLogArea.getParagraph(i).length() + 1;
                }

                if (endPosition > 0 && endPosition <= txtLogArea.getLength()) {
                    // 1. Borramos el bloque excedido arriba
                    txtLogArea.deleteText(0, endPosition);

                    // 2. Ejecutamos el ajuste en el siguiente pulso del hilo de JavaFX
                    Platform.runLater(() -> {
                        // Forzamos al caret a ir al final absoluto del documento actual
                        txtLogArea.moveTo(txtLogArea.getLength());
                        // Le pedimos al visor virtualizado que baje hasta la posición del caret
                        txtLogArea.requestFollowCaret();
                    });
                }
            } else {
                // Si no está limpiando (bajada normal de logs), mantenemos el comportamiento base
                txtLogArea.requestFollowCaret();
            }

            // 3. Mantener el scroll al fondo
            txtLogArea.requestFollowCaret();
        });


        // Combo de pods
        podCombo.setItems(podsVM.pods());
        podCombo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Pod item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        podCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Pod item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Tabla: una sola columna con el texto del log
        // colLinea.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().text()));
        //logsTable.setItems(logsVM.getLines());
        //logsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        //logsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Estado/errores e indicador de seguimiento
        if (errorLabel != null) {
            errorLabel.textProperty().bind(logsVM.errorProperty());
        }
        if (progressFollowing != null) {
            progressFollowing.visibleProperty().bind(logsVM.followingProperty());
        }

        // Seleccionar el primero cuando lleguen pods
        podsVM.pods().addListener((ListChangeListener<Pod>) c -> {
            if (!podsVM.pods().isEmpty() && podCombo.getSelectionModel().isEmpty()) {
                podCombo.getSelectionModel().selectFirst();
            }
        });

        // (Re)abrir stream al cambiar pod seleccionado

        podCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldPod, newPod) -> {
            log.info("Cambio selección ComboBox: {}", newPod != null ? newPod.getName() : "null");
            if (newPod == null) {
                logsVM.stop();
                return;
            }
            if (ctx == null) {
                // Aún no han llamado setDeploymentContext: memoriza selección
                pendingPodSelection = newPod;
                log.info("Contexto aún no está listo; guardo selección pendiente: {}", newPod.getName());
                return;
            }
            startStreamFor(newPod);
        });
    }

    /**
     * Llamado por el padre (DeploymentTabController) al crear la vista.
     */
    public void setDeploymentContext(DeploymentContext ctx) {
        this.ctx = ctx;
        log.info("setDeploymentContext recibido: cluster={} ns={} deploy={}",
                 ctx.clusterName(), ctx.namespace(), ctx.deploymentName());

        // Si ya hay selección pendiente, arranca con esa
        if (pendingPodSelection != null) {
            Pod sel = pendingPodSelection;
            pendingPodSelection = null;
            startStreamFor(sel);
            return;
        }

        // Si no hay pendiente pero ya hay algo seleccionado, arranca
        Pod selected = podCombo.getSelectionModel().getSelectedItem();
        if (selected != null) {
            startStreamFor(selected);
        }
    }


    private void startStreamFor(Pod pod) {
        logsVM.clear();
        var credentials = AwsCredentials.builder().accessKey(ctx.credentials().accessKey())
                .secretKey(ctx.credentials().secretKey())
                .clusterName(ctx.credentials().clusterName())
                .sessionToken(ctx.credentials().sessionToken()).build();
        logsVM.startFollowing(
                ctx.kubeConfig(), credentials,
                ctx.clusterName(), ctx.namespace(), ctx.deploymentName(),
                pod.getName(),
                null,   // container: si tienes multi-container, pásalo desde la UI
                false,  // timestamps: si tu cluster los agrega en el payload y quieres mostrarlos
                200,    // tail inicial para contexto
                null    // sinceSeconds
        );
        log.info("Siguiendo logs de pod {}", pod.getName());
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

    // (Opcional) acciones de botones:
    @FXML
    private void onClearClicked() {
        logsVM.clear();
    }

    @FXML
    private void onStopClicked() {
        logsVM.stop();
    }


    @FXML
    private void onRefreshPodsClicked() {
        // Si implementaste ensureLoaded(ctx) en PodsViewModel: evita recargas innecesarias.
        // podsVM.reload(ctx); // o podsVM.ensureLoaded(ctx) si detecta cambios
        // Por ahora, si ya tienes un método loadPods(ctx):
        if (ctx != null) {
            podsVM.loadPods(ctx);
        }
    }

}
