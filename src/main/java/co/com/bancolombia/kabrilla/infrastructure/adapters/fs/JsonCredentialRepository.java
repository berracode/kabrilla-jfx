package co.com.bancolombia.kabrilla.infrastructure.adapters.fs;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import co.com.bancolombia.kabrilla.domain.gateway.CredentialRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonCredentialRepository implements CredentialRepository {

    private static final String FILE_PATH = "credentials.2.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void save(AwsCredentials credentials) {
        log.info("Credenciales a guardar: {}", credentials);
        List<AwsCredentials> all = findAll();
        log.info("Credenciales actuales: {}", all.size());
        all.removeIf(c -> c.getClusterName().equals(credentials.getClusterName())); // Reemplaza si ya existe
        all.add(credentials);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), all);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<AwsCredentials> findByClusterName(String clusterName) {
        return findAll().stream()
                .filter(c -> c.getClusterName().equals(clusterName))
                .findFirst();
    }

    public List<AwsCredentials> findAll() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        log.info("Exitse el archivo de credenciales..");
        try {
            return objectMapper.readValue(file, new TypeReference<>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

