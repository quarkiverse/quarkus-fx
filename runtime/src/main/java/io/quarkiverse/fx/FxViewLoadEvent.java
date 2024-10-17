package io.quarkiverse.fx;

import javafx.stage.Stage;

public final class FxViewLoadEvent {

    private final Stage primaryStage;

    FxViewLoadEvent(final Stage primaryStage) {
        // Package-private constructor
        this.primaryStage = primaryStage;
    }

    public Stage getPrimaryStage() {
        return this.primaryStage;
    }
}
