package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.smallrye.common.os.OS;

/**
 * The platform of the native executable : the build host, or Linux for a container build.
 */
class FxTargetPlatformTest {

    @Test
    void hostPlatform() {
        assertEquals("win", QuarkusFxExtensionProcessor.targetPlatform(OS.WINDOWS, "amd64", false));
        assertEquals("mac", QuarkusFxExtensionProcessor.targetPlatform(OS.MAC, "x86_64", false));
        assertEquals("mac-aarch64", QuarkusFxExtensionProcessor.targetPlatform(OS.MAC, "aarch64", false));
        assertEquals("linux", QuarkusFxExtensionProcessor.targetPlatform(OS.LINUX, "amd64", false));
        assertEquals("linux-aarch64", QuarkusFxExtensionProcessor.targetPlatform(OS.LINUX, "aarch64", false));
    }

    @Test
    void containerBuild() {
        // a Linux executable, on the architecture of the host : the Linux lists, the Linux JavaFX artifacts, and no macOS
        // steps (the JavaFX version check, the macOS versions of the java launcher)
        assertEquals("linux", QuarkusFxExtensionProcessor.targetPlatform(OS.WINDOWS, "amd64", true));
        assertEquals("linux", QuarkusFxExtensionProcessor.targetPlatform(OS.MAC, "x86_64", true));
        assertEquals("linux-aarch64", QuarkusFxExtensionProcessor.targetPlatform(OS.MAC, "aarch64", true));
        assertEquals("linux", QuarkusFxExtensionProcessor.targetPlatform(OS.LINUX, "amd64", true));
    }
}
