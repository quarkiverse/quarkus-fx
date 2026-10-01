package io.quarkiverse.fx.showcase.pages.fxml;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

/**
 * Controller of {@code plain-controller.fxml}, which is loaded by a plain {@code new FXMLLoader(url)} : no controller
 * factory, so JavaFX instantiates this class itself (reflective no-arg constructor) and binds the {@code onAction}
 * handler and the {@code onValueChange} property listener reflectively. Not a CDI bean.
 */
@RegisterForReflection
public class PlainController {

    @FXML
    Button button;

    @FXML
    Slider slider;

    @FXML
    Label status;

    final List<String> calls = new ArrayList<>();
    boolean initialized;

    public PlainController() {
    }

    @FXML
    void initialize() {
        initialized = true;
    }

    @FXML
    void fired(ActionEvent event) {
        calls.add("fired(" + ((Button) event.getSource()).getId() + ")");
        status.setText("onAction called " + calls.stream().filter(c -> c.startsWith("fired")).count() + "x");
    }

    /**
     * {@code onValueChange="#valueChanged"} : a ChangeListener on the {@code value} property.
     */
    @FXML
    void valueChanged(ObservableValue<? extends Number> value, Number oldValue, Number newValue) {
        calls.add("valueChanged(" + oldValue + " -> " + newValue + ")");
    }
}
