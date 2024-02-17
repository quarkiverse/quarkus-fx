package io.quarkiverse.fx;

import javafx.stage.Stage;

/**
 * Event used to indicate that the FX application has started.
 * Holds the {@linkplain Stage} instance.
 */
public class FxStartupEvent {

    private final Stage primaryStage;

    FxStartupEvent(final Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Stage getPrimaryStage() {
        return this.primaryStage;
    }
}
