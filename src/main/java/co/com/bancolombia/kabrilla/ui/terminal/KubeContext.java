package co.com.bancolombia.kabrilla.ui.terminal;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Contexto actual de kubectl (lo que usaría {@code kubectl} en una terminal): cluster y namespace por defecto.
 *
 * @param cluster   nombre corto del cluster (sin el ARN de EKS), o {@code null} si no hay contexto
 * @param namespace namespace por defecto del contexto, o {@code null} (kubectl usa "default")
 * @param problem   por qué no se pudo determinar, o {@code null}
 */
public record KubeContext(String cluster, String namespace, String problem) {

    /**
     * Kubeconfig que usará la terminal: el mismo orden que kubectl y que Kabrilla.
     *
     * @param configured {@code kabrilla.kubeconfig} (puede ser vacío)
     * @param envValue   variable {@code KUBECONFIG} (puede ser {@code null}); si trae varios archivos, el primero
     */
    public static Path kubeconfigPath(String configured, String envValue, Path home) {
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim());
        }
        if (envValue != null && !envValue.isBlank()) {
            return Path.of(envValue.split(File.pathSeparator)[0].trim());
        }
        return home.resolve(".kube").resolve("config");
    }

    public static KubeContext read(Path kubeconfig) {
        if (!Files.isRegularFile(kubeconfig)) {
            return new KubeContext(null, null, "sin kubeconfig (" + kubeconfig + ")");
        }
        try (Reader reader = Files.newBufferedReader(kubeconfig, StandardCharsets.UTF_8)) {
            Object document = new Yaml().load(reader);
            if (!(document instanceof Map<?, ?> config)) {
                return new KubeContext(null, null, "kubeconfig vacío");
            }
            Object current = config.get("current-context");
            if (current == null || current.toString().isBlank()) {
                return new KubeContext(null, null, "sin contexto actual");
            }
            String contextName = current.toString();
            Map<?, ?> context = findContext(config.get("contexts"), contextName);
            String cluster = context != null && context.get("cluster") != null
                    ? context.get("cluster").toString() : contextName;
            Object namespace = context != null ? context.get("namespace") : null;
            return new KubeContext(shortName(cluster), namespace != null ? namespace.toString() : null, null);
        } catch (IOException | RuntimeException e) {
            return new KubeContext(null, null, "no se pudo leer el kubeconfig: " + e.getMessage());
        }
    }

    /**
     * Texto para la UI: "eks-ecosistemas-dev · ns pagos-qa".
     */
    public String describe() {
        if (cluster == null) {
            return problem;
        }
        return namespace == null ? cluster : cluster + " · ns " + namespace;
    }

    private static Map<?, ?> findContext(Object contexts, String name) {
        if (!(contexts instanceof List<?> list)) {
            return null;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> entry && name.equals(String.valueOf(entry.get("name")))
                    && entry.get("context") instanceof Map<?, ?> context) {
                return context;
            }
        }
        return null;
    }

    /**
     * EKS nombra los clusters con su ARN (arn:aws:eks:región:cuenta:cluster/nombre): se muestra solo el nombre.
     */
    static String shortName(String cluster) {
        int slash = cluster.lastIndexOf("cluster/");
        return slash >= 0 ? cluster.substring(slash + "cluster/".length()) : cluster;
    }
}
