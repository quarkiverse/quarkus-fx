package io.quarkiverse.fx;

import org.jboss.logging.Logger;

import io.quarkus.runtime.QuarkusApplication;
import javafx.application.Application;

public class QuarkusFxApplication implements QuarkusApplication {

    private static final Logger LOGGER = Logger.getLogger(QuarkusFxApplication.class);

    private static boolean launched = false;

    @Override
    public int run(final String... args) {

        System.out.println(this);

        if (launched) {
            LOGGER.warn("Fx application already launched : skipping call to Application.launch() method");
            return 0;
        }
        launched = true;
        Application.launch(FxApplication.class, args);
        return 0;
    }
}
