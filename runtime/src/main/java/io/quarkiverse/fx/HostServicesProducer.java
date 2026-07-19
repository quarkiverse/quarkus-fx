package io.quarkiverse.fx;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.ObserverMethod;

import javafx.application.Application;
import javafx.application.HostServices;

@ApplicationScoped
public class HostServicesProducer {

    private Application application;

    void observeFxPreStartupEvent(
            @Observes @Priority(ObserverMethod.DEFAULT_PRIORITY - 1) FxApplicationStartupEvent event) {
        this.application = event.getApplication();
    }

    @Produces
    @ApplicationScoped
    HostServices produceHostServices() {
        if (this.application == null) {
            throw new IllegalStateException("Application is null");
        }
        return this.application.getHostServices();
    }
}
