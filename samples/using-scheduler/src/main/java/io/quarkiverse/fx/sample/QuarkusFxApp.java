package io.quarkiverse.fx.sample;

import java.io.InputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import io.quarkiverse.fx.PrimaryStage;
import io.quarkus.logging.Log;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

@ApplicationScoped
public class QuarkusFxApp {

    @Inject
    FXMLLoader fxmlLoader;
    private AppController appController;

    public void start(@Observes @PrimaryStage final Stage stage) {
        Log.info("Begin start");
        stage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        try {
            InputStream fxml = this.getClass().getResourceAsStream("/app.fxml");
            Parent fxmlParent = this.fxmlLoader.load(fxml);
            appController = this.fxmlLoader.getController();

            Scene scene = new Scene(fxmlParent, 300, 200);
            stage.setScene(scene);
            stage.setTitle("Hello Quarkus + JavaFX ! ⏰");
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Log.info("End start");
    }

    void onMessage(@ObservesAsync TimeEvent timeEvent) {
        Log.infof("Time: %s;%s", timeEvent.unixTime(), timeEvent.timeString());
        appController.onMessage(timeEvent.timeString());
    }
}
