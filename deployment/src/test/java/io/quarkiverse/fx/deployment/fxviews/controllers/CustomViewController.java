package io.quarkiverse.fx.deployment.fxviews.controllers;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

@Singleton
@FxView("custom-view")
public class CustomViewController {
    @FXML
    public Label label;
}
