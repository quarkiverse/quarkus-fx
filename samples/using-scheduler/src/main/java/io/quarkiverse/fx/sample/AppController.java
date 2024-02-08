package io.quarkiverse.fx.sample;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;

@Dependent
public class AppController {

    @Inject
    Instance<FXMLLoader> fxmlLoaderInstance;
    @FXML
    private Label timeLabel;

    @FXML
    private void initialize() {
        //
    }

    public void onMessage(String timeString) {
        Platform.runLater(() -> timeLabel.setText(timeString));
    }
}
