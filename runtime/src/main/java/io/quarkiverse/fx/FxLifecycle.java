package io.quarkiverse.fx;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.fx.style.StylesheetWatchService;
import io.quarkiverse.fx.swt.SwtEmbeddingRecorder;
import io.quarkiverse.fx.views.FxViewConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Owns one Quarkus runtime's attachment to the persistent JavaFX shell. */
@Singleton
public class FxLifecycle {

    private static final Logger LOGGER = Logger.getLogger(FxLifecycle.class);

    @Inject
    BeanManager beanManager;

    @Inject
    FxViewConfig config;

    private boolean liveReload;

    public void setLiveReload(boolean liveReload) {
        this.liveReload = liveReload;
    }

    private boolean retainUiAcrossRestarts() {
        return LaunchMode.current() == LaunchMode.DEVELOPMENT
                && this.config.hotReloadStrategy() == HotReloadStrategy.PRESERVE;
    }

    private FxPlatform platform;
    private volatile ClassLoader classLoader;
    private ListChangeListener<Window> windowsListener;
    private Stage primaryStage;
    private volatile boolean active;
    private volatile boolean embeddedInSwt;

    /**
     * With Quarkus Desktop SWT : JavaFX runs embedded in SWT, started by the first FXCanvas on the SWT user interface
     * thread, which becomes the JavaFX Application Thread. Quarkus FX does not launch a JavaFX application then.
     */
    public void embedInSwt() {
        this.embeddedInSwt = true;
    }

    public boolean isEmbeddedInSwt() {
        return this.embeddedInSwt;
    }

    public synchronized void start(String... args) {
        if (this.embeddedInSwt) {
            // A JavaFX application launched next to SWT : on macOS, SWT then crashes the JVM when it creates its Display
            throw new IllegalStateException("Quarkus Desktop SWT is present : JavaFX runs embedded in SWT (FXCanvas), "
                    + "Quarkus FX does not launch a JavaFX application. Run the SWT user interface instead "
                    + "(SwtLifecycle.run()), see " + SwtEmbeddingRecorder.GUIDE);
        }
        if (this.platform != null || (this.liveReload && this.retainUiAcrossRestarts())) {
            return;
        }
        this.classLoader = Thread.currentThread().getContextClassLoader();
        boolean attached;
        try {
            // Fails at once when JavaFX no longer runs : after Platform.exit(), it cannot be started again in this JVM
            this.platform = FxPlatform.launch(args);
            attached = this.platform.invoke(
                    this.classLoader, application -> {
                        this.active = true;
                        // A fresh stage avoids retaining arbitrary user listeners in the persistent shell.
                        Stage stage = new Stage();
                        this.primaryStage = stage;
                        this.platform.restoreWindow(stage);
                        if (LaunchMode.current() == LaunchMode.NORMAL) {
                            this.windowsListener = change -> {
                                if (Window.getWindows().isEmpty()) {
                                    Quarkus.asyncExit();
                                }
                            };
                            Window.getWindows().addListener(this.windowsListener);
                        }
                        this.beanManager.getEvent().fire(new FxApplicationStartupEvent(application));
                        this.beanManager.getEvent().fire(new FxViewLoadEvent(stage));
                        this.beanManager.getEvent().fire(new FxPostStartupEvent(stage));
                    });
        } catch (IllegalStateException failure) {
            if (!FxPlatform.isShuttingDown()) {
                throw failure;
            }
            // The JVM shut down while JavaFX started, or while Quarkus FX attached to it : JavaFX disposes itself then
            LOGGER.debug("The JVM shut down while Quarkus FX attached to JavaFX", failure);
            attached = false;
        }
        if (!attached) {
            // This runtime never attached to JavaFX : there is nothing to detach
            this.platform = null;
            this.classLoader = null;
            if (!FxPlatform.isShuttingDown()) {
                throw new IllegalStateException("JavaFX stopped before Quarkus FX attached to it : it has exited");
            }
        }
    }

    void stop(@Observes @Priority(1) ShutdownEvent event) {
        this.detach();
    }

    /**
     * Detaches this Quarkus runtime from the JavaFX shell, and exits JavaFX in a packaged application. Called when Quarkus
     * shuts down, and by {@link QuarkusFxApplication} in a macOS native executable before Quarkus shuts down : JavaFX no
     * longer runs then. Only the first call detaches. Does not wait for JavaFX once it no longer runs (the application
     * exited it first, or the JVM is shutting down) : the work on the FX thread, FxShutdownEvent included, is skipped.
     * When the JVM shuts down while that work runs, JavaFX may dispose itself under it : its failure is logged at debug
     * level.
     */
    synchronized void detach() {
        if (this.retainUiAcrossRestarts()) {
            // Legacy behavior: the original UI and its CSS watchers survive the runtime restart.
            return;
        }
        this.active = false;
        StylesheetWatchService.stopAll();
        if (this.platform == null) {
            return;
        }
        // The work may run after this method returns, when JavaFX did not run it in time : it uses this shell, cleared
        // below
        FxPlatform shell = this.platform;
        boolean running = true;
        try {
            running = shell.invoke(
                    this.classLoader, application -> {
                        if (this.windowsListener != null) {
                            Window.getWindows().removeListener(this.windowsListener);
                            this.windowsListener = null;
                        }
                        try {
                            Stage stage = this.primaryStage;
                            if (stage != null && stage.isShowing()) {
                                shell.rememberWindow(stage);
                            }
                            this.beanManager.getEvent().fire(new FxShutdownEvent());
                        } finally {
                            StylesheetWatchService.stopAll();
                            // Hiding windows must not terminate the toolkit during a reload.
                            Platform.setImplicitExit(false);
                            for (Window window : List.copyOf(Window.getWindows())) {
                                window.hide();
                            }
                        }
                    });
            if (!running) {
                // JavaFX closes its windows itself, and the FxShutdownEvent observers expect the FX thread
                LOGGER.debug("JavaFX no longer runs : FxShutdownEvent is not fired");
            }
        } catch (IllegalStateException failure) {
            if (!FxPlatform.isShuttingDown()) {
                throw failure;
            }
            // The JVM is shutting down, and JavaFX disposed itself while the work ran on the FX thread (its renderer, as
            // a window was hidden) : the work could not complete
            running = false;
            LOGGER.debug("JavaFX stopped while Quarkus FX detached from it", failure);
        } finally {
            this.platform = null;
            this.primaryStage = null;
            this.classLoader = null;
            // Not once JavaFX no longer runs : it has exited already, or the JVM shuts down and JavaFX disposes itself
            if (running && LaunchMode.current() == LaunchMode.NORMAL && !FxPlatform.isShuttingDown()) {
                Platform.exit();
            }
        }
    }

    /**
     * Schedules work only while this Quarkus runtime is attached. Embedded in SWT, on the JavaFX Application Thread once
     * an FXCanvas has started JavaFX : {@link IllegalStateException} before.
     */
    public void runLater(Runnable action) {
        if (this.embeddedInSwt) {
            Platform.runLater(action);
            return;
        }
        if (!this.active) {
            return;
        }
        Platform.runLater(() -> {
            // Read once : once JavaFX no longer runs actions, detach clears them without waiting for the FX thread
            FxPlatform current = this.platform;
            ClassLoader loader = this.classLoader;
            if (this.active && current != null && loader != null) {
                current.invoke(loader, application -> action.run());
            }
        });
    }

    /** The current runtime loader, including when called from an ordinary FX event handler. */
    public ClassLoader getApplicationClassLoader() {
        ClassLoader current = this.classLoader;
        return current != null ? current : Thread.currentThread().getContextClassLoader();
    }
}
