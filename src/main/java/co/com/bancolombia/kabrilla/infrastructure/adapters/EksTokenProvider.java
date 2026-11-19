package co.com.bancolombia.kabrilla.infrastructure.adapters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EksTokenProvider {

    private static final ObjectMapper mapper = new ObjectMapper();


    public static String getEksToken(AwsCredentials creds) throws IOException {
        if (creds == null) {
            throw new IOException("No hay credenciales AWS para este cluster: agrégalas con clic derecho sobre el "
                                          + "cluster → Credenciales AWS…");
        }
        ProcessBuilder pb = new ProcessBuilder("aws", "eks", "get-token", "--cluster-name", creds.getClusterName());
        Map<String, String> env = pb.environment();
        env.put("AWS_ACCESS_KEY_ID", creds.getAccessKey());
        env.put("AWS_SECRET_ACCESS_KEY", creds.getSecretKey());
        env.put("AWS_SESSION_TOKEN", creds.getSessionToken());

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IOException("No se pudo ejecutar AWS CLI (\"aws\"). ¿Está instalado y en el PATH?", e);
        }
        String result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();

        String token = result.isBlank() ? "" : mapper.readTree(result).at("/status/token").asText();
        if (token.isBlank()) {
            // stderr de AWS CLI no contiene secretos (p. ej. "ExpiredToken: The security token included ... expired")
            throw new IOException("AWS CLI no devolvió token para " + creds.getClusterName()
                                          + (errors.isEmpty() ? "" : ": " + errors));
        }
        return token;
    }
}
