package co.com.bancolombia.kabrilla.infrastructure.adapters.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserConfigFileTest {

    @TempDir
    Path dataDir;

    @Test
    void createsCommentedTemplateOnFirstRun() throws Exception {
        UserConfigFile config = new UserConfigFile(dataDir.resolve("nueva"));

        String content = Files.readString(config.path(), StandardCharsets.UTF_8);
        assertEquals(UserConfigFile.TEMPLATE, content);
        // Plantilla sin valores activos: no cambia nada hasta que el usuario descomente
        assertTrue(content.lines().allMatch(line -> line.isBlank() || line.startsWith("#")), content);
    }

    @Test
    void neverOverwritesUserChanges() throws Exception {
        Path file = dataDir.resolve(UserConfigFile.FILE_NAME);
        Files.writeString(file, "kabrilla.ui.styles-enabled=false\n");

        new UserConfigFile(dataDir);

        assertEquals("kabrilla.ui.styles-enabled=false\n", Files.readString(file));
    }
}
