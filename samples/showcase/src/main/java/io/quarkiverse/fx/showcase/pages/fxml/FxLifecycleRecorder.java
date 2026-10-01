package io.quarkiverse.fx.showcase.pages.fxml;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkiverse.fx.FxApplicationStartupEvent;
import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.FxViewLoadEvent;
import io.quarkus.runtime.StartupEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Records the Quarkus and quarkus-fx startup events, in the order they are observed.
 */
@Singleton
public class FxLifecycleRecorder {

    final List<String> events = new CopyOnWriteArrayList<>();
    volatile Application application;
    volatile Stage primaryStage;

    void onQuarkusStartup(@Observes StartupEvent event) {
        events.add("StartupEvent (fx thread: " + Platform.isFxApplicationThread() + ")");
    }

    void onApplicationStartup(@Observes FxApplicationStartupEvent event) {
        application = event.getApplication();
        events.add("FxApplicationStartupEvent (fx thread: " + Platform.isFxApplicationThread() + ", "
                + event.getApplication().getClass().getSimpleName() + ")");
    }

    void onViewLoad(@Observes FxViewLoadEvent event) {
        events.add("FxViewLoadEvent (fx thread: " + Platform.isFxApplicationThread() + ", stage: "
                + (event.getPrimaryStage() != null) + ")");
    }

    void onPostStartup(@Observes FxPostStartupEvent event) {
        primaryStage = event.getPrimaryStage();
        events.add("FxPostStartupEvent (fx thread: " + Platform.isFxApplicationThread() + ", stage: "
                + (event.getPrimaryStage() != null) + ")");
    }
}
