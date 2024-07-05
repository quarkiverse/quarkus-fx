package io.quarkiverse.fx;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Stage;

/**
 * A non-CDI bean that is instantiated by the {@linkplain Application} layer. This
 * calls into the quarkus-fx application bean using a CDI event with {@linkplain FxStartupEvent}
 * that holds a {@linkplain Stage} instance.
 *
 * @see Stage
 */
public class FxApplication extends Application {

    private static HostServices hostServices;

    @Override
    public void start(final Stage primaryStage) {

        hostServices = this.getHostServices();

        BeanManager beanManager = CDI.current().getBeanManager();

        // Fire event that marks that Fx context is ready
        // initializations can be performed (like FXML conventional view loading)
        beanManager.getEvent().fire(new FxPreStartupEvent());

        // Fire event that marks that the application has finished starting
        // and that Stage instance is available for use
        // Views (if any) are available
        beanManager.getEvent().fire(new FxStartupEvent(primaryStage));
    }

    public static HostServices getHostServicesInstance() {
        return hostServices;
    }
}
