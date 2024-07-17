package io.quarkiverse.fx.deployment;

/**
 * Some constants shared across tests
 */
public final class FxTestConstants {

    // Allow some time between launch and FX readiness
    public static final int LAUNCH_TIMEOUT_MS = 3_000;

    private FxTestConstants() {
        // Not instantiable
    }
}
