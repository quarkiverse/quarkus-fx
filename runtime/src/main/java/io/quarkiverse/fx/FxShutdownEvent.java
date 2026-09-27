package io.quarkiverse.fx;

/**
 * Fired on the FX thread before the UI is detached and CDI is destroyed, including on reload.
 * Observers should stop animations and tasks and remove externally registered listeners.
 */
public final class FxShutdownEvent {
}
