package io.quarkiverse.fx.views;

import java.security.SecureRandom;
import java.util.Random;

import jakarta.enterprise.context.Dependent;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@FxView("custom-sample")
@Dependent
public class YetAnotherSampleController {

    private static final Random RANDOM = new SecureRandom();

    @FXML
    Parent root;

    @FXML
    Label rollResultLabel;

    @FXML
    public void initialize() {
        Stage stage = new Stage();
        Scene scene = new Scene(this.root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleClickMeAction() {
        // Roll a d20
        int value = RANDOM.nextInt(0, 21);
        this.rollResultLabel.setText(String.valueOf(value));
    }
}
