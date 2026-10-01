package io.quarkiverse.fx.it;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkiverse.fx.views.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

/**
 * The controller of the view views/Main.fxml, loaded at startup by Quarkus FX (@FxView : the name of the class without
 * Controller), with its resource bundle views/Main.properties and its stylesheet views/Main.css.
 */
@FxView
@Dependent
public class MainController {

    static final String VIEW = "Main";

    @Inject
    GreetingService greetingService;

    @FXML
    Label greeting;

    @FXML
    Region tile;

    @FXML
    ImageView logo;

    String initializedWith;

    @FXML
    void initialize() {
        initializedWith = greetingService.greet(greeting.getText());
    }
}
