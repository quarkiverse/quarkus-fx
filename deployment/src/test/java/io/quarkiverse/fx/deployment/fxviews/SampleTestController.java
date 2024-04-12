package io.quarkiverse.fx.deployment.fxviews;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

@FxView
@Singleton
public class SampleTestController {

    @FXML
    Label label;
}
