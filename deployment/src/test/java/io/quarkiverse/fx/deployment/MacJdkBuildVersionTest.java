package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;

/**
 * Quarkus FX declares the macOS versions of the java launcher in a macOS native executable, unless Quarkus Desktop does
 * (quarkus-desktop-awt, quarkus-desktop-swing, quarkus-desktop-swt) : one writer.
 */
class MacJdkBuildVersionTest {

    @ParameterizedTest
    @ValueSource(strings = { "mac", "mac-aarch64" })
    void declaredOnMacWithoutQuarkusDesktop(String targetPlatform) {
        List<NativeImageSystemPropertyBuildItem> properties = macJdkBuildVersion(targetPlatform, "io.quarkus.rest");

        assertEquals(1, properties.size());
        assertEquals(FxClassesAndResources.MAC_JDK_BUILD_VERSION_PROPERTY, properties.get(0).getKey());
        assertEquals("true", properties.get(0).getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = { "win", "linux", "linux-aarch64" })
    void notDeclaredOnOtherPlatforms(String targetPlatform) {
        assertTrue(macJdkBuildVersion(targetPlatform).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { FxClassesAndResources.DESKTOP_AWT_CAPABILITY, FxClassesAndResources.DESKTOP_SWING_CAPABILITY,
            FxClassesAndResources.DESKTOP_SWT_CAPABILITY })
    void leftToQuarkusDesktop(String capability) {
        assertTrue(macJdkBuildVersion("mac-aarch64", capability).isEmpty());
    }

    private static List<NativeImageSystemPropertyBuildItem> macJdkBuildVersion(String targetPlatform,
            String... capabilities) {
        List<NativeImageSystemPropertyBuildItem> properties = new ArrayList<>();
        new QuarkusFxExtensionProcessor().macJdkBuildVersion(new FxTargetPlatformBuildItem(targetPlatform),
                new Capabilities(Set.of(capabilities)), properties::add);
        return properties;
    }
}
