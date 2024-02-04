package io.quarkiverse.fx.sample.time;

import java.net.URL;
import java.time.LocalTime;
import java.util.ResourceBundle;

import jakarta.enterprise.context.Dependent;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

@Dependent
public class TimeController implements Initializable {

    @FXML
    Label label;

    @FXML
    void update() {
        label.setText(LocalTime.now().toString());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        update();
    }
}
