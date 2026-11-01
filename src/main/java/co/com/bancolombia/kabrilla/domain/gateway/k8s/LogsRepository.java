package co.com.bancolombia.kabrilla.domain.gateway.k8s;

import java.io.Closeable;
import java.util.function.Consumer;

import co.com.bancolombia.kabrilla.domain.model.AwsCredentials;
import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogFetchResult;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogRange;

public interface LogsRepository {
    
    Closeable streamPodLogs(
            KubeConfig kubeConfig,
            AwsCredentials credentials,
            String namespace,
            String podName,
            String container,         // puede ser null si el pod es single-container
            boolean timestamps,
            Integer tailLines,
            Integer sinceSeconds,
            Consumer<LogLine> onLine,
            Consumer<Throwable> onError
    ) throws Exception;

    /**
     * Logs de un rango de tiempo, sin follow. Las marcas de tiempo son las del pod (kubelet).
     * Kubernetes solo conserva el archivo de log actual del container (y el del anterior con previous).
     */
    LogFetchResult fetchPodLogs(
            KubeConfig kubeConfig,
            AwsCredentials credentials,
            String namespace,
            String podName,
            String container,
            LogRange range,
            int maxLines
    ) throws Exception;
}
