package io.quarkiverse.fx;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A non-CDI bean that is instantiated by the {@link Application} layer.
 * This calls into the quarkus-fx application bean using CDI events :
 * {@link FxApplicationStartupEvent} : {@link Application} is created
 * {@link FxPostStartupEvent} : {@link Application} is ready and {@link Stage} available
 * that holds a {@link Stage} instance.
 *
 * @see Stage
 */
public class FxApplication extends Application {

    @Override
    public void start(final Stage primaryStage) {

        BeanManager beanManager = CDI.current().getBeanManager();

        // Fire event that marks that Fx context is ready
        // initializations can be performed, application instance is available
        beanManager.getEvent().fire(new FxApplicationStartupEvent(this));

        // FXML conventional views loading
        beanManager.getEvent().fire(new FxViewLoadEvent(primaryStage));

        // Fire event that marks that the application has finished starting
        // and that Stage instance is available for use
        // Views (if any) are available
        beanManager.getEvent().fire(new FxPostStartupEvent(primaryStage));
    }
}
