package io.quarkiverse.fx;

/** Controls what happens to the JavaFX UI when Quarkus restarts in dev mode. */
public enum HotReloadStrategy {
    /** Recreate the UI with the new runtime's CDI beans and FXML resources. */
    RECREATE,
    /** Keep the original UI without rebinding it to the new CDI container. */
    PRESERVE
}
