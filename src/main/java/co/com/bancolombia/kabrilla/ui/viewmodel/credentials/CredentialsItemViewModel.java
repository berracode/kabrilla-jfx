package co.com.bancolombia.kabrilla.ui.viewmodel.credentials;

public record CredentialsItemViewModel(
        String clusterName,
        String accessKey,
        String secretKey,
        String sessionToken
) {

    /**
     * Sin secretos: el toString por defecto de un record los incluiría y podrían terminar en logs.
     */
    @Override
    public String toString() {
        return "CredentialsItemViewModel[clusterName=" + clusterName + ", accessKey=" + mask(accessKey) + "]";
    }

    /**
     * Últimos 4 caracteres, suficiente para reconocer qué credencial es.
     */
    public static String mask(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "(vacío)";
        }
        return "****" + secret.substring(Math.max(0, secret.length() - 4));
    }
}
