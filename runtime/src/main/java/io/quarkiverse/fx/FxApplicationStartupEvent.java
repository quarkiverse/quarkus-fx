package io.quarkiverse.fx;

import javafx.application.Application;

/**
 * Event used to indicate that the FX {@link Application} is ready.
 * Called before FxStartupEvent, and used to performs initializations before applicative code
 */
public final class FxApplicationStartupEvent {

    private final Application application;

    FxApplicationStartupEvent(final Application application) {
        this.application = application;
    }

    public Application getApplication() {
        return this.application;
    }
}
