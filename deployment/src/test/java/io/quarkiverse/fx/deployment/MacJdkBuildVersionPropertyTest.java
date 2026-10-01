package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class MacJdkBuildVersionPropertyTest {

    @Test
    void propertyReadByMacBuildVersion() throws Exception {
        Field property = Class.forName("io.quarkiverse.fx.graal.MacBuildVersion").getDeclaredField("PROPERTY");
        property.setAccessible(true);
        assertEquals(FxClassesAndResources.MAC_JDK_BUILD_VERSION_PROPERTY, property.get(null));
    }
}
