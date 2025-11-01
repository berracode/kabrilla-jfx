package co.com.bancolombia.kabrilla.infrastructure.adapters;

import java.io.IOException;
import java.util.Map;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EksTokenProvider {

    private static final ObjectMapper mapper = new ObjectMapper();


    public static String getEksToken(AwsCredentials creds) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("aws", "eks", "get-token", "--cluster-name", creds.getClusterName());
        Map<String, String> env = pb.environment();
        env.put("AWS_ACCESS_KEY_ID", creds.getAccessKey());
        env.put("AWS_SECRET_ACCESS_KEY", creds.getSecretKey());
        env.put("AWS_SESSION_TOKEN", creds.getSessionToken());

        Process process = pb.start();
        String result = new String(process.getInputStream().readAllBytes());

        return mapper.readTree(result).at("/status/token").asText();
    }
}
