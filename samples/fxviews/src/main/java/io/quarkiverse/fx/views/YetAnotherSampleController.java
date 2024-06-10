package io.quarkiverse.fx.views;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@FxView("custom-sample")
@Dependent
public class YetAnotherSampleController {

    @Inject
    RollService rollService;

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
        System.out.println(Thread.currentThread().getContextClassLoader());
        System.out.println(Thread.currentThread());

        CompletableFuture.runAsync(() -> {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            int roll = this.rollService.roll();

            Platform.runLater(() -> {
                Thread.currentThread().setContextClassLoader(cl);
                this.rollResultLabel.setText(String.valueOf(roll));
            });
        });

    }
}
