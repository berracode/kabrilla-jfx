package co.com.bancolombia.kabrilla.ui.viewmodel.logs;

import static co.com.bancolombia.kabrilla.ui.FxTestSupport.onFx;
import static co.com.bancolombia.kabrilla.ui.FxTestSupport.waitUntil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import co.com.bancolombia.kabrilla.application.service.LogsService;
import co.com.bancolombia.kabrilla.domain.gateway.k8s.LogsRepository;
import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;
import co.com.bancolombia.kabrilla.ui.FxTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogsViewModelTest {

    /**
     * Repositorio falso: guarda los callbacks de cada pod para emitir líneas/errores a voluntad.
     */
    private final Map<String, Consumer<LogLine>> onLineByPod = new HashMap<>();
    private final Map<String, Consumer<Throwable>> onErrorByPod = new HashMap<>();
    private final List<String> shown = new CopyOnWriteArrayList<>();
    private LogsViewModel vm;

    @BeforeEach
    void setUp() throws Exception {
        FxTestSupport.startToolkit();
        LogsRepository repo = (kc, creds, ns, pod, container, ts, tail, since, onLine, onError) -> {
            onLineByPod.put(pod, onLine);
            onErrorByPod.put(pod, onError);
            return () -> {
            };
        };
        vm = new LogsViewModel(new LogsService(repo));
        vm.setView(new LogView() {
            @Override
            public void append(List<String> lines) {
                shown.addAll(lines);
            }

            @Override
            public void reset(List<String> lines) {
                shown.clear();
                shown.addAll(lines);
            }
        });
    }

    private void follow(String pod) throws Exception {
        onFx(() -> vm.startFollowing(null, null, "c", "ns", "dep", pod, null, false, null, null));
    }

    @Test
    void linesReachTheView() throws Exception {
        follow("pod-a");
        onLineByPod.get("pod-a").accept(LogLine.now("hola"));

        waitUntil(() -> shown.size() == 1);
        assertTrue(shown.getFirst().endsWith("hola"));
    }

    @Test
    void previousStreamDoesNotLeakIntoNewOne() throws Exception {
        follow("pod-a");
        follow("pod-b");

        // El stream viejo sigue emitiendo / falla al cancelarse después del cambio de pod
        onLineByPod.get("pod-a").accept(LogLine.now("linea vieja"));
        onErrorByPod.get("pod-a").accept(new IOException("Canceled"));
        onLineByPod.get("pod-b").accept(LogLine.now("linea nueva"));

        waitUntil(() -> shown.size() == 1);
        Thread.sleep(300); // margen para que llegara cualquier lote o error indebido
        assertEquals(1, shown.size());
        assertTrue(shown.getFirst().endsWith("linea nueva"));
        assertNull(onFx(() -> vm.errorProperty().get()));
        assertTrue(onFx(() -> vm.followingProperty().get()));
    }

    @Test
    void stopIgnoresLaterLinesAndClearEmptiesView() throws Exception {
        follow("pod-a");
        onLineByPod.get("pod-a").accept(LogLine.now("antes"));
        waitUntil(() -> shown.size() == 1);

        onFx(vm::stop);
        onLineByPod.get("pod-a").accept(LogLine.now("después del stop"));
        Thread.sleep(300);
        assertEquals(1, shown.size());
        assertFalse(onFx(() -> vm.followingProperty().get()));

        onFx(vm::clear);
        assertTrue(shown.isEmpty());
    }
}
