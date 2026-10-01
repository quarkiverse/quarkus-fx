package io.quarkiverse.fx.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Checks the file of the macOS native executable (skipped when the integration tests run against the jar).
 */
@EnabledOnOs(OS.MAC)
public class NativeExecutableIT {

    private static Path executable;

    @BeforeAll
    static void executable() {
        assumeTrue(FxItIT.isNative(), "no native executable : the integration tests run against the jar");
        executable = Path.of(System.getProperty("native.image.path"));
        assertTrue(Files.isRegularFile(executable), executable + " not found");
    }

    /**
     * The executable declares the minimum macOS version and the SDK version of the java launcher of the GraalVM that
     * built it (io.quarkiverse.fx.graal.MacBuildVersion) : the window style of JVM mode, and it starts on the macOS
     * versions of the JDK, not only on the version of the SDK of the Xcode tools.
     */
    @Test
    void macExecutableHasTheBuildVersionOfTheJavaLauncher() throws IOException, InterruptedException {
        // the JDK of the native build : GRAALVM_HOME first, as Quarkus finds native-image
        String graalvmHome = System.getenv("GRAALVM_HOME");
        Path jdkHome = graalvmHome != null && !graalvmHome.isBlank() && Files.isDirectory(Path.of(graalvmHome))
                ? Path.of(graalvmHome)
                : Path.of(System.getProperty("java.home"));
        assertEquals(buildVersion(jdkHome.resolve("bin").resolve("java")), buildVersion(executable));
    }

    /**
     * Quarkus FX serves the main run loop on the first thread of the process (MacMainRunLoop) : the executable links
     * CoreFoundation.
     */
    @Test
    void macExecutableLinksCoreFoundation() throws IOException, InterruptedException {
        String libraries = run("otool", "-L", executable.toString());
        assertTrue(libraries.contains("/CoreFoundation.framework/"), "CoreFoundation not linked :\n" + libraries);
    }

    /**
     * The minimum macOS version and the SDK version of a Mach-O file ({@code otool -l}).
     */
    private static List<String> buildVersion(Path file) throws IOException, InterruptedException {
        List<String> versions = new ArrayList<>();
        String command = "";
        for (String line : run("otool", "-l", file.toString()).lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("cmd ")) {
                command = trimmed.substring(4);
            } else if (command.equals("LC_BUILD_VERSION") && (trimmed.startsWith("minos ") || trimmed.startsWith("sdk "))
                    || command.equals("LC_VERSION_MIN_MACOSX")
                            && (trimmed.startsWith("version ") || trimmed.startsWith("sdk "))) {
                // not the versions of the tools that built the file (LC_BUILD_VERSION "tool ... version ...")
                versions.add(trimmed);
            }
        }
        assertFalse(versions.isEmpty(), "no build version in " + file);
        return versions;
    }

    /**
     * The standard output of a command, which must succeed.
     */
    private static String run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), String.join(" ", command) + " failed :\n" + output);
        return output;
    }
}
