package io.quarkiverse.fx;

import java.util.List;

import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Fired on the JavaFX application thread after a Quarkus dev-mode restart.
 * Managed FX views have been reloaded before this event is fired.
 */
public final class FxLiveReloadEvent {

    private final Stage primaryStage;
    private final List<Window> windows;

    public FxLiveReloadEvent(Stage primaryStage, List<Window> windows) {
        this.primaryStage = primaryStage;
        this.windows = List.copyOf(windows);
    }

    public Stage getPrimaryStage() {
        return this.primaryStage;
    }

    public List<Window> getWindows() {
        return this.windows;
    }
}
