package io.quarkiverse.fx.graal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WebEngineUserStyleSheetSubstitutionsTest {

    @Test
    void userStyleSheetLocationSubstituted() {
        // Fails when a JavaFX version changes the userStyleSheetLocation property : the substitution would no longer apply
        assertTrue(new WebEngineUserStyleSheetSubstitutions.IsUserStyleSheetLocationSupported().getAsBoolean());
    }
}
