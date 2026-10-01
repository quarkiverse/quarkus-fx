package io.quarkiverse.fx.showcase.pages.fxml;

import java.util.ResourceBundle;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkiverse.fx.views.FxView;
import io.quarkiverse.fx.views.FxViewConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Conventional view with a custom name ({@code @FxView("StatusCard")}, no {@code Controller} suffix) whose files are in
 * the {@code fxviews/StatusCard/} sub-directory. The presenter is a CDI bean : the extension configuration is injected.
 */
@FxView("StatusCard")
@Dependent
public class StatusCardPresenter {

    @Inject
    FxViewConfig config;

    @Inject
    GreetingService greetings;

    @FXML
    ResourceBundle resources;

    @FXML
    Label viewsRootValue;

    @FXML
    Label reloadValue;

    @FXML
    Label greetingValue;

    @FXML
    Label bundleValue;

    boolean initialized;

    @FXML
    void initialize() {
        initialized = true;
        viewsRootValue.setText(config.viewsRoot());
        reloadValue.setText(String.valueOf(config.stylesheetReloadStrategy()));
        greetingValue.setText(greetings.greet("presenter"));
        bundleValue.setText(resources.getString("status.bundleValue"));
    }
}
