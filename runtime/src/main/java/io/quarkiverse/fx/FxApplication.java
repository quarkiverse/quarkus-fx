package io.quarkiverse.fx;

import java.util.concurrent.CountDownLatch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;

import javafx.application.Application;
import javafx.stage.Stage;

@ApplicationScoped
public class FxApplication extends Application {
    private static CountDownLatch started = new CountDownLatch(1);

    /**
     * A producer for the FxApplication.started CountDownLatch
     *
     * @return FxApplication.started CountDownLatch
     */
    @Produces
    @PrimaryStage
    CountDownLatch produceStartedFlag() {
        return started;
    }

    @Override
    public void start(final Stage primaryStage) {

        CDI.current()
                .getBeanManager()
                .getEvent()
                .select(new AnnotationLiteral<PrimaryStage>() {
                })
                .fire(primaryStage);
        // Mark that the application has finished starting.
        started.countDown();
    }
}
