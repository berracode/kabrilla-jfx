package co.com.bancolombia.kabrilla.ui.viewmodel.deployment;

import co.com.bancolombia.kabrilla.domain.model.Deployment;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class DeploymentStateViewModel {
    private final ObjectProperty<Deployment> selectedDeployment = new SimpleObjectProperty<>();

    public Deployment getSelectedDeployment() {
        return selectedDeployment.get();
    }

    public void setSelectedDeployment(Deployment deployment) {
        selectedDeployment.set(deployment);
    }

    public ObjectProperty<Deployment> selectedDeploymentProperty() {
        return selectedDeployment;
    }
}
