package io.quarkiverse.fx.views;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HotService {

    void saySomething() {
        System.out.println("Coucou");
    }
}
