package io.quarkiverse.fx.views;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javafx.fxml.FXML;

@Singleton
public class HotController {

    @Inject
    HotService hotService;

    @FXML
    private void handleClickMeAction() {
        System.out.println("onButtonClick");
        System.out.println(this);
        System.out.println(this.hotService);
        this.hotService.saySomething();
    }
}
