package co.com.bancolombia.kabrilla.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.Namespace;

import org.junit.jupiter.api.Test;

class ClusterServiceTest {

    private final InMemoryClusterRepository repository = new InMemoryClusterRepository();
    private final ClusterService service = new ClusterService(null, repository);

    @Test
    void createRejectsDuplicatesAndInvalidNames() {
        service.createNamespace("qa", "pagos-qa");

        var duplicate = assertThrows(IllegalArgumentException.class, () -> service.createNamespace("qa", "pagos-qa"));
        assertTrue(duplicate.getMessage().contains("ya está registrado"), duplicate.getMessage());
        assertThrows(IllegalArgumentException.class, () -> service.createNamespace("qa", "Pagos_QA"));
        assertEquals(List.of("pagos-qa"), namespaces("qa"));
    }

    @Test
    void renameFixesATypoKeepingPosition() {
        service.createNamespace("qa", "ventas-qa");
        service.createNamespace("qa", "pagso-qa");
        service.createNamespace("qa", "catalogo-qa");

        service.renameNamespace("qa", "pagso-qa", "pagos-qa");

        assertEquals(List.of("ventas-qa", "pagos-qa", "catalogo-qa"), namespaces("qa"));
    }

    @Test
    void renameRejectsExistingOrInvalidNameWithoutChangingAnything() {
        service.createNamespace("qa", "ventas-qa");
        service.createNamespace("qa", "pagos-qa");

        assertThrows(IllegalArgumentException.class, () -> service.renameNamespace("qa", "pagos-qa", "ventas-qa"));
        assertThrows(IllegalArgumentException.class, () -> service.renameNamespace("qa", "pagos-qa", "-pagos"));
        assertThrows(IllegalArgumentException.class, () -> service.renameNamespace("qa", "no-existe", "otro"));

        assertEquals(List.of("ventas-qa", "pagos-qa"), namespaces("qa"));
    }

    @Test
    void removeOnlyAffectsThatClusterAndNamespace() {
        service.createNamespace("qa", "pagos-qa");
        service.createNamespace("qa", "ventas-qa");
        service.createNamespace("dev", "pagos-qa");

        service.removeNamespace("qa", "pagos-qa");

        assertEquals(List.of("ventas-qa"), namespaces("qa"));
        assertEquals(List.of("pagos-qa"), namespaces("dev"));
    }

    @Test
    void validatesKubernetesNamespaceNames() {
        assertNull(Namespace.validateName("pagos-qa-2"));
        assertTrue(Namespace.validateName("pagos qa") != null);
        assertTrue(Namespace.validateName("pagos-") != null);
        assertTrue(Namespace.validateName("PAGOS") != null);
        assertTrue(Namespace.validateName("a".repeat(64)) != null);
        assertTrue(Namespace.validateName("  ") != null);
    }

    private List<String> namespaces(String cluster) {
        return repository.clusters.stream()
                .filter(c -> c.getName().equals(cluster))
                .flatMap(c -> c.getNamespaces().stream())
                .map(Namespace::getName)
                .toList();
    }

    /**
     * Guarda copias, como el archivo real: lo que no pase por saveAll no queda guardado.
     */
    private static final class InMemoryClusterRepository implements ClusterRepository {
        private List<Cluster> clusters = new ArrayList<>();

        @Override
        public List<Cluster> findAll() {
            return copy(clusters);
        }

        @Override
        public void saveAll(List<Cluster> updated) {
            clusters = copy(updated);
        }

        private static List<Cluster> copy(List<Cluster> source) {
            List<Cluster> result = new ArrayList<>();
            for (Cluster c : source) {
                Cluster clone = new Cluster(c.getName());
                c.getNamespaces().forEach(ns -> clone.getNamespaces().add(new Namespace(ns.getName())));
                result.add(clone);
            }
            return result;
        }
    }
}
