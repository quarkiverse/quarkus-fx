package io.quarkiverse.fx.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * The usual "Quit" of a JavaFX application, Platform.exit() then Quarkus.asyncExit() : the application exits at once,
 * Quarkus FX does not wait for JavaFX, which has exited. Its own class : JavaFX can only be launched once per JVM.
 */
@QuarkusMainTest
public class FxItPlatformExitTest {

    @Test
    public void platformExit(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("platform-exit");
        long exitedAt = System.currentTimeMillis();
        String output = result.getOutput() + "\n" + result.getErrorOutput();
        assertEquals(0, result.exitCode(), "exit code\n" + output);
        assertTrue(output.contains("SUMMARY ok=2 failed=0"), output);
        assertFalse(output.contains("Timed out waiting for JavaFX"), output);
        assertFalse(output.contains("Running a shutdown task failed"), output);
        // FxShutdownEvent needs the FX thread : not fired once JavaFX has exited
        assertFalse(output.contains("FX-SHUTDOWN"), output);
        long requestedAt = Long.parseLong(FxItTest.value(output, "exitRequestedAt"));
        assertTrue(exitedAt - requestedAt < 15_000,
                "exited " + (exitedAt - requestedAt) + " ms after Platform.exit()\n" + output);
    }
}
