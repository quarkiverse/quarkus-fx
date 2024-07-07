package io.quarkiverse.fx;

import javafx.application.Application;

/**
 * Event used to indicate that the FX {@link Application} is ready.
 * Called before FxStartupEvent, and used to performs initializations before applicative code
 */
public class FxPreStartupEvent {

    private final Application application;

    FxPreStartupEvent(final Application application) {
        this.application = application;
    }

    public Application getApplication() {
        return this.application;
    }
}
