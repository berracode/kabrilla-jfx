package co.com.bancolombia.kabrilla.infrastructure.adapters.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDataDirectoryTest {

    @TempDir
    Path tmp;

    @Test
    void createsDirectoryAndMigratesLegacyFilesWithNewNames() throws Exception {
        Path legacy = Files.createDirectories(tmp.resolve("workdir"));
        Files.writeString(legacy.resolve("clusters.json"), "[clusters]");
        Files.writeString(legacy.resolve("credentials.2.json"), "[creds]");

        LocalDataDirectory data = new LocalDataDirectory(tmp.resolve(".kabrilla"), legacy);

        assertEquals("[clusters]", Files.readString(data.resolve(LocalDataDirectory.CLUSTERS_FILE)));
        assertEquals("[creds]", Files.readString(data.resolve(LocalDataDirectory.CREDENTIALS_FILE)));
        assertTrue(Files.exists(legacy.resolve("credentials.2.json")), "Se copia, no se mueve");
    }

    @Test
    void neverOverwritesExistingData() throws Exception {
        Path legacy = Files.createDirectories(tmp.resolve("workdir"));
        Files.writeString(legacy.resolve("credentials.2.json"), "[viejas]");
        Path dataDir = Files.createDirectories(tmp.resolve(".kabrilla"));
        Files.writeString(dataDir.resolve(LocalDataDirectory.CREDENTIALS_FILE), "[actuales]");

        new LocalDataDirectory(dataDir, legacy);

        assertEquals("[actuales]", Files.readString(dataDir.resolve(LocalDataDirectory.CREDENTIALS_FILE)));
    }
}
