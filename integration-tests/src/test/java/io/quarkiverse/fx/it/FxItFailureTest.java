package io.quarkiverse.fx.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * A failed check : the application exits with 1, JavaFX running (Quarkus.asyncExit(1) while QuarkusFxApplication waits
 * for the exit, then returns 0). Its own class : JavaFX can only be launched once per JVM.
 */
@QuarkusMainTest
public class FxItFailureTest {

    @Test
    public void fail(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("fail");
        String output = result.getOutput() + "\n" + result.getErrorOutput();
        assertEquals(1, result.exitCode(), "exit code\n" + output);
        assertTrue(output.contains("RESULT environment OK"), output);
        assertTrue(output.contains("RESULT deliberate-failure FAILED"), output);
        assertTrue(output.contains("SUMMARY ok=1 failed=1 [deliberate-failure]"), output);
        // Quarkus.asyncExit while JavaFX runs : FxShutdownEvent on the FX thread (the first thread of a macOS native
        // executable)
        assertTrue(output.contains("FX-SHUTDOWN fxThread=true"), output);
    }
}
