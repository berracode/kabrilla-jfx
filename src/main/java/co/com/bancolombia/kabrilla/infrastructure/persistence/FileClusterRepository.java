package co.com.bancolombia.kabrilla.infrastructure.persistence;

import java.io.File;
import java.io.IOException;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.model.Cluster;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class FileClusterRepository implements ClusterRepository {

    private static final File CONFIG_FILE =
            new File(System.getProperty("user.dir"), "clusters.json");


    private final ObjectMapper mapper;

    public FileClusterRepository() {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public List<Cluster> findAll() throws IOException {
        if (!CONFIG_FILE.exists()) {
            return List.of();
        }
        return List.of(mapper.readValue(CONFIG_FILE, Cluster[].class));
    }

    @Override
    public void saveAll(List<Cluster> clusters) throws IOException {
        CONFIG_FILE.getParentFile().mkdirs();
        mapper.writerWithDefaultPrettyPrinter().writeValue(CONFIG_FILE, clusters);
        System.out.println("✅ Clusters guardados en: " + CONFIG_FILE.getAbsolutePath());

    }
}
