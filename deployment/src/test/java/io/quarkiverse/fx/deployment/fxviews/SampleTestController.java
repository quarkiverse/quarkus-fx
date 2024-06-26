package io.quarkiverse.fx.deployment.fxviews;

import io.quarkiverse.fx.context.FxScoped;
import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

@FxView
@FxScoped
public class SampleTestController {

    @FXML
    Label label;
}
