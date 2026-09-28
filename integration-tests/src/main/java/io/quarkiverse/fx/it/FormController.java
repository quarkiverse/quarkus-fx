package io.quarkiverse.fx.it;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * The controller of assets/form.fxml, loaded with the injected FXMLLoader : a bean (the controller factory of Quarkus FX),
 * registered for the reflection of FXMLLoader (@FXML field and method) in native executables.
 */
@Dependent
@RegisterForReflection
public class FormController {

    @Inject
    GreetingService greetingService;

    @FXML
    Label message;

    @FXML
    void initialize() {
        message.setText(greetingService.greet("FXMLLoader"));
    }
}
