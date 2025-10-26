package co.com.bancolombia.kabrilla.application.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.Namespace;

public class ClusterService {
    private final ClusterRepository repository;

    public ClusterService(ClusterRepository repository) {
        this.repository = repository;
    }

    public List<Cluster> getAllClusters() {
        try {
            return repository.findAll();
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void createNamespace(String clusterName, String namespaceName) {
        try {
            List<Cluster> clusters = new ArrayList<>(repository.findAll());

            // Buscar si ya existe el cluster
            Cluster existingCluster = clusters.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(clusterName))
                    .findFirst()
                    .orElse(null);

            if (existingCluster == null) {
                existingCluster = new Cluster(clusterName);
                clusters.add(existingCluster);
            }

            existingCluster.addNamespace(new Namespace(namespaceName));

            repository.saveAll(clusters);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
