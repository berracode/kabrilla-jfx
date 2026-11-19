package co.com.bancolombia.kabrilla.ui.controller.commands;

import java.util.Map;

import co.com.bancolombia.kabrilla.domain.model.k8s.resources.KubeResource;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;
import co.com.bancolombia.kabrilla.ui.context.DeploymentContext;
import co.com.bancolombia.kabrilla.ui.controller.tabs.DeploymentTab;
import co.com.bancolombia.kabrilla.ui.services.ClipboardSupport;
import co.com.bancolombia.kabrilla.ui.viewmodel.resources.ResourcesViewModel;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Pestaña genérica para Service, HPA, ConfigMap y recursos de Istio (AuthorizationPolicy, RequestAuthentication,
 * VirtualService, Gateway).
 * Todo el texto es seleccionable/copiable; el YAML completo queda plegado con su botón de copiar.
 */
public class ResourcesContentController implements DeploymentTab {

    private static final int MAX_TEXT_ROWS = 25;

    private static final Map<ResourceKind, String> TITLES = Map.of(
            ResourceKind.SERVICE, "Services",
            ResourceKind.HPA, "HorizontalPodAutoscalers",
            ResourceKind.CONFIG_MAP, "ConfigMaps",
            ResourceKind.AUTHORIZATION_POLICY, "AuthorizationPolicies (Istio)",
            ResourceKind.REQUEST_AUTHENTICATION, "RequestAuthentications (Istio)",
            ResourceKind.VIRTUAL_SERVICE, "VirtualServices (Istio)",
            ResourceKind.GATEWAY, "Gateways (Istio)");

    private static final Map<ResourceKind, String> ENTRIES_TITLES = Map.of(
            ResourceKind.HPA, "Métricas",
            ResourceKind.CONFIG_MAP, "Datos",
            ResourceKind.AUTHORIZATION_POLICY, "Reglas",
            ResourceKind.REQUEST_AUTHENTICATION, "Reglas JWT",
            ResourceKind.VIRTUAL_SERVICE, "Rutas",
            ResourceKind.GATEWAY, "Servidores");

    // Cómo se asocian los recursos que no apuntan directo al deployment (se muestra si no hay resultados)
    private static final Map<ResourceKind, String> EMPTY_HINTS = Map.of(
            ResourceKind.VIRTUAL_SERVICE, " Se buscan los VirtualServices del namespace cuyas rutas apuntan a un "
                    + "Service del deployment.",
            ResourceKind.GATEWAY, " Se muestran los Gateways que usan sus VirtualServices (ver pestaña "
                    + "VirtualService).");

    private final ResourcesViewModel viewModel;
    private DeploymentContext ctx;

    @FXML
    private Label titleLabel;
    @FXML
    private Label countLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private VBox resourcesBox;

    public ResourcesContentController(ResourcesViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        titleLabel.setText(TITLES.get(viewModel.kind()));
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        viewModel.resources().addListener((ListChangeListener<KubeResource>) c -> render());
        viewModel.errorProperty().addListener((obs, old, error) -> render());
        viewModel.loadedProperty().addListener((obs, old, loaded) -> render());
        render();
    }

    public void setDeploymentContext(DeploymentContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Carga perezosa: se llama al seleccionar la pestaña, para no consultar 5 recursos al abrir cada deployment.
     */
    public void ensureLoaded() {
        if (ctx != null && !viewModel.loadedProperty().get()) {
            viewModel.load(ctx);
        }
    }

    @Override
    public void setDeploymentName(String name) {
        // El contexto completo llega por setDeploymentContext
    }

    @FXML
    private void onReloadClicked() {
        if (ctx != null) {
            viewModel.load(ctx);
        }
    }

    private void render() {
        resourcesBox.getChildren().clear();
        String error = viewModel.errorProperty().get();
        int count = viewModel.resources().size();
        countLabel.setText(viewModel.loadedProperty().get() && error == null ? "(" + count + ")" : "");

        if (error != null) {
            messageLabel.setText("⚠ " + error);
        } else if (!viewModel.loadedProperty().get()) {
            messageLabel.setText("Cargando...");
        } else if (count == 0) {
            messageLabel.setText("No hay " + TITLES.get(viewModel.kind()) + " asociados a "
                                         + (ctx != null ? ctx.deploymentName() : "este deployment") + "."
                                         + EMPTY_HINTS.getOrDefault(viewModel.kind(), ""));
        } else {
            messageLabel.setText("");
        }
        messageLabel.setManaged(!messageLabel.getText().isEmpty());

        boolean first = true;
        for (KubeResource resource : viewModel.resources()) {
            TitledPane pane = new TitledPane(resource.name(), buildResourceContent(resource));
            pane.setExpanded(first || count <= 3);
            resourcesBox.getChildren().add(pane);
            first = false;
        }
    }

    private Node buildResourceContent(KubeResource resource) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(8));

        Button copyYaml = new Button("Copiar YAML");
        copyYaml.setOnAction(e -> ClipboardSupport.copy(resource.yaml()));
        Button copyName = new Button("Copiar nombre");
        copyName.setOnAction(e -> ClipboardSupport.copy(resource.name()));
        HBox actions = new HBox(8, copyName, copyYaml);
        content.getChildren().add(actions);

        if (!resource.summary().isEmpty()) {
            GridPane summary = twoColumnGrid();
            int row = 0;
            for (var item : resource.summary().entrySet()) {
                summary.addRow(row++, keyLabel(item.getKey()), readOnlyField(item.getValue()));
            }
            content.getChildren().add(summary);
        }

        if (!resource.entries().isEmpty()) {
            Label entriesTitle = keyLabel(ENTRIES_TITLES.getOrDefault(resource.kind(), "Detalle"));
            GridPane entries = twoColumnGrid();
            entries.getColumnConstraints().add(new ColumnConstraints());
            int row = 0;
            for (KubeResource.Entry entry : resource.entries()) {
                Button copy = new Button("Copiar");
                copy.setOnAction(e -> ClipboardSupport.copy(entry.value()));
                Label key = keyLabel(entry.key());
                GridPane.setValignment(key, javafx.geometry.VPos.TOP);
                GridPane.setValignment(copy, javafx.geometry.VPos.TOP);
                entries.addRow(row++, key, valueNode(entry.value()), copy);
            }
            content.getChildren().addAll(entriesTitle, entries);
        }

        TitledPane yamlPane = new TitledPane("YAML", monospaceArea(resource.yaml()));
        yamlPane.setExpanded(false);
        content.getChildren().add(yamlPane);
        return content;
    }

    private static GridPane twoColumnGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        ColumnConstraints keyColumn = new ColumnConstraints();
        keyColumn.setMinWidth(140);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        valueColumn.setFillWidth(true);
        grid.getColumnConstraints().addAll(keyColumn, valueColumn);
        return grid;
    }

    private static Label keyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("kb-key");
        label.setAlignment(Pos.TOP_LEFT);
        return label;
    }

    /**
     * Valores de una línea en TextField de solo lectura (seleccionable); multilínea en área monoespaciada.
     */
    private static Node valueNode(String value) {
        return value != null && value.contains("\n") ? monospaceArea(value) : readOnlyField(value);
    }

    private static TextField readOnlyField(String value) {
        TextField field = new TextField(value);
        field.setEditable(false);
        field.setFocusTraversable(false);
        return field;
    }

    private static TextArea monospaceArea(String text) {
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefRowCount((int) Math.min(MAX_TEXT_ROWS, text.lines().count() + 1));
        area.getStyleClass().add("kb-mono");
        return area;
    }
}
