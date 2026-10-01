package io.quarkiverse.fx.deployment.reload;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkiverse.fx.FxPostStartupEvent;
import javafx.application.Platform;

/** Exits JavaFX once started, when the test asks for it : the misuse of Platform.exit() in dev mode. */
@Singleton
public class ExitOnStartup {

    public static final String PROPERTY = "fx.test.reload.platform-exit";

    void started(@Observes FxPostStartupEvent event) {
        if (Boolean.getBoolean(PROPERTY)) {
            Platform.exit();
        }
    }
}
