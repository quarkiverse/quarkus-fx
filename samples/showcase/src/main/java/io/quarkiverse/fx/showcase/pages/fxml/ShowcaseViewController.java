package io.quarkiverse.fx.showcase.pages.fxml;

import java.net.URL;
import java.util.ResourceBundle;

import jakarta.enterprise.context.Dependent;

import io.quarkiverse.fx.views.FxView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * quarkus-fx conventional view : {@code ShowcaseViewController} -> {@code fxviews/ShowcaseView.fxml} and the
 * {@code fxviews/ShowcaseView.properties} bundle, loaded at startup into the {@code FxViewRepository}.
 * <p>
 * Deliberately not annotated with {@code @RegisterForReflection} : quarkus-fx registers {@code @FxView} classes itself.
 */
@FxView
@Dependent
public class ShowcaseViewController {

    @FXML
    ResourceBundle resources;

    @FXML
    URL location;

    @FXML
    VBox root;

    @FXML
    Label titleLabel;

    @FXML
    Label counterLabel;

    @FXML
    Button actionButton;

    @FXML
    ProgressBar progress;

    boolean initialized;
    int clicks;

    @FXML
    void initialize() {
        initialized = true;
    }

    @FXML
    void onAction(ActionEvent event) {
        clicks++;
        counterLabel.setText(resources.getString("view.clicks").replace("{0}", Integer.toString(clicks)));
    }
}
