package io.quarkiverse.fx.views;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkiverse.fx.FxPostStartupEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

@ApplicationScoped
public class SampleFxApp {

    @Inject
    FxViewRepository viewRepository;

    void start(@Observes FxPostStartupEvent event) {
        Parent root = this.viewRepository.getViewData("custom-sample").getRootNode();
        Stage stage = event.getPrimaryStage();
        stage.setScene(new Scene(root));
        stage.show();
    }
}
