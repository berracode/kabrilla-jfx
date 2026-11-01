package co.com.bancolombia.kabrilla.ui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javafx.application.Platform;

/**
 * Arranca el toolkit de JavaFX una sola vez y permite ejecutar código en su hilo desde los tests.
 */
public final class FxTestSupport {

    private static boolean started;

    private FxTestSupport() {
    }

    public static synchronized void startToolkit() throws InterruptedException {
        if (started) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        Platform.setImplicitExit(false);
        latch.await(10, TimeUnit.SECONDS);
        started = true;
    }

    public static <T> T onFx(Supplier<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Timeout en el hilo de JavaFX");
        }
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        return result.get();
    }

    public static void onFx(Runnable action) throws Exception {
        onFx(() -> {
            action.run();
            return null;
        });
    }

    public static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timeout esperando condición");
            }
            Thread.sleep(20);
        }
    }
}
