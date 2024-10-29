package io.quarkiverse.fx.deployment.fxviews;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkiverse.fx.deployment.FxTestConstants;
import io.quarkiverse.fx.deployment.fxviews.controllers.ComponentWithStyleController;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkiverse.fx.views.StylesheetReloadStrategy;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

class FxStyleReloadTest {

    @RegisterExtension
    private static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                jar.addClass(ComponentWithStyleController.class);
                jar.addAsResource("views");
            })
            .overrideConfigKey("quarkus.fx.views-root", "views")
            .overrideConfigKey("quarkus.fx.stylesheet-reload-strategy", StylesheetReloadStrategy.ALWAYS.name())
            .overrideConfigKey("quarkus.fx.target-classes", "app-root/views/")
            .overrideConfigKey("quarkus.fx.source-resources", "target/");

    private static final AtomicBoolean eventObserved = new AtomicBoolean(false);

    @Inject
    FxViewRepository viewRepository;

    @Inject
    FxStartupLatch startupLatch;

    private Path copy;

    @BeforeEach
    void before() {

        InputStream input = this.getClass().getClassLoader().getResourceAsStream("/views/ComponentWithStyle.css");
        Assertions.assertNotNull(input);

        // Copy the resource to a temp file
        try {
            this.copy = Path.of("ComponentWithStyle.css");
            Files.copy(input, this.copy, StandardCopyOption.REPLACE_EXISTING);

            // Read file content
            String content = String.join("\n", Files.readAllLines(this.copy));

            // Replace text as needed
            String modifiedContent = content.replace("blue", "red");

            // Write the modified content back to the temp file or a different location
            Files.writeString(this.copy, modifiedContent, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    @AfterEach
    void after() {
      try {
        Files.delete(this.copy);
      } catch (IOException e) {
          Assertions.fail(e);
      }
    }

    @Test
    void testLiveReload() throws InterruptedException {

        // Non-blocking launch
        CompletableFuture.runAsync(() -> Quarkus.run(QuarkusFxApplication.class));

        // Wait for readiness
        this.startupLatch.await();

        await()
                .atMost(FxTestConstants.LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .until(eventObserved::get);

        Stage primaryStage = this.viewRepository.getPrimaryStage();
        Assertions.assertNotNull(primaryStage);

        // Check that substitution has been done
        Parent component = this.viewRepository.getViewData("ComponentWithStyle").getRootNode();
        ObservableList<String> stylesheets = component.getStylesheets();
        Assertions.assertEquals(1, stylesheets.size());
        Assertions.assertTrue(stylesheets.get(0).endsWith("target/ComponentWithStyle.css"));
    }

    void observeEvent(@Observes final FxPostStartupEvent event) {
        eventObserved.set(true);
    }
}
