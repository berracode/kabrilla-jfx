package co.com.bancolombia.kabrilla.infrastructure.adapters.fs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración editable por el usuario: {@code <kabrilla.data-dir>/application.properties}.
 * <p>
 * Spring la importa (ver {@code spring.config.import} en el application.properties empaquetado) y sus valores
 * reemplazan los del paquete. Vive en la carpeta de datos y no junto al .exe para sobrevivir a actualizaciones de la
 * versión portable. Si no existe, se crea una plantilla con las propiedades comentadas; nunca se sobrescribe.
 */
@Slf4j
public final class UserConfigFile {

    public static final String FILE_NAME = "application.properties";

    static final String TEMPLATE = """
            # Configuración de Kabrilla. Descomenta (quita el #) y cambia lo que necesites; reinicia la app para aplicar.
            # Estos valores reemplazan los de la app. Si borras este archivo, se vuelve a crear esta plantilla.
            # Rutas de Windows con barras normales (C:/Users/...) o dobles (C:\\\\Users\\\\...).

            # Tema oscuro de Kabrilla. false = tema base de JavaFX.
            #kabrilla.ui.styles-enabled=true

            # Kubeconfig a usar. Vacío = variable KUBECONFIG (primer archivo) y si no ~/.kube/config.
            #kabrilla.kubeconfig=C:/Users/usuario/.kube/config

            # Shell de la terminal integrada (Ctrl+ñ). Rutas con espacios entre comillas dobles. Ejemplos:
            #   pwsh.exe -NoLogo      |  cmd.exe      |  "C:/Program Files/Git/bin/bash.exe" --login -i
            #kabrilla.terminal.shell=powershell.exe -NoLogo

            # Más detalle en el log (~/.kabrilla/logs/kabrilla.log) para diagnosticar un problema.
            #logging.level.co.com.bancolombia.kabrilla=DEBUG
            """;

    private final Path file;

    public UserConfigFile(Path dataDir) {
        this.file = dataDir.toAbsolutePath().resolve(FILE_NAME);
        if (Files.exists(file)) {
            log.info("Configuración del usuario: {}", file);
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, TEMPLATE, StandardCharsets.UTF_8);
            log.info("Creada la plantilla de configuración del usuario: {}", file);
        } catch (IOException e) {
            // La app funciona igual con los valores por defecto
            log.warn("No se pudo crear la plantilla de configuración {}", file, e);
        }
    }

    public Path path() {
        return file;
    }
}
