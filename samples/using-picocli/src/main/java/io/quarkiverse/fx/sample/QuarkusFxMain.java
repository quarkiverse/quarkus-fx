package io.quarkiverse.fx.sample;

import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class QuarkusFxMain implements QuarkusApplication {

    @Override
    public int run(String... args) {
        return new QuarkusFxApplication().run(args);
    }
}
