package io.quarkiverse.fx.deployment.fxviews;

import io.quarkiverse.fx.views.FxView;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.scene.Scene;

@FxView
@Singleton
public class SampleSceneController {

    @FXML
    Scene scene;
}
