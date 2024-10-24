package io.quarkiverse.fx.deployment.fxviews;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkiverse.fx.deployment.FxTestConstants;
import io.quarkiverse.fx.views.FxViewData;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

class FxViewRootResourceTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                jar.addClass(RootResourceController.class);
                jar.addAsResource("RootResource.fxml");
            });

    private static final AtomicBoolean eventObserved = new AtomicBoolean(false);

    @Inject
    FxViewRepository fxViewRepository;

    @Test
    void test() {

        // Non-blocking launch
        CompletableFuture.runAsync(() -> Quarkus.run(QuarkusFxApplication.class));

        await()
                .atMost(FxTestConstants.LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .until(eventObserved::get);

        FxViewData data = this.fxViewRepository.getViewData("RootResource");
        Assertions.assertNotNull(data);

        VBox root = data.getRootNode();
        Assertions.assertNotNull(root);

        RootResourceController controller = data.getController();
        Assertions.assertNotNull(data);
        Assertions.assertNotNull(controller.label);
    }

    void observeEvent(@Observes final FxPostStartupEvent event) {
        eventObserved.set(true);
    }
}
