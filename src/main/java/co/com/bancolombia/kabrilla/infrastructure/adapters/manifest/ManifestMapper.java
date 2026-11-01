package co.com.bancolombia.kabrilla.infrastructure.adapters.manifest;

import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.list;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.map;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.name;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.str;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.stringMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import co.com.bancolombia.kabrilla.domain.model.k8s.resources.KubeResource;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.KubeResource.Entry;
import co.com.bancolombia.kabrilla.domain.model.k8s.resources.ResourceKind;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Convierte un manifiesto (Map) en {@link KubeResource}: resumen legible por tipo + YAML limpio para copiar.
 */
class ManifestMapper {

    private static final String LAST_APPLIED = "kubectl.kubernetes.io/last-applied-configuration";

    private final Yaml yaml = createYaml();

    KubeResource toResource(ResourceKind kind, Map<String, Object> manifest) {
        Map<String, String> summary = new LinkedHashMap<>();
        List<Entry> entries = new ArrayList<>();
        switch (kind) {
            case SERVICE -> mapService(manifest, summary);
            case HPA -> mapHpa(manifest, summary, entries);
            case CONFIG_MAP -> mapConfigMap(manifest, summary, entries);
            case AUTHORIZATION_POLICY -> mapAuthorizationPolicy(manifest, summary, entries);
            case REQUEST_AUTHENTICATION -> mapRequestAuthentication(manifest, summary, entries);
        }
        return new KubeResource(kind, name(manifest), summary, entries, yaml.dump(clean(manifest)));
    }

    private void mapService(Map<String, Object> svc, Map<String, String> summary) {
        summary.put("Tipo", Objects.requireNonNullElse(str(svc, "spec", "type"), "ClusterIP"));
        put(summary, "Cluster IP", str(svc, "spec", "clusterIP"));
        summary.put("Puertos", list(svc, "spec", "ports").stream().map(this::describePort)
                .collect(Collectors.joining(", ")));
        summary.put("Selector", joinLabels(stringMap(svc, "spec", "selector")));
        String ingress = list(svc, "status", "loadBalancer", "ingress").stream()
                .map(i -> Objects.requireNonNullElse(str(i, "hostname"), str(i, "ip")))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        put(summary, "Load balancer", ingress);
    }

    private String describePort(Object port) {
        // "http 80/TCP → 8080 (nodePort 30080)"
        StringBuilder sb = new StringBuilder();
        if (str(port, "name") != null) {
            sb.append(str(port, "name")).append(' ');
        }
        sb.append(str(port, "port")).append('/').append(Objects.requireNonNullElse(str(port, "protocol"), "TCP"));
        if (str(port, "targetPort") != null) {
            sb.append(" → ").append(str(port, "targetPort"));
        }
        if (str(port, "nodePort") != null) {
            sb.append(" (nodePort ").append(str(port, "nodePort")).append(')');
        }
        return sb.toString();
    }

    private void mapHpa(Map<String, Object> hpa, Map<String, String> summary, List<Entry> entries) {
        summary.put("Objetivo", str(hpa, "spec", "scaleTargetRef", "kind") + "/"
                + str(hpa, "spec", "scaleTargetRef", "name"));
        summary.put("Réplicas mín – máx", Objects.requireNonNullElse(str(hpa, "spec", "minReplicas"), "1") + " – "
                + str(hpa, "spec", "maxReplicas"));
        put(summary, "Réplicas actuales", str(hpa, "status", "currentReplicas"));
        put(summary, "Réplicas deseadas", str(hpa, "status", "desiredReplicas"));

        List<Object> current = list(hpa, "status", "currentMetrics");
        for (Object metric : list(hpa, "spec", "metrics")) {
            String type = str(metric, "type");
            if (!"Resource".equals(type)) {
                entries.add(new Entry(type, "Métrica personalizada: ver YAML"));
                continue;
            }
            String resource = str(metric, "resource", "name");
            Object currentMetric = current.stream()
                    .filter(c -> resource.equals(str(c, "resource", "name")))
                    .findFirst().orElse(null);
            entries.add(new Entry(resource, "actual " + metricValue(currentMetric, "current")
                    + " · objetivo " + metricValue(metric, "target")));
        }
    }

    private String metricValue(Object metric, String field) {
        if (metric == null) {
            return "—";
        }
        String utilization = str(metric, "resource", field, "averageUtilization");
        if (utilization != null) {
            return utilization + "%";
        }
        return Objects.requireNonNullElse(str(metric, "resource", field, "averageValue"), "—");
    }

    private void mapConfigMap(Map<String, Object> cm, Map<String, String> summary, List<Entry> entries) {
        Map<String, String> data = stringMap(cm, "data");
        summary.put("Claves", String.valueOf(data.size()));
        data.forEach((k, v) -> entries.add(new Entry(k, v)));
    }

    private void mapAuthorizationPolicy(Map<String, Object> policy, Map<String, String> summary,
                                        List<Entry> entries) {
        summary.put("Acción", Objects.requireNonNullElse(str(policy, "spec", "action"), "ALLOW"));
        summary.put("Aplica a", appliesTo(policy));
        List<Object> rules = list(policy, "spec", "rules");
        if (rules.isEmpty()) {
            entries.add(new Entry("Reglas", "Sin reglas"));
        }
        for (int i = 0; i < rules.size(); i++) {
            Object rule = rules.get(i);
            List<String> parts = new ArrayList<>();
            list(rule, "from").forEach(f -> parts.add("Desde: " + flatten(map(f, "source"))));
            list(rule, "to").forEach(t -> parts.add("Hacia: " + flatten(map(t, "operation"))));
            list(rule, "when").forEach(w -> parts.add("Cuando: " + flatten(w)));
            entries.add(new Entry("Regla " + (i + 1), parts.isEmpty() ? "Cualquier request" : String.join("\n", parts)));
        }
    }

    private void mapRequestAuthentication(Map<String, Object> ra, Map<String, String> summary, List<Entry> entries) {
        summary.put("Aplica a", appliesTo(ra));
        List<Object> jwtRules = list(ra, "spec", "jwtRules");
        for (int i = 0; i < jwtRules.size(); i++) {
            List<String> lines = new ArrayList<>();
            map(jwtRules.get(i)).forEach((k, v) -> lines.add(k + ": " + flatten(v)));
            entries.add(new Entry("JWT " + (i + 1), String.join("\n", lines)));
        }
    }

    private String appliesTo(Map<String, Object> policy) {
        Map<String, String> selector = stringMap(policy, "spec", "selector", "matchLabels");
        return selector.isEmpty() ? "Todo el namespace" : joinLabels(selector);
    }

    private static String joinLabels(Map<String, String> labels) {
        return labels.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    /**
     * Representación compacta de una estructura YAML: {a: [x, y], b: z} → "a: x, y; b: z".
     */
    private static String flatten(Object value) {
        if (value instanceof Map<?, ?> m) {
            return m.entrySet().stream().map(e -> e.getKey() + ": " + flatten(e.getValue()))
                    .collect(Collectors.joining("; "));
        }
        if (value instanceof List<?> l) {
            return l.stream().map(ManifestMapper::flatten).collect(Collectors.joining(", "));
        }
        return String.valueOf(value);
    }

    private static void put(Map<String, String> summary, String key, String value) {
        if (value != null && !value.isBlank()) {
            summary.put(key, value);
        }
    }

    /**
     * Quita ruido que no aporta al leer/copiar: managedFields y la anotación last-applied-configuration.
     */
    private static Map<String, Object> clean(Map<String, Object> manifest) {
        Map<String, Object> copy = new LinkedHashMap<>(manifest);
        Map<String, Object> metadata = new LinkedHashMap<>(map(manifest, "metadata"));
        metadata.remove("managedFields");
        Map<String, Object> annotations = new LinkedHashMap<>(map(metadata, "annotations"));
        annotations.remove(LAST_APPLIED);
        if (annotations.isEmpty()) {
            metadata.remove("annotations");
        } else {
            metadata.put("annotations", annotations);
        }
        copy.put("metadata", metadata);
        return copy;
    }

    private static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setIndicatorIndent(2);
        options.setIndentWithIndicator(true);
        options.setWidth(160);
        return new Yaml(new LiteralStringRepresenter(options), options);
    }

    /**
     * Strings multilínea (archivos dentro de un ConfigMap) como bloque literal "|" en vez de "...\n...".
     */
    private static final class LiteralStringRepresenter extends Representer {
        LiteralStringRepresenter(DumperOptions options) {
            super(options);
            this.representers.put(String.class, data -> {
                String text = (String) data;
                return text.contains("\n")
                        ? representScalar(Tag.STR, text, DumperOptions.ScalarStyle.LITERAL)
                        : representScalar(Tag.STR, text);
            });
        }
    }
}
