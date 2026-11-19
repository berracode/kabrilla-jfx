package co.com.bancolombia.kabrilla.ui.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import co.com.bancolombia.kabrilla.infrastructure.adapters.fs.UserConfigFile;
import co.com.bancolombia.kabrilla.ui.services.ErrorMessages;
import co.com.bancolombia.kabrilla.ui.viewmodel.cluster.SidebarStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.LayoutState;
import co.com.bancolombia.kabrilla.ui.viewmodel.terminal.TerminalPanelViewModel;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Menú superior (Archivo, Ver, Terminal, Ayuda). Reúne las acciones que antes eran botones del panel izquierdo.
 */
@Slf4j
@Component
public class AppMenuController {

    private final SidebarStateViewModel sidebar;
    private final TerminalPanelViewModel terminal;
    private final LayoutState layout;
    // Solo existe si la app arrancó por JavaFxApplication (no en tests)
    private final ObjectProvider<HostServices> hostServices;
    private final UserConfigFile userConfig;
    private final String version;
    private final Path dataDir;
    private final String releasesUrl;

    @FXML
    private CheckMenuItem sidebarItem;
    @FXML
    private CheckMenuItem terminalPanelItem;
    @FXML
    private MenuItem toggleTerminalItem;

    public AppMenuController(SidebarStateViewModel sidebar, TerminalPanelViewModel terminal, LayoutState layout,
                             ObjectProvider<HostServices> hostServices, UserConfigFile userConfig,
                             @Value("${kabrilla.version:dev}") String version,
                             @Value("${kabrilla.data-dir}") String dataDir,
                             @Value("${kabrilla.releases-url:}") String releasesUrl) {
        this.sidebar = sidebar;
        this.terminal = terminal;
        this.layout = layout;
        this.hostServices = hostServices;
        this.userConfig = userConfig;
        this.version = version;
        this.dataDir = Path.of(dataDir).toAbsolutePath();
        this.releasesUrl = releasesUrl;
    }

    @FXML
    public void initialize() {
        sidebarItem.selectedProperty().bindBidirectional(layout.sidebarVisibleProperty());
        terminalPanelItem.selectedProperty().bindBidirectional(terminal.visibleProperty());
        // Solo se muestra: el atajo lo atiende MainController (también funciona con el foco en la terminal)
        toggleTerminalItem.setAccelerator(new KeyCharacterCombination("ñ", KeyCombination.SHORTCUT_DOWN));
    }

    @FXML
    private void onAddNamespace() {
        sidebar.openNamespaceDialog();
    }

    @FXML
    private void onCredentials() {
        sidebar.openAddCredentialsTab();
    }

    /**
     * El .properties no suele tener programa asociado en Windows: se abre con el Bloc de notas.
     */
    @FXML
    private void onOpenConfig() {
        try {
            new ProcessBuilder("notepad.exe", userConfig.path().toString()).start();
        } catch (IOException e) {
            showError("No se pudo abrir " + userConfig.path(), e);
        }
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onNewTerminal() {
        terminal.requestNewTerminal();
    }

    @FXML
    private void onToggleTerminal() {
        terminal.toggle();
    }

    @FXML
    private void onOpenLogs() {
        Path logs = dataDir.resolve("logs");
        try {
            Files.createDirectories(logs);
            showDocument(logs.toUri().toString());
        } catch (IOException | RuntimeException e) {
            showError("No se pudo abrir " + logs, e);
        }
    }

    @FXML
    private void onReleaseNotes() {
        if (releasesUrl.isBlank()) {
            return;
        }
        // Versión de un release (1.1.0, 1.1.0-rc.1): su página; en desarrollo, la lista de releases
        boolean released = version.matches("\\d+\\.\\d+\\.\\d+(-[\\w.]+)?") && !version.contains("SNAPSHOT");
        showDocument(released ? releasesUrl + "/tag/v" + version : releasesUrl);
    }

    private void showDocument(String uri) {
        hostServices.ifAvailable(services -> services.showDocument(uri));
    }

    @FXML
    private void onAbout() {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("Acerca de Kabrilla");
        about.setHeaderText("Kabrilla " + version);
        about.setContentText(String.join("\n",
                                         "Cliente de escritorio para ver y filtrar logs de Kubernetes (EKS).",
                                         "",
                                         "Usuario: " + System.getProperty("user.name"),
                                         "Datos y configuración: " + dataDir,
                                         "Kubeconfig de la terminal: " + terminal.kubeconfig(),
                                         "Java " + System.getProperty("java.version")));
        about.show();
    }

    private static void showError(String header, Exception e) {
        log.error(header, e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Kabrilla");
        alert.setHeaderText(header);
        alert.setContentText(ErrorMessages.describe(e));
        alert.show();
    }
}
