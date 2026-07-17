package io.quarkiverse.fx.deployment.fxviews;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.deployment.FxTestConstants;
import io.quarkiverse.fx.deployment.base.FxTestBase;
import io.quarkiverse.fx.deployment.fxviews.controllers.ComponentWithStyleController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SampleDialogController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SampleSceneController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SampleStageController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SampleTestController;
import io.quarkiverse.fx.deployment.fxviews.controllers.SubSampleTestController;
import io.quarkiverse.fx.livereload.FxLiveReloadState;
import io.quarkiverse.fx.views.FxViewData;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

class FxViewTest extends FxTestBase {

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
    SubSampleTestController subSampleTestController;

    @Inject
    SampleStageController sampleStageController;

    @Inject
    SampleDialogController sampleDialogController;

    @Inject
    SampleSceneController sampleSceneController;

    @Test
    void testFxView() throws Exception {

        this.startAndWait();

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

        BorderPane replacement = new BorderPane();
        CompletableFuture<Void> replacementDone = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                Scene scene = new Scene(viewData.getRootNode());
                primaryStage.setScene(scene);

                FxLiveReloadState.replaceView("SampleTest", replacement);

                Assertions.assertSame(scene, primaryStage.getScene());
                Assertions.assertSame(replacement, scene.getRoot());

                Stage retainedStage = new Stage();
                Scene oldScene = new Scene(new BorderPane());
                retainedStage.setScene(oldScene);
                FxLiveReloadState.registerView("ReloadableScene", oldScene);
                Scene reloadedScene = new Scene(new BorderPane());
                FxLiveReloadState.replaceView("ReloadableScene", reloadedScene);
                Assertions.assertSame(reloadedScene, retainedStage.getScene());

                BorderPane attachedReplacement = new BorderPane();
                Stage temporaryStage = new Stage();
                temporaryStage.setScene(new Scene(attachedReplacement));
                boolean attachedRootReplaced = FxLiveReloadState.replaceView("SampleTest", attachedReplacement);
                Assertions.assertFalse(attachedRootReplaced);
                Assertions.assertSame(replacement, scene.getRoot());
                replacementDone.complete(null);
            } catch (Throwable t) {
                replacementDone.completeExceptionally(t);
            }
        });
        replacementDone.get(FxTestConstants.LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }
}
