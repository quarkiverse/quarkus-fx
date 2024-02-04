package io.quarkiverse.fx.deployment.fxmlview;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxApplication;
import io.quarkiverse.fx.PrimaryStage;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@Disabled // Disabled until #9 is fixed
public class FXMLViewTest {

    private static final int LAUNCH_TIMEOUT_MS = 1_000;
    private static final AtomicBoolean primaryStageObserved = new AtomicBoolean(false);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                jar.addClass(ContactCardView.class);
                jar.addAsResource("io/quarkiverse/fx/deployment/fxmlview/ContactCardView.fxml");
                jar.addAsResource("io/quarkiverse/fx/deployment/fxmlview/ContactCardView.css");
                jar.addAsResource("io/quarkiverse/fx/deployment/fxmlview/ContactCardView.properties");
            });

    @Inject
    ContactCardView contactCardView;

    @Test
    void testContactView() {
        CompletableFuture.runAsync(() -> Application.launch(FxApplication.class));
        await().atMost(LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(primaryStageObserved::get);

        // fxml should be loaded into a node
        VBox node = contactCardView.getNode();
        Assertions.assertNotNull(node);

        // keys should be resolved from resource bundle
        for (Node child : node.getChildren()) {
            if (child.getId().equals("location")) {
                Assertions.assertEquals("Some location", ((Label) child).getText());
            }
        }

        // stylesheet should be added to scene upon adding the node to them
        Scene scene = new Scene(node);
        boolean contactViewStylesheetLoaded = scene.getStylesheets().stream()
                .anyMatch(stylesheet -> stylesheet.contains("ContactCardView"));
        Assertions.assertTrue(contactViewStylesheetLoaded);

    }

    void observePrimaryStage(@Observes @PrimaryStage final Stage stage) {
        Assertions.assertNotNull(stage);
        primaryStageObserved.set(true);
    }
}
