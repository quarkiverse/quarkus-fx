package io.quarkiverse.fx.sample;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;

@ApplicationScoped
public class QuarkusFxApp {

    @Inject
    FXMLLoader fxmlLoader;

    public void start(@Observes FxPostStartupEvent event) throws InterruptedException {

        // Make it slow on purpose
        Thread.sleep(3_000);

        Stage stage = event.getPrimaryStage();

        Log.info("Begin start");
        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        try {
            InputStream fxml = this.getClass().getResourceAsStream("/app.fxml");
            Parent fxmlParent = this.fxmlLoader.load(fxml);

            Scene scene = new Scene(fxmlParent, 300, 200);
            stage.setScene(scene);
            stage.setTitle("Hello Quarkus + JavaFX ! ⏰");
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Log.info("End start");
    }
}
