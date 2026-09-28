package io.quarkiverse.fx.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import io.quarkus.test.junit.main.QuarkusMainLauncher;

/**
 * Runs the checks with the artifact of the build : the native executable with -Dnative.
 */
@QuarkusMainIntegrationTest
public class FxItIT extends FxItTest {

    @Test
    @Override
    public void fx(QuarkusMainLauncher launcher) {
        LaunchResult result = launchChecks(launcher);
        if (isNative()) {
            String output = result.getOutput();
            // HostServices.getCodeBase() : the directory of the executable, as the directory of the jar in JVM mode
            Path codeBase = Path.of(URI.create(value(output, "codeBase")));
            Path directory = nativeExecutable().toAbsolutePath().getParent();
            try {
                assertTrue(Files.isSameFile(directory, codeBase), "code base " + codeBase + " instead of " + directory);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // Class path resources have resource: URLs, WebEngine.getLocation() returns the resource: URL of the page
            assertEquals("resource", value(output, "urlScheme"), output);
            if (output.contains("RESULT webview OK")) {
                assertEquals("resource", value(output, "pageLocation"), output);
            }
        }
    }

    @Override
    String expectedMode() {
        return isNative() ? "native" : "jvm";
    }

    @Override
    boolean webViewMaySkip() {
        return isNative() && OS.MAC.isCurrentOs();
    }

    /**
     * The type of the artifact of the build that the integration tests run (target/quarkus-artifact.properties).
     */
    static boolean isNative() {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("target", "quarkus-artifact.properties"))) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "native".equals(properties.getProperty("type"));
    }

    static Path nativeExecutable() {
        String path = System.getProperty("native.image.path");
        return Path.of(OS.WINDOWS.isCurrentOs() && !path.endsWith(".exe") ? path + ".exe" : path);
    }
}
