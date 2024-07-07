package io.quarkiverse.fx;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Stage;

/**
 * A non-CDI bean that is instantiated by the {@link Application} layer. This
 * calls into the quarkus-fx application bean using a CDI event with {@link FxStartupEvent}
 * that holds a {@link Stage} instance.
 *
 * @see Stage
 */
public class FxApplication extends Application {

    private static Application application;
    private static HostServices hostServices;

    @Override
    public void start(final Stage primaryStage) {

        // Keep a static reference to application instance
        application = this;

        BeanManager beanManager = CDI.current().getBeanManager();

        // Fire event that marks that Fx context is ready
        // initializations can be performed (like FXML conventional view loading)
        beanManager.getEvent().fire(new FxPreStartupEvent(this));

        // Fire event that marks that the application has finished starting
        // and that Stage instance is available for use
        // Views (if any) are available
        beanManager.getEvent().fire(new FxStartupEvent(primaryStage));
    }

    /**
     * Retrieve the {@link HostServices} instance by delegation to {@link Application}
     *
     * @return HostServices provider
     */
    public static HostServices getHostServicesInstance() {
        // Lazy initialization
        if (hostServices == null) {
            hostServices = application.getHostServices();
        }
        return hostServices;
    }
}
