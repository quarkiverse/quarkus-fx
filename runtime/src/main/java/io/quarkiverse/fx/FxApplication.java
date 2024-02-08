package io.quarkiverse.fx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
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

        // We need to obtain the StartupLatch singleton
        BeanManager bm = CDI.current().getBeanManager();
        Bean<StartupLatch> bean = (Bean<StartupLatch>) bm.getBeans(StartupLatch.class).iterator().next();
        CreationalContext<StartupLatch> ctx = bm.createCreationalContext(bean);
        StartupLatch started = (StartupLatch) bm.getReference(bean, StartupLatch.class, ctx);

        // Now broadcast the startup event
        bm
            .getEvent()
            .select(new AnnotationLiteral<PrimaryStage>() {})
            .fire(primaryStage);

        // Mark that the application has finished starting.
        started.countDown();
    }
}
