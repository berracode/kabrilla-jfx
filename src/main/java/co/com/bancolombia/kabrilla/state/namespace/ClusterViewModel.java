package co.com.bancolombia.kabrilla.state.namespace;

import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.Namespace;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ClusterViewModel {
    private final String name;
    private final ObservableList<Namespace> namespaces = FXCollections.observableArrayList();

    public ClusterViewModel(Cluster cluster) {
        this.name = cluster.getName();
        this.namespaces.addAll(cluster.getNamespaces());
    }

    public String getName() {
        return name;
    }

    public ObservableList<Namespace> getNamespaces() {
        return namespaces;
    }

    public void addNamespace(Namespace ns) {
        if (!namespaces.contains(ns)) {
            namespaces.add(ns);
        }
    }
}
