package co.com.bancolombia.kabrilla.domain.model.k8s.resources;

import java.util.List;
import java.util.Map;

/**
 * Vista resumida de un recurso para mostrar al usuario.
 *
 * @param kind    tipo de recurso
 * @param name    metadata.name
 * @param summary datos clave en orden de presentación (p. ej. "Tipo" → "ClusterIP")
 * @param entries filas de detalle: datos de un ConfigMap, métricas de un HPA, reglas de una política
 * @param yaml    manifiesto completo (sin managedFields), listo para copiar
 */
public record KubeResource(ResourceKind kind, String name, Map<String, String> summary, List<Entry> entries,
                           String yaml) {

    public record Entry(String key, String value) {
    }
}
