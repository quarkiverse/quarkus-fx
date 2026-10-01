package io.quarkiverse.fx.showcase.pages.fxml;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.fx.RunOnFxThread;
import javafx.application.Platform;

/**
 * A bean whose method is moved to the JavaFX Application Thread by the quarkus-fx {@code @RunOnFxThread} interceptor.
 */
@ApplicationScoped
public class FxThreadProbe {

    @RunOnFxThread
    public void record(String caller, CompletableFuture<String> result) {
        result.complete(caller + " -> ran on FX thread: " + Platform.isFxApplicationThread());
    }
}
