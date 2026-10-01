package io.quarkiverse.fx;

import io.quarkiverse.fx.style.StylesheetWatchService;
import io.quarkiverse.fx.views.FxViewConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/** Owns one Quarkus runtime's attachment to the persistent JavaFX shell. */
@Singleton
public class FxLifecycle {

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

    public synchronized void start(String... args) {
        if (this.platform != null || (this.liveReload && this.retainUiAcrossRestarts())) {
            return;
        }
      this.classLoader = Thread.currentThread().getContextClassLoader();
      this.platform = FxPlatform.launch(args);
      this.platform.invoke(
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
    }

    void stop(@Observes @Priority(1) ShutdownEvent event) {
        if (this.retainUiAcrossRestarts()) {
            // Legacy behavior: the original UI and its CSS watchers survive the runtime restart.
            return;
        }
      this.active = false;
        StylesheetWatchService.stopAll();
        if (this.platform == null) {
            return;
        }
        try {
          this.platform.invoke(
              this.classLoader, application -> {
                if (this.windowsListener != null) {
                    Window.getWindows().removeListener(this.windowsListener);
                  this.windowsListener = null;
                }
                try {
                    if (this.primaryStage != null && this.primaryStage.isShowing()) {
                      this.platform.rememberWindow(this.primaryStage);
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
        } finally {
          this.platform = null;
          this.primaryStage = null;
          this.classLoader = null;
            if (LaunchMode.current() == LaunchMode.NORMAL) {
                Platform.exit();
            }
        }
    }

    /** Schedules work only while this Quarkus runtime is attached. */
    public void runLater(Runnable action) {
        if (!this.active) {
            return;
        }
        Platform.runLater(() -> {
            if (this.active) {
              this.platform.invoke(this.classLoader, application -> action.run());
            }
        });
    }

    /** The current runtime loader, including when called from an ordinary FX event handler. */
    public ClassLoader getApplicationClassLoader() {
        ClassLoader current = this.classLoader;
        return current != null ? current : Thread.currentThread().getContextClassLoader();
    }
}
