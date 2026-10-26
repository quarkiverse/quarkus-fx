package io.quarkiverse.fx.deployment.fxviews;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.views.FxView;
import io.quarkus.test.QuarkusUnitTest;

class FxViewDuplicateNameTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(FirstController.class, SecondController.class))
            .assertException(failure -> {
                while (failure.getCause() != null) {
                    failure = failure.getCause();
                }
                assertTrue(failure.getMessage().contains("View 'duplicate' is registered by both"));
                assertTrue(failure.getMessage().contains(FirstController.class.getName()));
                assertTrue(failure.getMessage().contains(SecondController.class.getName()));
            });

    @Test
    void rejectsAmbiguousViewName() {
        fail("Duplicate view names should fail augmentation");
    }

    @FxView("duplicate")
    public static class FirstController {
    }

    @FxView("duplicate")
    public static class SecondController {
    }
}
