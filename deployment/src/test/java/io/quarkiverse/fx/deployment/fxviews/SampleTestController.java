package io.quarkiverse.fx.deployment.fxviews;

import jakarta.enterprise.context.Dependent;

import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

@FxView
@Dependent
public class SampleTestController {

    @FXML
    Label label;
}
