package io.quarkiverse.fx.deployment.fxviews;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.deployment.FxTestBase;
import io.quarkiverse.fx.deployment.fxviews.controllers.RootResourceController;
import io.quarkiverse.fx.views.FxViewData;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkus.test.QuarkusUnitTest;
import javafx.scene.layout.VBox;

class FxViewRootResourceTest extends FxTestBase {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                jar.addClass(RootResourceController.class);
                jar.addAsResource("RootResource.fxml");
            });

    @Inject
    FxViewRepository viewRepository;

    @Test
    void test() {

        this.startAndWait();

        FxViewData data = this.viewRepository.getViewData("RootResource");
        Assertions.assertNotNull(data);

        VBox root = data.getRootNode();
        Assertions.assertNotNull(root);

        RootResourceController controller = data.getController();
        Assertions.assertNotNull(data);
        Assertions.assertNotNull(controller.label);
    }
}
