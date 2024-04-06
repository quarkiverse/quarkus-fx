package io.quarkiverse.fx;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.List;

/**
 * A non-CDI bean that is instantiated by the {@linkplain Application} layer. This
 * calls into the quarkus-fx application bean using a CDI event with {@linkplain FxStartupEvent}
 * that holds a {@linkplain Stage} instance.
 *
 * @see Stage
 */
public class FxApplication extends Application {

    private static List<String> views;

    @Override
    public void start(final Stage primaryStage) {

        BeanManager beanManager = CDI.current().getBeanManager();

        System.out.println("Fx Views : ");
        System.out.println(views);
        beanManager.getEvent().fire(new FxViewEvent());

        // Fire event that marks that the application has finished starting
        // and that Stage instance is available for use
        beanManager.getEvent().fire(new FxStartupEvent(primaryStage));
    }

    public static void setViews(final List<String> views) {
        FxApplication.views = views;
    }
}
