package co.com.bancolombia.kabrilla.ui.task;

import java.util.List;

import co.com.bancolombia.kabrilla.application.service.ClusterService;
import co.com.bancolombia.kabrilla.domain.model.Namespace;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadNamespacesTask extends Task<List<Namespace>> {

    private static final Logger logger = LoggerFactory.getLogger(LoadNamespacesTask.class);

    private final ClusterService clusterService;
    private final String clusterName;

    public LoadNamespacesTask(ClusterService clusterService, String clusterName) {
        this.clusterService = clusterService;
        this.clusterName = clusterName;
    }

    @Override
    protected List<Namespace> call() throws Exception {
        try {
            return clusterService.getNamespacesForCluster(clusterName);
        } catch (Exception e) {
            logger.error("Error al obtener namespaces para el cluster {}", clusterName, e);
            throw e;
        }
    }
}

