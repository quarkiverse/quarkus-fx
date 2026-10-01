package io.quarkiverse.fx.showcase.pages.fxml;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A plain CDI bean injected into the FXML controllers, proving they are created by the CDI controller factory.
 */
@ApplicationScoped
public class GreetingService {

    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
