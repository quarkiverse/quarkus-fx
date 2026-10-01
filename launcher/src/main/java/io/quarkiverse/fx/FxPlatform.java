package io.quarkiverse.fx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Process-wide toolkit ownership, loaded outside the reloadable Quarkus runtime.
 * Never store runtime callbacks, beans or classloaders in this class.
 */
public final class FxPlatform {

    private static final AtomicBoolean LAUNCHED = new AtomicBoolean();
    private static final CompletableFuture<FxPlatform> READY = new CompletableFuture<>();

    private final Application application;
    private WindowBounds windowBounds;

    private FxPlatform(Application application) {
        this.application = application;
    }

    static void started(Application application) {
        Platform.setImplicitExit(false);
        READY.complete(new FxPlatform(application));
    }

    public static FxPlatform launch(String... args) {
        if (LAUNCHED.compareAndSet(false, true)) {
            Thread launcher = new Thread(() -> {
                try {
                    Application.launch(FxShellApplication.class, args);
                } catch (Throwable failure) {
                    READY.completeExceptionally(failure);
                }
            }, "quarkus-fx-launcher");
            // The FX thread must not inherit the first application's reloadable classloader.
            launcher.setContextClassLoader(FxPlatform.class.getClassLoader());
            launcher.start();
        }
        return await(READY);
    }

    public void invoke(ClassLoader classLoader, Consumer<Application> action) {
        Runnable task = () -> {
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(classLoader);
                action.accept(this.application);
            } finally {
                thread.setContextClassLoader(previous);
            }
        };
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            CompletableFuture<Void> completion = new CompletableFuture<>();
            Platform.runLater(() -> {
                try {
                    task.run();
                    completion.complete(null);
                } catch (Throwable failure) {
                    completion.completeExceptionally(failure);
                }
            });
            await(completion);
        }
    }

    /** Called on the FX thread; keeps only scalar state between application generations. */
    public void rememberWindow(Stage stage) {
        this.windowBounds = new WindowBounds(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    public void restoreWindow(Stage stage) {
        if (this.windowBounds != null) {
            if (Double.isFinite(this.windowBounds.x())) {
                stage.setX(this.windowBounds.x());
            }
            if (Double.isFinite(this.windowBounds.y())) {
                stage.setY(this.windowBounds.y());
            }
            if (Double.isFinite(this.windowBounds.width())) {
                stage.setWidth(this.windowBounds.width());
            }
            if (Double.isFinite(this.windowBounds.height())) {
                stage.setHeight(this.windowBounds.height());
            }
        }
    }

    private record WindowBounds(double x, double y, double width, double height) {
    }

    private static <T> T await(CompletableFuture<T> completion) {
        try {
            return completion.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for JavaFX", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("JavaFX lifecycle operation failed", e.getCause());
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timed out waiting for JavaFX", e);
        }
    }
}
