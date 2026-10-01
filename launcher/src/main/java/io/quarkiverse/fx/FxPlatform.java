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

    private static final String EXITED = "JavaFX has exited (Platform.exit()) : it cannot be started again in this JVM";

    private static final String JVM_SHUTDOWN = "The JVM is shutting down : JavaFX no longer runs actions";

    /**
     * How long to wait for an action that runs on the FX thread while the JVM shuts down.
     */
    private static final long SHUTDOWN_GRACE_SECONDS = 2;

    private static final AtomicBoolean LAUNCHED = new AtomicBoolean();
    private static final CompletableFuture<FxPlatform> READY = new CompletableFuture<>();

    /**
     * Completed, with the reason, once JavaFX no longer runs the actions posted to the FX thread : Application.launch
     * has returned (the toolkit has exited), or the JVM is shutting down (the shutdown hook of JavaFX disposes the
     * toolkit, and Application.launch then never returns). Platform.runLater silently drops actions then.
     */
    private static final CompletableFuture<String> ENDED = new CompletableFuture<>();

    private final Application application;
    private WindowBounds windowBounds;

    private FxPlatform(Application application) {
        this.application = application;
    }

    static void started(Application application) {
        Platform.setImplicitExit(false);
        READY.complete(new FxPlatform(application));
    }

    /**
     * Launches JavaFX once per JVM, and returns the running shell.
     *
     * @throws IllegalStateException at once when JavaFX no longer runs : it has exited, or the JVM is shutting down
     */
    public static FxPlatform launch(String... args) {
        if (LAUNCHED.compareAndSet(false, true)) {
            Thread launcher = new Thread(() -> {
                try {
                    Application.launch(FxShellApplication.class, args);
                } catch (Throwable failure) {
                    READY.completeExceptionally(failure);
                } finally {
                    // The FX thread has run the actions accepted before the toolkit exited (they precede Toolkit.exit
                    // in its queue), and Platform.runLater dropped the later ones
                    ENDED.complete(EXITED);
                    // No effect once the shell has started : JavaFX exited without starting it
                    READY.completeExceptionally(new IllegalStateException(EXITED));
                }
            }, "quarkus-fx-launcher");
            // On a signal or System.exit(), the shutdown hook of JavaFX disposes the toolkit, which then drops actions
            Thread shutdown = new Thread(() -> ENDED.complete(JVM_SHUTDOWN), "quarkus-fx-shutdown");
            // The FX thread must not inherit the first application's reloadable classloader.
            launcher.setContextClassLoader(FxPlatform.class.getClassLoader());
            shutdown.setContextClassLoader(FxPlatform.class.getClassLoader());
            try {
                Runtime.getRuntime().addShutdownHook(shutdown);
                launcher.start();
            } catch (IllegalStateException shuttingDown) {
                // Too late to start JavaFX
                ENDED.complete(JVM_SHUTDOWN);
                READY.completeExceptionally(shuttingDown);
            }
        }
        // JavaFX never starts once the JVM shuts down while it starts (it disposes itself and drops the start of the
        // shell) : stop waiting then
        await(CompletableFuture.anyOf(READY, ENDED));
        String ended = ENDED.getNow(null);
        if (ended != null) {
            throw new IllegalStateException(ended);
        }
        return READY.join();
    }

    /**
     * @return whether the JVM is shutting down : JavaFX then disposes itself in its own shutdown hook, also while an
     *         action runs on the FX thread, which may then fail
     */
    public static boolean isShuttingDown() {
        // Shutdown hooks can no longer be registered once the JVM is shutting down
        Thread probe = new Thread(() -> {
        }, "quarkus-fx-shutdown-probe");
        try {
            Runtime.getRuntime().addShutdownHook(probe);
            Runtime.getRuntime().removeShutdownHook(probe);
            return false;
        } catch (IllegalStateException shuttingDown) {
            ENDED.complete(JVM_SHUTDOWN);
            return true;
        }
    }

    /**
     * Runs the action on the FX thread, with the classloader as context classloader, and waits for it. On the FX
     * thread, runs it at once.
     *
     * @return true when the action ran, or still runs once the JVM shuts down (after a short wait : it may be blocked in
     *         System.exit()) ; false when JavaFX no longer runs actions (it has exited, or the JVM is shutting down) : the
     *         action did not run, and never will
     * @throws IllegalStateException when the action fails, or when JavaFX does not run it in time (it may then still
     *         run later, as the FX thread gets to it)
     */
    public boolean invoke(ClassLoader classLoader, Consumer<Application> action) {
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
            return true;
        }
        if (ENDED.isDone()) {
            return false;
        }
        AtomicBoolean claimed = new AtomicBoolean();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        Platform.runLater(() -> {
            // Not once the waiting thread has given up on it
            if (claimed.compareAndSet(false, true)) {
                try {
                    task.run();
                    completion.complete(null);
                } catch (Throwable failure) {
                    completion.completeExceptionally(failure);
                }
            }
        });
        // Platform.runLater silently drops the action once JavaFX no longer runs actions : stop waiting then
        await(CompletableFuture.anyOf(completion, ENDED));
        if (claimed.compareAndSet(false, true)) {
            // JavaFX no longer runs actions, and had not started this one : it never will
            return false;
        }
        if (!completion.isDone()) {
            // Started before the JVM began shutting down : the action may itself be blocked in System.exit(), which
            // waits for the shutdown hooks. Wait for its end only briefly
            try {
                completion.get(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException stillRunning) {
                return true;
            } catch (InterruptedException e) {
                // Reported by the await below, which fails at once
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // Reported by the await below
            }
        }
        await(completion);
        return true;
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
