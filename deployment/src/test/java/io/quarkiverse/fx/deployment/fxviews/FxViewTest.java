package io.quarkiverse.fx.deployment.fxviews;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkiverse.fx.views.FxViewData;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.QuarkusUnitTest;
import javafx.collections.ObservableList;

class FxViewTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                jar.addClasses(SampleTestController.class, SubSampleTestController.class);
                jar.addAsResource("SampleTest.fxml");
                jar.addAsResource("SampleTest.properties");
                jar.addAsResource("SampleTest.css");
                jar.addAsResource("SubSampleTest.fxml");
            });

    @Inject
    FxViewRepository viewRepository;

    @Inject
    FxStartupLatch startupLatch;

    @Inject
    SubSampleTestController subSampleTestController;

    @Test
    void testFxView() throws InterruptedException {

        // Non-blocking launch
        CompletableFuture.runAsync(() -> Quarkus.run(QuarkusFxApplication.class));

        // Wait for readiness
        this.startupLatch.await();

        FxViewData viewData = this.viewRepository.getViewData("SampleTest");
        Assertions.assertNotNull(viewData);

        SampleTestController controller = viewData.getController();
        Assertions.assertNotNull(controller);
        Assertions.assertNotNull(controller.label);

        String text = controller.label.getText();
        Assertions.assertEquals("Bonjour", text);

        ObservableList<String> stylesheets = viewData.getRootNode().getStylesheets();
        Assertions.assertEquals(1, stylesheets.size());
        URI uri = URI.create(stylesheets.get(0));
        Path path = Path.of(uri);
        Assertions.assertEquals("SampleTest.css", path.getFileName().toString());

        Assertions.assertNotNull(this.subSampleTestController.button);
    }
}
