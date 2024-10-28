package io.quarkiverse.fx.deployment.fxviews.controllers;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Dialog;

@FxView
@Singleton
public class SampleDialogController {

    @FXML
    public Dialog<?> dialog;
}
