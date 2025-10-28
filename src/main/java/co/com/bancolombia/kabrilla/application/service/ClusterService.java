package co.com.bancolombia.kabrilla.application.service;

import java.io.IOException;
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
            log.info("Tratando de obtener los cluster con sus namesapces");
            Thread.sleep(5000);
            log.info("DESPERTANDO...");

            return repository.findAll();
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
