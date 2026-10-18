package co.com.bancolombia.kabrilla.ui.viewmodel.deployment;

import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
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
