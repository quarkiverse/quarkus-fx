package io.quarkiverse.fx.deployment.fxviews;

import io.quarkiverse.fx.views.FxView;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

@FxView
@Singleton
public class SubSampleTestController {

    @FXML
    Button button;
}
