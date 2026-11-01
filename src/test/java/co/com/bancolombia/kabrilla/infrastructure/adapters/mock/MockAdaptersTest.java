package co.com.bancolombia.kabrilla.infrastructure.adapters.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import co.com.bancolombia.kabrilla.domain.model.KubeConfig;
import co.com.bancolombia.kabrilla.domain.model.k8s.Deployment;
import co.com.bancolombia.kabrilla.domain.model.k8s.Pod;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MockAdaptersTest {

    private static final KubeConfig KUBE_CONFIG = KubeConfig.builder()
            .clusterName("arn:aws:eks:us-east-1:000000000000:cluster/mock-qa")
            .build();

    @TempDir
    Path root;

    @Test
    void listaDeploymentsYPodsDesdeCarpetas() throws Exception {
        Files.createDirectories(root.resolve("mock-qa/ventas-qa/orders-api"));
        Files.writeString(root.resolve("mock-qa/ventas-qa/orders-api/orders-api-abc.log"), "hola\n");
        Files.writeString(root.resolve("mock-qa/ventas-qa/orders-api/notas.md"), "no es un pod");

        List<Deployment> deployments = new MockDeploymentAdapter(root).findAll(KUBE_CONFIG, null, "ventas-qa");
        List<Pod> pods = new MockPodAdapter(root).listPods(KUBE_CONFIG, null, "ventas-qa", "orders-api");

        assertEquals(List.of("orders-api"), deployments.stream().map(Deployment::getName).toList());
        assertEquals(KUBE_CONFIG.getClusterName(), deployments.getFirst().getClusterName());
        assertEquals(List.of("orders-api-abc"), pods.stream().map(Pod::getName).toList());
    }

    @Test
    void namespaceSinCarpetaNoTieneDeployments() {
        assertTrue(new MockDeploymentAdapter(root).findAll(KUBE_CONFIG, null, "no-existe").isEmpty());
    }

    @Test
    void reproduceElArchivoYSigueLasLineasAgregadas() throws Exception {
        Path log = root.resolve("mock-qa/ventas-qa/orders-api/orders-api-abc.log");
        Files.createDirectories(log.getParent());
        Files.writeString(log, "linea 1\nlinea 2\n");

        List<String> received = new CopyOnWriteArrayList<>();
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        Closeable stream = new MockLogsAdapter(root, 0).streamPodLogs(
                KUBE_CONFIG, null, "ventas-qa", "orders-api-abc", null, false, null, null,
                (LogLine l) -> received.add(l.text()), errors::add);

        waitUntil(() -> received.size() == 2);
        Files.writeString(log, "linea 1\nlinea 2\nlinea 3\n");
        waitUntil(() -> received.size() == 3);
        stream.close();

        assertEquals(List.of("linea 1", "linea 2", "linea 3"), received);
        assertTrue(errors.isEmpty(), "Cerrar el stream no debe reportar error: " + errors);
    }

    @Test
    void losDatosDeMockDataSonConsistentes() throws Exception {
        Path logs = Path.of("mock-data", "logs");
        var pods = new MockPodAdapter(logs).listPods(KUBE_CONFIG, null, "ventas-qa", "orders-api");
        assertEquals(2, pods.size());
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timeout esperando condición");
            }
            Thread.sleep(50);
        }
    }
}
