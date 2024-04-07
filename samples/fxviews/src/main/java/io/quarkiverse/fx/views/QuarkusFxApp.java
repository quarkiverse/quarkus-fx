package io.quarkiverse.fx.views;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkiverse.fx.FxStartupEvent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

@ApplicationScoped
public class QuarkusFxApp {

    public void start(@Observes final FxStartupEvent event) {
        Stage stage = event.getPrimaryStage();
        Scene scene = new Scene(new Pane());
        stage.setScene(scene);
        stage.setTitle("Hello World");
        stage.show();
    }
}
