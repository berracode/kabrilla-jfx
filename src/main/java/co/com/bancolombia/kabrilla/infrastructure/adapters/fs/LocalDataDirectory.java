package co.com.bancolombia.kabrilla.infrastructure.adapters.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Carpeta de datos local del usuario (por defecto {@code ~/.kabrilla}): namespaces registrados y credenciales.
 * <p>
 * Va en el perfil del usuario (no junto al .exe ni en el proyecto) para que sobreviva a actualizaciones de la app
 * y quede protegida como {@code ~/.aws/credentials}: en Windows el perfil solo es accesible por su dueño; en
 * Linux/macOS la carpeta se deja con permisos 700.
 */
@Slf4j
public final class LocalDataDirectory {

    public static final String CLUSTERS_FILE = "clusters.json";
    public static final String CREDENTIALS_FILE = "credentials.json";

    // Archivos que versiones anteriores guardaban en el directorio de trabajo → nombre nuevo
    private static final Map<String, String> LEGACY_FILES = new LinkedHashMap<>();

    static {
        LEGACY_FILES.put("clusters.json", CLUSTERS_FILE);
        LEGACY_FILES.put("credentials.2.json", CREDENTIALS_FILE);
    }

    private final Path dir;

    /**
     * @param dir       carpeta de datos (se crea si no existe)
     * @param legacyDir donde buscar archivos de versiones anteriores (el directorio de trabajo)
     */
    public LocalDataDirectory(Path dir, Path legacyDir) {
        this.dir = dir.toAbsolutePath();
        try {
            Files.createDirectories(this.dir);
            restrictToOwner(this.dir);
            migrateLegacyFiles(legacyDir.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo preparar la carpeta de datos " + this.dir, e);
        }
        log.info("Carpeta de datos: {}", this.dir);
    }

    public Path resolve(String fileName) {
        return dir.resolve(fileName);
    }

    public Path path() {
        return dir;
    }

    /**
     * Copia (no mueve) los archivos antiguos solo si en la carpeta nueva aún no existen: nunca sobrescribe.
     */
    private void migrateLegacyFiles(Path legacyDir) throws IOException {
        if (legacyDir.equals(dir)) {
            return;
        }
        for (var legacy : LEGACY_FILES.entrySet()) {
            Path source = legacyDir.resolve(legacy.getKey());
            Path target = dir.resolve(legacy.getValue());
            if (Files.isRegularFile(source) && !Files.exists(target)) {
                Files.copy(source, target);
                log.warn("Migrado {} -> {}. El archivo anterior ya no se usa; puedes borrarlo.", source, target);
            }
        }
    }

    private static void restrictToOwner(Path path) throws IOException {
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
        }
    }
}
