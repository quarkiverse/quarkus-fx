package io.quarkiverse.fx;

import javafx.stage.Stage;

/**
 * Event used to indicate that the FX application has started.
 * Holds the {@linkplain Stage} instance.
 */
public record FxStartupEvent(Stage primaryStage) {
}
