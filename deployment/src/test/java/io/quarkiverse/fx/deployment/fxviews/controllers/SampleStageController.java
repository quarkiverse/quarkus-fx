package io.quarkiverse.fx.deployment.fxviews.controllers;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@FxView
@Singleton
public class SampleStageController {

    @FXML
    public Stage stage;
}
