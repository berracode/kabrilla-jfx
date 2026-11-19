package co.com.bancolombia.kabrilla.infrastructure.adapters.manifest;

import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.list;
import static co.com.bancolombia.kabrilla.infrastructure.adapters.manifest.Manifests.str;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lectura de VirtualServices de Istio (networking.istio.io): a qué Services enrutan y por qué Gateways entran.
 */
final class IstioRoutes {

    /**
     * Tipos de ruta de un VirtualService; todos tienen {@code route[].destination.host}.
     */
    static final List<String> ROUTE_TYPES = List.of("http", "tcp", "tls");
    /**
     * Gateway reservado de Istio: tráfico interno entre sidecars, no es un recurso Gateway.
     */
    static final String MESH = "mesh";

    private IstioRoutes() {
    }

    /**
     * Gateway referenciado desde {@code spec.gateways} de un VirtualService.
     */
    record GatewayRef(String namespace, String name) {
        String qualifiedName() {
            return namespace + "/" + name;
        }
    }

    /**
     * Nombre corto del Service al que apunta un host, si está en {@code namespace}; si no, {@code null}.
     * Acepta las formas de Kubernetes: "svc", "svc.ns", "svc.ns.svc" y "svc.ns.svc.cluster.local".
     */
    static String serviceOfHost(String host, String namespace) {
        if (host == null || host.isBlank() || host.contains("*")) {
            return null;
        }
        String h = host.trim();
        for (String suffix : List.of(".svc.cluster.local", ".svc")) {
            if (h.endsWith(suffix)) {
                h = h.substring(0, h.length() - suffix.length());
                break;
            }
        }
        String[] parts = h.split("\\.");
        if (parts.length == 1) {
            return parts[0];
        }
        if (parts.length == 2 && parts[1].equals(namespace)) {
            return parts[0];
        }
        return null; // otro namespace o un host externo (p. ej. api.example.com)
    }

    /**
     * Hosts de destino de todas las rutas (http, tcp, tls) del VirtualService.
     */
    static List<String> destinationHosts(Map<String, Object> virtualService) {
        List<String> hosts = new ArrayList<>();
        for (String type : ROUTE_TYPES) {
            for (Object route : list(virtualService, "spec", type)) {
                for (Object destination : list(route, "route")) {
                    String host = str(destination, "destination", "host");
                    if (host != null) {
                        hosts.add(host);
                    }
                }
            }
        }
        return hosts;
    }

    /**
     * Services de {@code services} a los que enruta el VirtualService (vacío si no enruta a ninguno).
     */
    static Set<String> routedServices(Map<String, Object> virtualService, Set<String> services, String namespace) {
        return destinationHosts(virtualService).stream()
                .map(host -> serviceOfHost(host, namespace))
                .filter(Objects::nonNull)
                .filter(services::contains)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
    }

    /**
     * Gateways del VirtualService, sin "mesh". Sin namespace explícito, el Gateway está en el del VirtualService.
     * Formas: "nombre", "namespace/nombre" o el FQDN "nombre.namespace.svc.cluster.local".
     */
    static List<GatewayRef> gatewayRefs(Map<String, Object> virtualService, String defaultNamespace) {
        String vsNamespace = Objects.requireNonNullElse(str(virtualService, "metadata", "namespace"),
                                                        defaultNamespace);
        List<GatewayRef> refs = new ArrayList<>();
        for (Object entry : list(virtualService, "spec", "gateways")) {
            String ref = String.valueOf(entry).trim();
            if (ref.isEmpty() || MESH.equals(ref)) {
                continue;
            }
            int slash = ref.indexOf('/');
            if (slash > 0) {
                refs.add(new GatewayRef(ref.substring(0, slash), ref.substring(slash + 1)));
            } else if (ref.contains(".")) {
                String[] parts = ref.split("\\.");
                refs.add(new GatewayRef(parts[1], parts[0]));
            } else {
                refs.add(new GatewayRef(vsNamespace, ref));
            }
        }
        return refs;
    }
}
