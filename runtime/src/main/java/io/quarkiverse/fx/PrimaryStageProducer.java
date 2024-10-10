package io.quarkiverse.fx;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import javafx.stage.Stage;

@ApplicationScoped
public class PrimaryStageProducer {

    private Stage primaryStage;

    void observeFxPostStartupEvent(@Observes @Priority(1) final FxPostStartupEvent event) {
        this.primaryStage = event.getPrimaryStage();
    }

    @Produces
    @ApplicationScoped
    Stage producePrimaryStage() {
        if (this.primaryStage == null) {
            throw new IllegalStateException("primaryStage is null");
        }
        return this.primaryStage;
    }
}
