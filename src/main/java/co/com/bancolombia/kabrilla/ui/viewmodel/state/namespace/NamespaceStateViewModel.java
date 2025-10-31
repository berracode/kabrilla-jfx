package co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.Deployment;
import co.com.bancolombia.kabrilla.domain.model.Namespace;
import co.com.bancolombia.kabrilla.ui.viewmodel.deployment.DeploymentItemViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class NamespaceStateViewModel {

    private final Namespace domainNamespace; // referencia al dominio puro
    private final ObservableList<DeploymentItemViewModel> deployments = FXCollections.observableArrayList();

    public NamespaceStateViewModel(Namespace domainNamespace) {
        this.domainNamespace = domainNamespace;
    }

    public String getName() {
        return domainNamespace.getName();
    }

    public ObservableList<DeploymentItemViewModel> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<Deployment> deploymentList) {
        deployments.setAll(deploymentList.stream()
                                   .map(DeploymentItemViewModel::new)
                                   .toList());
    }

    public Namespace getDomainNamespace() {
        return domainNamespace;
    }
}

