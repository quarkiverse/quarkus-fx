package io.quarkiverse.fx;

/**
 * Fired on the FX thread before the UI is detached and CDI is destroyed, including on reload.
 * Observers should stop animations and tasks and remove externally registered listeners.
 * <p>
 * Never fired on another thread : not fired once JavaFX has exited (the application called {@code Platform.exit()}
 * before Quarkus shut down), and usually not when the JVM shuts down (a signal, {@code System.exit()}, Ctrl+C or
 * {@code q} in dev mode), since JavaFX then disposes itself at the same time : when it is fired then, JavaFX may dispose
 * itself under its observers, whose failures are logged at debug level. Release what must always be released in
 * {@code @PreDestroy} methods or {@code ShutdownEvent} observers.
 */
public final class FxShutdownEvent {
}
