package io.quarkiverse.fx.deployment.fxviews;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Dialog;

@FxView
@Singleton
public class SampleDialogController {

    @FXML
    Dialog<?> dialog;
}
