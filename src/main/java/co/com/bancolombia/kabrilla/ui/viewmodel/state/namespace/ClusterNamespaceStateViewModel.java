package co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.Cluster;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.AppContextState;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ClusterNamespaceStateViewModel implements AppContextState {

    private final ObservableList<ClusterStateViewModel> observableClusters = FXCollections.observableArrayList();

    public ObservableList<ClusterStateViewModel> getObservableClusters() {
        return observableClusters;
    }

    public void syncFromDomain(List<Cluster> domainClusters) {
        observableClusters.clear();
        for (Cluster cluster : domainClusters) {
            observableClusters.add(new ClusterStateViewModel(cluster));
        }
    }

    public void addNamespaceToCluster(String clusterName, Namespace namespace) {
        for (ClusterStateViewModel vm : observableClusters) {
            if (vm.getName().equals(clusterName)) {
                vm.addNamespace(namespace);
                break;
            }
        }
    }

    public void addDeploymentsToNamespace(String clusterName, String namespaceName, List<Deployment> deployments) {
        for (ClusterStateViewModel clusterVM : observableClusters) {
            if (clusterVM.getName().equals(clusterName)) {
                for (NamespaceStateViewModel nsVM : clusterVM.getNamespaces()) {
                    if (nsVM.getName().equals(namespaceName)) {
                        nsVM.setDeployments(deployments);
                        return;
                    }
                }
            }
        }
    }

}