package io.quarkiverse.fx.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * Runs the application in the JVM of the tests. JavaFX can only be launched once per JVM : a single launch in this
 * class, and each test class in its own JVM (surefire reuseForks=false). Add a scenario in a new test class.
 */
@QuarkusMainTest
public class FxItTest {

    static final List<String> CHECKS = List.of("environment", "static-initializer", "fx-view", "stage", "stylesheet",
            "fxml-loader", "encoded-resource-names", "encoded-stylesheet", "image", "host-services", "run-on-fx-thread",
            "webview", "webview-missing-page");

    /**
     * The checks that may be skipped, where WebView cannot run (macOS native executables, see FxChecks).
     */
    static final List<String> WEBVIEW_CHECKS = List.of("webview", "webview-missing-page");

    @Test
    public void fx(QuarkusMainLauncher launcher) {
        launchChecks(launcher);
    }

    /**
     * Launches the checks : the output is in the message of every failed assertion (the exit code included).
     */
    LaunchResult launchChecks(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("fx");
        String output = result.getOutput() + "\n" + result.getErrorOutput();
        assertEquals(0, result.exitCode(), "exit code\n" + output);
        assertFalse(output.contains(" FAILED "), output);
        int ok = 0;
        for (String check : CHECKS) {
            if (output.contains("RESULT " + check + " OK")) {
                ok++;
            } else {
                assertTrue(WEBVIEW_CHECKS.contains(check) && webViewMaySkip()
                        && output.contains("RESULT " + check + " SKIPPED"), check + "\n" + output);
            }
        }
        assertTrue(output.contains("SUMMARY ok=" + ok + " skipped=" + (CHECKS.size() - ok) + " failed=0"), output);
        assertEquals(expectedMode(), value(output, "mode"), output);
        // The JavaFX version of the build : the default of Quarkus FX, JavaFX 25 on JDK 23 and later (javafx-25 profile)
        String javaFxVersion = System.getProperty("javafx.expected.version");
        if (javaFxVersion != null) {
            assertEquals(javaFxVersion, value(output, "javafx"), output);
        }
        return result;
    }

    /**
     * @return the mode of the application : here the JVM of the tests
     */
    String expectedMode() {
        return "jvm";
    }

    /**
     * @return whether the WebView checks may be skipped : never in JVM mode
     */
    boolean webViewMaySkip() {
        return false;
    }

    static String value(String output, String key) {
        Matcher matcher = Pattern.compile("\\b" + key + "=(\\S+)").matcher(output);
        assertTrue(matcher.find(), key + " not found in\n" + output);
        return matcher.group(1);
    }
}
