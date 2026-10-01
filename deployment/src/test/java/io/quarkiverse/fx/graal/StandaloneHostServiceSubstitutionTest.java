package io.quarkiverse.fx.graal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StandaloneHostServiceSubstitutionTest {

    @Test
    void getCodeBaseSubstituted() {
        // Fails when a JavaFX version changes HostServicesDelegate : the substitution would no longer apply
        assertTrue(new Target_com_sun_javafx_application_HostServicesDelegate_StandaloneHostService.IsStandaloneHostService()
                .getAsBoolean());
    }
}
