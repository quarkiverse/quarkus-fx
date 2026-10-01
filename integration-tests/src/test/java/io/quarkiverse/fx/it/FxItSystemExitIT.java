package io.quarkiverse.fx.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import io.quarkus.test.junit.main.QuarkusMainLauncher;

/**
 * System.exit() while JavaFX runs, as a signal (SIGTERM, Ctrl+C) : the application exits at once, Quarkus FX does not
 * wait for JavaFX, which disposes itself in its own shutdown hook. With the artifact of the build only : System.exit()
 * would exit the JVM of a {@code @QuarkusMainTest}.
 */
@QuarkusMainIntegrationTest
public class FxItSystemExitIT {

    @Test
    public void systemExit(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("system-exit");
        long exitedAt = System.currentTimeMillis();
        String output = result.getOutput() + "\n" + result.getErrorOutput();
        assertEquals(0, result.exitCode(), "exit code\n" + output);
        assertTrue(output.contains("RESULT stage OK"), output);
        assertFalse(output.contains("Timed out waiting for JavaFX"), output);
        assertFalse(output.contains("Running a shutdown task failed"), output);
        // Fired on the FX thread when Quarkus won the race with the shutdown hook of JavaFX, never on another thread
        assertFalse(output.contains("FX-SHUTDOWN fxThread=false"), output);
        long requestedAt = Long.parseLong(FxItTest.value(output, "exitRequestedAt"));
        assertTrue(exitedAt - requestedAt < 15_000,
                "exited " + (exitedAt - requestedAt) + " ms after System.exit()\n" + output);
    }
}
