package io.quarkiverse.fx.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * The usual "Quit" of a JavaFX application, as written in an event handler : Platform.exit(), then Quarkus.asyncExit()
 * at once. Quarkus FX detaches from JavaFX before or after it exits (a race) : either way, the application exits at
 * once, without an error. Its own class : JavaFX can only be launched once per JVM.
 */
@QuarkusMainTest
public class FxItQuitTest {

    @Test
    public void quit(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("quit");
        long exitedAt = System.currentTimeMillis();
        String output = result.getOutput() + "\n" + result.getErrorOutput();
        assertEquals(0, result.exitCode(), "exit code\n" + output);
        assertTrue(output.contains("SUMMARY ok=2 failed=0"), output);
        assertFalse(output.contains("Timed out waiting for JavaFX"), output);
        assertFalse(output.contains("Running a shutdown task failed"), output);
        // Fired on the FX thread when Quarkus FX detached before JavaFX exited, never on another thread
        assertFalse(output.contains("FX-SHUTDOWN fxThread=false"), output);
        long requestedAt = Long.parseLong(FxItTest.value(output, "exitRequestedAt"));
        assertTrue(exitedAt - requestedAt < 15_000,
                "exited " + (exitedAt - requestedAt) + " ms after Platform.exit()\n" + output);
    }
}
