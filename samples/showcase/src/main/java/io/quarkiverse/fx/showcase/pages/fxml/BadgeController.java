package io.quarkiverse.fx.showcase.pages.fxml;

import java.util.ResourceBundle;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller of the included {@code loader-badge.fxml} : nested controllers are created by the same CDI controller
 * factory.
 */
@Dependent
@RegisterForReflection
public class BadgeController {

    @Inject
    GreetingService greetings;

    @FXML
    ResourceBundle resources;

    @FXML
    Label badgeText;

    @FXML
    Label ownerLabel;

    boolean initialized;
    String owner;

    @FXML
    void initialize() {
        initialized = true;
        badgeText.setText(resources.getString("badge.text"));
        ownerLabel.setText(resources.getString("badge.noOwner"));
    }

    void setOwner(String owner) {
        this.owner = owner;
        ownerLabel.setText(resources.getString("badge.owner").replace("{0}", owner));
    }

    String greeting() {
        return greetings.greet("include");
    }
}
