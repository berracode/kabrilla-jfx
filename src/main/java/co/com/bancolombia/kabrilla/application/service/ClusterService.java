package co.com.bancolombia.kabrilla.application.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.ClusterInfo;
import co.com.bancolombia.kabrilla.domain.model.Namespace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterService {
    private static final Logger log = LoggerFactory.getLogger(ClusterService.class);
    private final KubeconfigService kubeconfigService;
    private final ClusterRepository repository;

    public ClusterService(KubeconfigService kubeconfigService, ClusterRepository repository) {
        this.kubeconfigService = kubeconfigService;
        this.repository = repository;
    }

    public List<Cluster> getAllClusters() {
        try {
            return repository.findAll();
        } catch (IOException e) {
            // Archivo dañado: la app arranca sin namespaces registrados en vez de no arrancar
            log.error("No se pudieron leer los namespaces registrados (clusters.json)", e);
            return List.of();
        }
    }

    public List<Namespace> getNamespacesForCluster(String clusterName) {
        var clusters = this.getAllClusters(); // ya tienes este método
        Optional<Cluster> clusterOpt = clusters
                .stream()
                .filter(c -> c.getName().equals(clusterName))
                .findFirst();

        return clusterOpt.map(Cluster::getNamespaces).orElse(Collections.emptyList());
    }

    public ClusterInfo getClusterInfo() {
        var clusterList = this.getAllClusters();
        var kubeConfigModel = kubeconfigService.getKubeConfig();

        return new ClusterInfo(clusterList, kubeConfigModel);
    }

    /**
     * Registra un namespace en el cluster.
     *
     * @throws IllegalArgumentException si el nombre no es válido o ya está registrado (mensaje para el usuario)
     */
    public void createNamespace(String clusterName, String namespaceName) {
        requireValidName(namespaceName);
        updateClusters(namespaceName, clusters -> {
            Cluster cluster = findCluster(clusters, clusterName);
            if (cluster == null) {
                cluster = new Cluster(clusterName);
                clusters.add(cluster);
            }
            requireNotRegistered(cluster, namespaceName);
            cluster.getNamespaces().add(new Namespace(namespaceName));
        });
    }

    /**
     * Cambia el nombre de un namespace registrado (p. ej. para corregir un error al escribirlo).
     *
     * @throws IllegalArgumentException si el nombre nuevo no es válido, ya existe o el original no está registrado
     */
    public void renameNamespace(String clusterName, String oldName, String newName) {
        requireValidName(newName);
        updateClusters(oldName, clusters -> {
            Cluster cluster = findCluster(clusters, clusterName);
            Namespace namespace = cluster == null ? null : findNamespace(cluster, oldName);
            if (namespace == null) {
                throw new IllegalArgumentException("El namespace " + oldName + " no está registrado en " + clusterName
                                                           + ".");
            }
            if (!oldName.equals(newName)) {
                requireNotRegistered(cluster, newName);
            }
            // Objeto nuevo en la misma posición: no se muta un Namespace que otra capa pueda estar usando
            List<Namespace> namespaces = cluster.getNamespaces();
            namespaces.set(namespaces.indexOf(namespace), new Namespace(newName));
        });
        log.info("Namespace {} renombrado a {} en el cluster {}", oldName, newName, clusterName);
    }

    /**
     * Quita un namespace de la lista de Kabrilla. No toca nada en el cluster.
     */
    public void removeNamespace(String clusterName, String namespaceName) {
        updateClusters(namespaceName, clusters -> {
            Cluster cluster = findCluster(clusters, clusterName);
            if (cluster != null) {
                cluster.getNamespaces().removeIf(ns -> namespaceName.equals(ns.getName()));
            }
        });
        log.info("Namespace {} quitado del cluster {}", namespaceName, clusterName);
    }

    private void updateClusters(String namespaceName, java.util.function.Consumer<List<Cluster>> change) {
        try {
            List<Cluster> clusters = new ArrayList<>(repository.findAll());
            change.accept(clusters);
            repository.saveAll(clusters);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el namespace " + namespaceName + ": " + e.getMessage(),
                                           e);
        }
    }

    private static Cluster findCluster(List<Cluster> clusters, String clusterName) {
        return clusters.stream()
                .filter(c -> c.getName().equalsIgnoreCase(clusterName))
                .findFirst()
                .orElse(null);
    }

    private static Namespace findNamespace(Cluster cluster, String name) {
        return cluster.getNamespaces().stream()
                .filter(ns -> name.equals(ns.getName()))
                .findFirst()
                .orElse(null);
    }

    private static void requireValidName(String name) {
        String problem = Namespace.validateName(name);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
    }

    private static void requireNotRegistered(Cluster cluster, String name) {
        if (findNamespace(cluster, name) != null) {
            throw new IllegalArgumentException("El namespace " + name + " ya está registrado en " + cluster.getName()
                                                       + ".");
        }
    }
}
