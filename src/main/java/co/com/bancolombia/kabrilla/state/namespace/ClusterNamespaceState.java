package co.com.bancolombia.kabrilla.state.namespace;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.state.base.AppContextState;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ClusterNamespaceState implements AppContextState {

    private final ObservableList<ClusterViewModel> observableClusters = FXCollections.observableArrayList();

    public ObservableList<ClusterViewModel> getObservableClusters() {
        return observableClusters;
    }

    public void syncFromDomain(List<Cluster> domainClusters) {
        observableClusters.clear();
        for (Cluster cluster : domainClusters) {
            observableClusters.add(new ClusterViewModel(cluster));
        }
    }

    public void addNamespaceToCluster(String clusterName, Namespace namespace) {
        for (ClusterViewModel vm : observableClusters) {
            if (vm.getName().equals(clusterName)) {
                vm.addNamespace(namespace);
                break;
            }
        }
    }
}