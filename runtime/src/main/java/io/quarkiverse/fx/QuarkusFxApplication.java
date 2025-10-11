package io.quarkiverse.fx;

import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import javafx.application.Application;

public class QuarkusFxApplication implements QuarkusApplication {

    private static final Logger LOGGER = Logger.getLogger(QuarkusFxApplication.class);

    private static boolean launched = false;

    @Override
    public int run(String... args) {

        // Prevent launching more than once
        if (launched) {
            LOGGER.warn("Fx application already launched : skipping call to Application::launch");
            Quarkus.waitForExit();
            return 0;
        }

        launched = true;

        // Launch in a new thread to prevent blocking
        new Thread(() -> {
            try {
                Application.launch(FxApplication.class, args);
            } catch (Exception e) {
                LOGGER.error("An exception occurred in Fx application launch", e);
            }
        }).start();

        Quarkus.waitForExit();
        return 0;
    }
}
