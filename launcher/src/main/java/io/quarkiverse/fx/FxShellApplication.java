package io.quarkiverse.fx;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * The process-wide JavaFX shell. This class must not reference CDI or application classes.
 */
public class FxShellApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        FxPlatform.started(this);
    }
}
