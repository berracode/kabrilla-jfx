package co.com.bancolombia.kabrilla.ui.viewmodel.credentials;

public record CredentialsItemViewModel(
        String clusterName,
        String accessKey,
        String secretKey,
        String sessionToken
) {
}
