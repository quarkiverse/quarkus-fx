package io.quarkiverse.fx.deployment.fxviews.controllers;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.scene.Scene;

@FxView
@Singleton
public class SampleSceneController {

    @FXML
    public Scene scene;
}
