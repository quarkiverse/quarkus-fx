package io.quarkiverse.fx.deployment.fxviews;

import io.quarkiverse.fx.views.FxView;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@FxView
@Singleton
public class SampleStageController {

    @FXML
    Stage stage;
}
