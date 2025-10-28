package co.com.bancolombia.kabrilla.application.service;


import java.util.Optional;

import co.com.bancolombia.kabrilla.domain.gateway.CredentialRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;

public class CredentialService {

    private final CredentialRepository repository;

    public CredentialService(CredentialRepository repository) {
        this.repository = repository;
    }

    public void saveCredentials(String clusterName, String rawCredentials) {
        AwsCredentials parsed = parseCredentials(clusterName, rawCredentials);
        repository.save(parsed);
    }

    public Optional<AwsCredentials> getCredentials(String clusterName) {
        return repository.findByClusterName(clusterName);
    }

    private AwsCredentials parseCredentials(String clusterName, String raw) {
        String[] lines = raw.split("\\r?\\n");
        String accessKey = "", secretKey = "", sessionToken = "";

        for (String line : lines) {
            if (line.startsWith("aws_access_key_id=")) {
                accessKey = line.substring("aws_access_key_id=".length()).trim();
            } else if (line.startsWith("aws_secret_access_key=")) {
                secretKey = line.substring("aws_secret_access_key=".length()).trim();
            } else if (line.startsWith("aws_session_token=")) {
                sessionToken = line.substring("aws_session_token=".length()).trim();
            }
        }

        return new AwsCredentials(clusterName, accessKey, secretKey, sessionToken);
    }
}

