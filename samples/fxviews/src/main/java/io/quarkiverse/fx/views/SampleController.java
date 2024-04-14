package io.quarkiverse.fx.views;

import jakarta.enterprise.context.Dependent;

import javafx.fxml.FXML;

@FxView
@Dependent
public class SampleController {

    @FXML
    private void onButtonClick() {
        System.out.println("onButtonClick");
    }
}
