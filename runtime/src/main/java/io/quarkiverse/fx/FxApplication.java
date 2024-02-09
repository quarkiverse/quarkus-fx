package io.quarkiverse.fx;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A non-CDI bean that is instantiated by the {@linkplain Application} layer. This
 * calls into the quarkus-fx application bean using a CDI event with {@linkplain Stage}
 * with a {@linkplain PrimaryStage} qualifier.
 *
 * @see PrimaryStage
 * @see Stage
 */
public class FxApplication extends Application {

    @Override
    public void start(final Stage primaryStage) {

        BeanManager beanManager = CDI.current().getBeanManager();

        // Broadcast the stage availability event
        beanManager
                .getEvent()
                .select(new AnnotationLiteral<PrimaryStage>() {
                })
                .fire(primaryStage);

        // Fire event that marks that the application has finished starting.
        beanManager.getEvent().fire(new FxStartupEvent());
    }
}
