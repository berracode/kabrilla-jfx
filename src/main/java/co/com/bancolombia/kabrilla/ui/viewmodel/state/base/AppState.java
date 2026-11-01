package co.com.bancolombia.kabrilla.ui.viewmodel.state.base;

import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.ui.viewmodel.cluster.KubeClusterItemViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.credentials.CredentialsItemViewModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AppState {
    private final ObservableList<CredentialsItemViewModel> credentials = FXCollections.observableArrayList();
    private final ObservableList<KubeClusterItemViewModel> kubeClusters = FXCollections.observableArrayList();

    public ObservableList<CredentialsItemViewModel> getCredentials() {
        return credentials;
    }

    public ObservableList<KubeClusterItemViewModel> getKubeConfig() {
        return kubeClusters;
    }

    public void loadCredentials(List<AwsCredentials> credentialsList) {
        credentialsList.forEach(aws -> credentials.add(new CredentialsItemViewModel(
                aws.getClusterName(), aws.getAccessKey(), aws.getSecretKey(), aws.getSessionToken()
        )));
    }

}
