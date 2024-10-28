package io.quarkiverse.fx.deployment.fxviews.controllers;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

@FxView
@Singleton
public class SampleTestController {

    @FXML
    public Label label;

    boolean initialized;

    @FXML
    void initialize() {
        this.runOnFx();
    }

    @RunOnFxThread
    void runOnFx() {
        this.initialized = true;
    }
}
