package io.quarkiverse.fx.graal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocatorSubstitutionTest {

    @Test
    void locatorSubstitutedOnMac() {
        // The AVFoundation engine is only in the macOS javafx-media jar (javafx-web depends on javafx-media).
        // Fails on macOS when a JavaFX version changes the Locator : the substitution would no longer apply
        boolean mac = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");
        assertEquals(mac, new Target_com_sun_media_jfxmedia_locator_Locator.IsAvFoundationEngine().getAsBoolean());
    }
}
