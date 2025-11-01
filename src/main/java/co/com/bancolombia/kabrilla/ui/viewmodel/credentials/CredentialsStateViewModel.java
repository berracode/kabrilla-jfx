package co.com.bancolombia.kabrilla.ui.viewmodel.credentials;

import co.com.bancolombia.kabrilla.application.service.CredentialService;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.base.AppState;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterNamespaceStateViewModel;
import co.com.bancolombia.kabrilla.ui.viewmodel.state.namespace.ClusterStateViewModel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

public class CredentialsStateViewModel {

    private final AppState appState; //estado global
    private final ClusterNamespaceStateViewModel clusterNamespaceStateViewModel;
    private final CredentialService credentialService;

    private final StringProperty selectedCluster = new SimpleStringProperty();
    private final StringProperty rawCredentials = new SimpleStringProperty();


    public CredentialsStateViewModel(AppState appState, CredentialService credentialService,
                                     ClusterNamespaceStateViewModel clusterState) {
        this.appState = appState;
        this.credentialService = credentialService;
        this.clusterNamespaceStateViewModel = clusterState;
    }


    public StringProperty selectedClusterProperty() {
        return selectedCluster;
    }

    public StringProperty rawCredentialsProperty() {
        return rawCredentials;
    }


    public ObservableList<ClusterStateViewModel> getAvailableClusters() {
        return clusterNamespaceStateViewModel.getObservableClusters();
    }


    public boolean saveCredentials() {
        String cluster = selectedCluster.get();
        String credentials = rawCredentials.get();

        if (cluster == null || cluster.isBlank() || credentials == null || credentials.isBlank()) {
            return false;
        }

        var awsCredential = credentialService.saveCredentials(cluster, credentials);
        addCredential(new CredentialsItemViewModel(awsCredential.getClusterName(), awsCredential.getAccessKey(),
                                                   awsCredential.getSecretKey(), awsCredential.getSessionToken()));
        return true;
    }

    public void clear() {
        selectedCluster.set("");
        rawCredentials.set("");
    }

    // CREDENTIALS

    public ObservableList<CredentialsItemViewModel> getCredentials() {
        return appState.getCredentials();
    }

    public void addCredential(CredentialsItemViewModel credentialsItemViewModel) {
        replaceCredential(credentialsItemViewModel);
    }

    private void replaceCredential(CredentialsItemViewModel newCredential) {
        ObservableList<CredentialsItemViewModel> credentials = appState.getCredentials();

        for (int i = 0; i < credentials.size(); i++) {
            if (credentials.get(i).clusterName().equals(newCredential.clusterName())) {
                credentials.set(i, newCredential);
                return;
            }
        }

        credentials.add(newCredential);
    }


}
