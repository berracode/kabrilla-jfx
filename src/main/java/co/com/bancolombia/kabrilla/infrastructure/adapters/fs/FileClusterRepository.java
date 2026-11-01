package co.com.bancolombia.kabrilla.infrastructure.adapters.fs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.gateway.ClusterRepository;
import co.com.bancolombia.kabrilla.domain.model.Cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileClusterRepository implements ClusterRepository {

    private final File configFile;
    private final ObjectMapper mapper;

    public FileClusterRepository(File configFile) {
        this.configFile = configFile;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public List<Cluster> findAll() throws IOException {
        if (!configFile.exists() || configFile.length() == 0) {
            return List.of(); // archivo no existe o está vacío
        }

        Cluster[] clusters = mapper.readValue(configFile, Cluster[].class);
        return Arrays.asList(clusters);
    }

    @Override
    public void saveAll(List<Cluster> clusters) throws IOException {
        configFile.getAbsoluteFile().getParentFile().mkdirs();
        mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, clusters);
        log.info("Clusters guardados en {}", configFile.getAbsolutePath());

    }
}
