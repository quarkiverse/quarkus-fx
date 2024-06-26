package io.quarkiverse.fx.views;

import io.quarkiverse.fx.context.FxScoped;
import javafx.fxml.FXML;

@FxView
@FxScoped
public class SampleController {

    @FXML
    private void onButtonClick() {
        System.out.println("onButtonClick");
    }
}
