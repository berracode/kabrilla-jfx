package co.com.bancolombia.kabrilla.state.namespace;

import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.state.base.AppContextState;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ClusterNamespaceState implements AppContextState {

    private final ObservableList<Cluster> clusters = FXCollections.observableArrayList();

    public ObservableList<Cluster> getClusters() {
        return clusters;
    }

    public void addNamespaceToCluster(String clusterName, Namespace namespace) {
        clusters.stream()
                .filter(c -> c.getName().equals(clusterName))
                .findFirst()
                .ifPresentOrElse(
                        cluster -> cluster.getNamespaces().add(namespace),
                        () -> {
                            Cluster newCluster = new Cluster(clusterName);
                            newCluster.getNamespaces().add(namespace);
                            clusters.add(newCluster);
                        }
                );
    }

}
