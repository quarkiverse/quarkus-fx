package io.quarkiverse.fx.views;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import io.quarkiverse.fx.context.FxScoped;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@FxView("custom-sample")
@FxScoped
public class YetAnotherSampleController {

    @Inject
    RollService rollService;

    @FXML
    Parent root;

    @FXML
    Label rollResultLabel;

    @FXML
    private void initialize() {
        Stage stage = new Stage();
        Scene scene = new Scene(this.root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleClickMeAction() {

        System.out.println(this.rollService);

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
