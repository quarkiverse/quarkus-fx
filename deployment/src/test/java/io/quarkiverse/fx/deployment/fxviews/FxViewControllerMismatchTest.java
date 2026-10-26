package io.quarkiverse.fx.deployment.fxviews;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxLifecycle;
import io.quarkiverse.fx.views.FxView;
import io.quarkus.test.QuarkusUnitTest;

class FxViewControllerMismatchTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(ExpectedController.class, ActualController.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            <?xml version="1.0" encoding="UTF-8"?>
                                            <?import javafx.scene.layout.VBox?>
                                            <VBox xmlns:fx="http://javafx.com/fxml/1"
                                                fx:controller="io.quarkiverse.fx.deployment.fxviews.FxViewControllerMismatchTest$ActualController"/>
                                            """),
                            "Mismatch.fxml"));

    @Inject
    FxLifecycle lifecycle;

    @Test
    void rejectsControllerDifferentFromDeclaredType() {
        Throwable failure = assertThrows(IllegalStateException.class, () -> lifecycle.start());
        while (failure.getCause() != null) {
            failure = failure.getCause();
        }
        assertTrue(failure.getMessage().contains("View 'Mismatch' declares controller"));
        assertTrue(failure.getMessage().contains(ExpectedController.class.getName()));
        assertTrue(failure.getMessage().contains(ActualController.class.getName()));
    }

    @Singleton
    @FxView("Mismatch")
    public static class ExpectedController {
    }

    @Singleton
    public static class ActualController {
    }
}
