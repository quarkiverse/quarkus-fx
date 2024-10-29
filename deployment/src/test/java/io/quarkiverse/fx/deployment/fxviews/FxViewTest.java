package io.quarkiverse.fx.deployment.fxviews;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkiverse.fx.deployment.FxTestConstants;
import io.quarkiverse.fx.deployment.fxviews.controllers.ComponentWithStyleController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SampleDialogController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SampleSceneController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SampleStageController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SampleTestController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SubSampleTestController;
import io.quarkiverse.fx.views.FxViewData;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.QuarkusUnitTest;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

class FxViewTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                jar.addClasses(
                        SampleTestController.class,
                        SubSampleTestController.class,
                        SampleStageController.class,
                        SampleDialogController.class,
                        SampleSceneController.class,
                        ComponentWithStyleController.class);
                jar.addAsResource("views");
            })
            .overrideConfigKey("quarkus.fx.views-root", "views");

    @Inject
    FxViewRepository viewRepository;

    @Inject
    FxStartupLatch startupLatch;

    @Inject
    SubSampleTestController subSampleTestController;

    @Inject
    SampleStageController sampleStageController;

    @Inject
    SampleDialogController sampleDialogController;

    @Inject
    SampleSceneController sampleSceneController;

    private static final AtomicBoolean eventObserved = new AtomicBoolean(false);

    @Test
    void testFxView() throws InterruptedException {

        // Non-blocking launch
        CompletableFuture.runAsync(() -> Quarkus.run(QuarkusFxApplication.class));

        // Wait for readiness
        this.startupLatch.await();

        await()
                .atMost(FxTestConstants.LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .until(eventObserved::get);

        Stage primaryStage = this.viewRepository.getPrimaryStage();
        Assertions.assertNotNull(primaryStage);

        FxViewData viewData = this.viewRepository.getViewData("SampleTest");
        Assertions.assertNotNull(viewData);

        SampleTestController controller = viewData.getController();
        Assertions.assertNotNull(controller);
        Assertions.assertNotNull(controller.label);

        String text = controller.label.getText();
        Assertions.assertEquals("Bonjour", text);

        Assertions.assertNotNull(this.subSampleTestController.button);
        Assertions.assertNotNull(this.sampleStageController.stage);
        Assertions.assertNotNull(this.sampleDialogController.dialog);
        Assertions.assertNotNull(this.sampleSceneController.scene);

        // Component with stylesheet
        FxViewData componentWithStyle = this.viewRepository.getViewData("ComponentWithStyle");
        Assertions.assertNotNull(componentWithStyle);
        Assertions.assertInstanceOf(BorderPane.class, componentWithStyle.getRootNode());
        BorderPane pane = componentWithStyle.getRootNode();
        ObservableList<String> componentStylesheets = pane.getStylesheets();
        Assertions.assertEquals(1, componentStylesheets.size());
    }

    void observeEvent(@Observes final FxPostStartupEvent event) {
        eventObserved.set(true);
    }
}
