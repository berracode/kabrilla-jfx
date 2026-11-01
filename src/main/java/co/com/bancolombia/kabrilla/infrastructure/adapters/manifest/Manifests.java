package co.com.bancolombia.kabrilla.infrastructure.adapters.manifest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Navegación segura por manifiestos representados como Map (lo que entrega la API o un YAML parseado).
 * Las rutas inexistentes devuelven vacío/null en vez de lanzar excepción.
 */
public final class Manifests {

    private Manifests() {
    }

    public static Object get(Object node, String... path) {
        Object current = node;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }

    public static String str(Object node, String... path) {
        Object value = get(node, path);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object node, String... path) {
        return get(node, path) instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object node, String... path) {
        return get(node, path) instanceof List<?> list ? (List<Object>) list : List.of();
    }

    public static Map<String, String> stringMap(Object node, String... path) {
        Map<String, String> result = new LinkedHashMap<>();
        map(node, path).forEach((k, v) -> result.put(k, String.valueOf(v)));
        return result;
    }

    public static String name(Object manifest) {
        return str(manifest, "metadata", "name");
    }
}
