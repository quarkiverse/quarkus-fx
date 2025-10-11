package io.quarkiverse.fx.sample;

import io.quarkiverse.fx.FxPostStartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class QuarkusFxApp {

    @Inject
    FXMLLoader fxmlLoader;

    public void start(@Observes FxPostStartupEvent event) {

        Stage stage = event.getPrimaryStage();

        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        try {
            InputStream fxml = this.getClass().getResourceAsStream("/app.fxml");
            Parent fxmlParent = this.fxmlLoader.load(fxml);

            Scene scene = new Scene(fxmlParent, 400, 600);
            stage.setScene(scene);
            stage.setTitle("Hello World Quarkus FX & picocli !");
            stage.show();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
