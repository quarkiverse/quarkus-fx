package io.quarkiverse.fx.views;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

@FxView("custom-sample")
@Dependent
public class YetAnotherSampleController {

    @Inject
    RollService rollService;

    @FXML
    Label rollResultLabel;

    @FXML
    private void handleClickMeAction() {
        // Roll a d20
        int value = this.rollService.roll();
        this.rollResultLabel.setText(String.valueOf(value));
    }
}
