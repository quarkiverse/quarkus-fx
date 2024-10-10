package io.quarkiverse.fx.deployment.fxviews;

import io.quarkiverse.fx.views.FxView;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;

@FxView
@Singleton
public class SampleDialogController {

    @FXML
    Dialog<?> dialog;
}
