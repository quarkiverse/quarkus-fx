package io.quarkiverse.fx.views;

import io.quarkiverse.fx.context.FxScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.concurrent.CompletableFuture;

@FxView("custom-sample")
@FxScoped
public class YetAnotherSampleController {

    @Inject
    Instance<RollService> rollService;

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

            RollService s = this.rollService.get();
            System.out.println(s);
            int roll = s.roll();

            Platform.runLater(() -> {
                Thread.currentThread().setContextClassLoader(cl);
                this.rollResultLabel.setText(String.valueOf(roll));
            });
        });

    }
}
