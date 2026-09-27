package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JavaFxFeatureVersionTest {

    @Test
    void featureVersion() {
        assertEquals(25, QuarkusFxExtensionProcessor.javaFxFeatureVersion("25.0.4"));
        assertEquals(22, QuarkusFxExtensionProcessor.javaFxFeatureVersion("22.0.2"));
        assertEquals(27, QuarkusFxExtensionProcessor.javaFxFeatureVersion("27"));
        assertEquals(24, QuarkusFxExtensionProcessor.javaFxFeatureVersion("24-ea+5"));
        assertEquals(0, QuarkusFxExtensionProcessor.javaFxFeatureVersion(""));
        assertEquals(0, QuarkusFxExtensionProcessor.javaFxFeatureVersion("${javafx.version}"));
        assertEquals(0, QuarkusFxExtensionProcessor.javaFxFeatureVersion("LATEST"));
    }
}
