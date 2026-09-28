package io.quarkiverse.fx.it;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.fx.RunOnFxThread;
import javafx.application.Platform;

@ApplicationScoped
public class FxThreadProbe {

    static final String NOT_FX = "not the JavaFX application thread : ";

    /**
     * Completes the future with the name of the thread it runs on.
     */
    @RunOnFxThread
    public void record(CompletableFuture<String> thread) {
        thread.complete((Platform.isFxApplicationThread() ? "" : NOT_FX) + Thread.currentThread().getName());
    }
}
