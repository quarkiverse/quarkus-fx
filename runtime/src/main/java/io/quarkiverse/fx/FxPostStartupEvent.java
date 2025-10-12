package io.quarkiverse.fx;

import javafx.stage.Stage;

/**
 * Event used to indicate that the FX application has started.
 * Holds the {@linkplain Stage} instance.
 */
public final class FxPostStartupEvent {

    private final Stage primaryStage;

    FxPostStartupEvent(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Stage getPrimaryStage() {
        return this.primaryStage;
    }
}
