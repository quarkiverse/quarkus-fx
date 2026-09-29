package io.quarkiverse.fx.deployment.fxviews;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.deployment.FxTestConstants;
import io.quarkiverse.fx.deployment.base.FxTestBase;
import io.quarkiverse.fx.deployment.fxviews.controllers.ComponentWithStyleController;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkiverse.fx.views.StylesheetReloadStrategy;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.Platform;
import javafx.scene.Parent;

class FxStyleReloadTest extends FxTestBase {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                jar.addClass(ComponentWithStyleController.class);
                jar.addAsResource("views");
            })
            .overrideConfigKey("quarkus.fx.views-root", "views")
            .overrideConfigKey("quarkus.fx.stylesheet-reload-strategy", StylesheetReloadStrategy.ALWAYS.name())
            .overrideConfigKey("quarkus.fx.target-resources", "app-root/")
            .overrideConfigKey("quarkus.fx.source-resources", "src/test/resources/");

    @Inject
    FxViewRepository viewRepository;

    // Can't really test that modifications are effective,
    // but we can test that stylesheet has been replaced by the one from sources directory
    @Test
    void testLiveReload() throws Exception {

        this.startAndWait();

        // Check that substitution has been done : the stylesheet is replaced with Platform.runLater while the views load,
        // read it on the FX thread, after that task (runLater tasks run in the order they are posted)
        Parent component = this.viewRepository.getViewData("ComponentWithStyle").getRootNode();
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        Platform.runLater(() -> future.complete(List.copyOf(component.getStylesheets())));
        List<String> stylesheets = future.get(FxTestConstants.LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(1, stylesheets.size());
        Assertions.assertTrue(stylesheets.get(0).endsWith("src/test/resources/views/ComponentWithStyle.css"));
    }
}
