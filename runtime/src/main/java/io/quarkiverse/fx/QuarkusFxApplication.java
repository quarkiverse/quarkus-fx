package io.quarkiverse.fx;

import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

public class QuarkusFxApplication implements QuarkusApplication {

    @Override
    public int run(String... args) {
        CDI.current().select(FxLifecycle.class).get().start(args);
        Quarkus.waitForExit();
        return 0;
    }
}
