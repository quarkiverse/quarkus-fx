package io.quarkiverse.fx.deployment.reload;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import io.quarkiverse.fx.FxLifecycle;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Inspects the visible UI after each attachment attempt, avoiding asynchronous startup races in the test. */
@QuarkusMain
public class PreserveTestMain implements QuarkusApplication {

    @Inject
    FxLifecycle lifecycle;

    @Inject
    ReloadController controller;

    @Override
    public int run(String... args) throws Exception {
        lifecycle.start(args);
        CompletableFuture<String> checkpoint = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                Window window = Window.getWindows().get(0);
                Label label = (Label) ((VBox) window.getScene().getRoot()).getChildren().get(0);
                checkpoint.complete(controller.version() + "|" + System.identityHashCode(window) + "|" + label.getText());
            } catch (Throwable failure) {
                checkpoint.completeExceptionally(failure);
            }
        });
        System.setProperty("fx.test.reload.checkpoint", checkpoint.get(10, TimeUnit.SECONDS));
        Quarkus.waitForExit();
        return 0;
    }
}
