package co.com.bancolombia.kabrilla.infrastructure.adapters.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import co.com.bancolombia.kabrilla.domain.model.KubeConfig;

/**
 * Utilidades compartidas por los adaptadores mock.
 * Estructura esperada: {root}/{cluster}/{namespace}/{deployment}/{pod}.log
 */
final class MockFiles {

    static final List<String> LOG_EXTENSIONS = List.of(".log", ".txt");

    private MockFiles() {
    }

    /**
     * El kubeconfig usa ARNs (arn:aws:eks:...:cluster/mock-qa); en disco solo se usa el nombre corto.
     */
    static String shortClusterName(KubeConfig kubeConfig) {
        if (kubeConfig == null || kubeConfig.getClusterName() == null) {
            throw new IllegalStateException("No hay kubeconfig para el cluster; revisa mock-data/kubeconfig.yaml");
        }
        String name = kubeConfig.getClusterName();
        return name.substring(name.lastIndexOf('/') + 1);
    }

    static boolean isLogFile(Path path) {
        String name = path.getFileName().toString();
        return Files.isRegularFile(path) && LOG_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    static String podName(Path logFile) {
        String name = logFile.getFileName().toString();
        return name.substring(0, name.lastIndexOf('.'));
    }

    static List<Path> list(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.sorted().toList();
        }
    }
}
