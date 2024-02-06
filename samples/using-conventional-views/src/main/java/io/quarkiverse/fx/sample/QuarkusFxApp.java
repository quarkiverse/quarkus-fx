package io.quarkiverse.fx.sample;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkiverse.fx.PrimaryStage;
import io.quarkiverse.fx.sample.date.DateView;
import io.quarkiverse.fx.sample.time.TimeView;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@ApplicationScoped
public class QuarkusFxApp {

    @Inject
    DateView dateView;

    @Inject
    TimeView timeView;

    public void start(@Observes @PrimaryStage final Stage stage) {
        VBox dateViewNode = dateView.getNode();
        VBox timeViewNode = timeView.getNode();
        HBox hBox = new HBox(dateViewNode, timeViewNode);
        Scene scene = new Scene(hBox);
        stage.setScene(scene);
        stage.show();
    }
}
