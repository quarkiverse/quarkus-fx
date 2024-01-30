package io.quarkiverse.fx;

import io.quarkus.runtime.QuarkusApplication;
import jakarta.enterprise.context.Dependent;
import javafx.application.Application;

@Dependent
public class QuarkusFxApplication implements QuarkusApplication {

    @Override
    public int run(final String... args) {
        Application.launch(FxApplication.class, args);
        return 0;
    }
}
