package io.quarkiverse.fx.sample.date;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import jakarta.enterprise.context.Dependent;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

@Dependent
public class DateController implements Initializable {

    @FXML
    Label label;

    @FXML
    void update() {
        label.setText(LocalDate.now().toString());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        update();

    }
}
