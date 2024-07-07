package io.quarkiverse.fx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;

import javafx.application.Application;
import javafx.application.HostServices;

@ApplicationScoped
public class HostServicesProducer {

    private Application application;

    void observeFxPreStartupEvent(@Observes final FxPreStartupEvent event) {
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
