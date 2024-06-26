package io.quarkiverse.fx;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.CDI;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A non-CDI bean that is instantiated by the {@linkplain Application} layer. This
 * calls into the quarkus-fx application bean using a CDI event with {@linkplain FxStartupEvent}
 * that holds a {@linkplain Stage} instance.
 *
 * @see Stage
 */
public class FxApplication extends Application {

    @Override
    public void start(final Stage primaryStage) {

        Event<Object> event = CDI.current().getBeanManager().getEvent();

        // Fire event that marks that Fx context is ready
        // initializations can be performed (like FXML conventional view loading)
        event.fire(new FxPreStartupEvent());

        // Fire event that marks that the application has finished starting
        // and that Stage instance is available for use
        // Views (if any) are available
        event.fire(new FxStartupEvent(primaryStage));
    }
}
