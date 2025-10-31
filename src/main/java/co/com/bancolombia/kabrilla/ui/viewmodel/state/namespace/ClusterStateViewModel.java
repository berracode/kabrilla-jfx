package co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace;

import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ClusterStateViewModel {
    private final String name;
    private final ObservableList<NamespaceStateViewModel> namespaces = FXCollections.observableArrayList();

    public ClusterStateViewModel(Cluster cluster) {
        this.name = cluster.getName();
        cluster.getNamespaces().forEach(ns -> namespaces.add(new NamespaceStateViewModel(ns)));
    }

    public String getName() {
        return name;
    }

    public ObservableList<NamespaceStateViewModel> getNamespaces() {
        return namespaces;
    }

    public void addNamespace(Namespace ns) {
        if (namespaces.stream().noneMatch(vm -> vm.getName().equals(ns.getName()))) {
            namespaces.add(new NamespaceStateViewModel(ns));
        }
    }
}
