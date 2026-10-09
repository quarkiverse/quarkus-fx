package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.ChainBuildException;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Without the public API of quarkus-desktop-swt on the class path, the overridable producer of the main application of
 * Quarkus FX is registered : next to the one of Quarkus Desktop SWT, Quarkus rejects both. This is what
 * {@link QuarkusDesktopSwtPresent} prevents (see {@link SwtEmbeddingTest}).
 */
class SwtMainConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClass(SwtEmbeddingTest.SwtApplicationStandIn.class))
            .addBuildChainCustomizer(SwtEmbeddingTest::produceSwtApplication)
            .assertException(failure -> {
                Throwable cause = failure;
                while (cause != null && !(cause instanceof ChainBuildException)) {
                    cause = cause.getCause();
                }
                assertTrue(cause != null && cause.getMessage().contains("Multiple overridable producers"),
                        String.valueOf(failure));
            });

    @Test
    void rejected() {
        // Not run : the application does not build
    }
}
