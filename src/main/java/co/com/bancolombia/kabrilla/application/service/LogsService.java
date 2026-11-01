package co.com.bancolombia.kabrilla.application.service;


import java.io.Closeable;
import java.util.function.Consumer;

import co.com.bancolombia.kabrilla.domain.gateway.k8s.LogsRepository;
import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogFetchResult;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogRange;

public class LogsService {
    private final LogsRepository repository;

    public LogsService(LogsRepository repository) {
        this.repository = repository;
    }

    public Closeable followPodLogs(
            KubeConfig kubeConfig,
            AwsCredentials credentials,
            String namespace,
            String podName,
            String container,
            boolean timestamps,
            Integer tailLines,
            Integer sinceSeconds,
            Consumer<LogLine> onLine,
            Consumer<Throwable> onError
    ) throws Exception {
        return repository.streamPodLogs(kubeConfig, credentials, namespace, podName, container,
                                        timestamps, tailLines, sinceSeconds, onLine, onError
        );
    }

    public LogFetchResult fetchPodLogs(KubeConfig kubeConfig, AwsCredentials credentials, String namespace,
                                       String podName, String container, LogRange range, int maxLines)
            throws Exception {
        return repository.fetchPodLogs(kubeConfig, credentials, namespace, podName, container, range, maxLines);
    }
}

