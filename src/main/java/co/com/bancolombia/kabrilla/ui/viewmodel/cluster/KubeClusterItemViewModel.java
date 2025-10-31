package co.com.bancolombia.kabrilla.ui.viewmodel.cluster;

public record KubeClusterItemViewModel(
        String serverUrl,
        String clusterName,
        byte[] caCertBytes) {
}
