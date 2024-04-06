package io.quarkiverse.fx.views;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkiverse.fx.FxStartupEvent;

@ApplicationScoped
public class QuarkusFxApp {

    public void start(@Observes final FxStartupEvent event) {
        System.out.println("START !!");
    }
}
