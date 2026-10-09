package io.quarkiverse.fx.showcase.pages.fxml;

import java.net.URL;
import java.util.ResourceBundle;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/**
 * Controller of {@code loader.fxml}, created by the CDI controller factory of the quarkus-fx {@code FXMLLoader}
 * producer (hence the CDI injection). FXMLLoader injects the {@code @FXML} fields and calls the {@code @FXML} methods
 * reflectively.
 */
@Dependent
@RegisterForReflection
public class LoaderDemoController {

    @Inject
    GreetingService greetings;

    @FXML
    ResourceBundle resources;

    @FXML
    URL location;

    @FXML
    TextField nameField;

    @FXML
    Label greetingLabel;

    @FXML
    Label serviceLabel;

    @FXML
    Button incrementButton;

    @FXML
    Button resetButton;

    @FXML
    Button wideButton;

    @FXML
    CheckBox enableBox;

    @FXML
    ToggleGroup sizeGroup;

    @FXML
    ComboBox<String> languageBox;

    @FXML
    Slider levelSlider;

    @FXML
    ProgressBar levelBar;

    @FXML
    Label clicksLabel;

    @FXML
    TableView<Person> peopleTable;

    @FXML
    ColorSwatch primarySwatch;

    /** Root node of the {@code <fx:include fx:id="badge">}. */
    @FXML
    HBox badge;

    /** Controller of the {@code <fx:include fx:id="badge">} ({@code fx:id} + "Controller"). */
    @FXML
    BadgeController badgeController;

    private final IntegerProperty clicks = new SimpleIntegerProperty(this, "clicks");
    private final StringProperty clickText = new SimpleStringProperty(this, "clickText", "");

    boolean initialized;
    int handlerCalls;
    String lastEventSource;

    /**
     * Read by the {@code ${controller.clickText}} expression binding.
     */
    public StringProperty clickTextProperty() {
        return clickText;
    }

    public String getClickText() {
        return clickText.get();
    }

    public IntegerProperty clicksProperty() {
        return clicks;
    }

    @FXML
    private void initialize() {
        initialized = true;
        String pattern = resources.getString("loader.clicks");
        clickText.bind(Bindings.createStringBinding(() -> pattern.replace("{0}", Integer.toString(clicks.get())), clicks));
        serviceLabel.setText(greetings.greet("CDI"));
        if (badgeController != null) {
            badgeController.setOwner(getClass().getSimpleName());
        }
    }

    @FXML
    private void increment(ActionEvent event) {
        handlerCalls++;
        lastEventSource = event.getSource() instanceof Button button ? button.getId() : String.valueOf(event.getSource());
        clicks.set(clicks.get() + 1);
    }

    @FXML
    private void reset() {
        handlerCalls++;
        clicks.set(0);
    }
}
