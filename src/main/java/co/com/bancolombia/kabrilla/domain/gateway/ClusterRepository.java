package co.com.bancolombia.kabrilla.domain.gateway;

import java.io.IOException;
import java.util.List;

import co.com.bancolombia.kabrilla.domain.model.Cluster;

public interface ClusterRepository {

    List<Cluster> findAll() throws IOException;

    void saveAll(List<Cluster> clusters) throws IOException;

}
