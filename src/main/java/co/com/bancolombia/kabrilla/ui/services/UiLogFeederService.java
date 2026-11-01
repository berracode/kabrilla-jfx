package co.com.bancolombia.kabrilla.ui.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import co.com.bancolombia.kabrilla.domain.model.k8s.logs.LogLine;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UiLogFeederService extends Service<Void> {
    private final BlockingQueue<LogLine> buffer;
    private final Consumer<String> onLinesBatchProcessed;

    // ~150 ms se percibe como tiempo real; 1000 líneas por lote da ~6.000 líneas/s de capacidad
    private static final long BATCH_INTERVAL_MS = 150;
    private static final int MAX_BATCH_LINES = 1000;

    // Pasamos el buffer y el callback explícitamente en el constructor
    public UiLogFeederService(BlockingQueue<LogLine> buffer, Consumer<String> onLinesBatchProcessed) {
        this.buffer = buffer;
        this.onLinesBatchProcessed = onLinesBatchProcessed;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<LogLine> drainList = new ArrayList<>();
                log.info("Inicia el hilo consumidor de LOGS");

                while (!isCancelled()) {
                    // Bloquea el hilo secundario hasta que entre un log
                    LogLine firstLog = buffer.take();
                    drainList.add(firstLog);

                    // Extrae ráfagas acumuladas para armar el lote (batch)
                    buffer.drainTo(drainList, MAX_BATCH_LINES - 1);

                    // Construcción del bloque de texto plano
                    StringBuilder sb = new StringBuilder();
                    for (LogLine logLine : drainList) {
                        sb.append(logLine.timestamp()).append(" - ").append(logLine.text()).append(" \n");
                    }
                    String textBatch = sb.toString();

                    // Despachamos el bloque al hilo de JavaFX de forma segura
                    Platform.runLater(() -> {
                        // Si se detuvo mientras el lote esperaba, no se pinta
                        if (onLinesBatchProcessed != null && !isCancelled()) {
                            onLinesBatchProcessed.accept(textBatch);
                        }
                    });

                    drainList.clear();
                    Thread.sleep(BATCH_INTERVAL_MS); // Pequeña tregua para acumular el siguiente lote
                }
                return null;
            }
        };
    }
}
