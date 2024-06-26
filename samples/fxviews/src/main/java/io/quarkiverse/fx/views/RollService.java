package io.quarkiverse.fx.views;

import java.security.SecureRandom;
import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RollService {

    private static final Random RANDOM = new SecureRandom();

    public int roll() {
        System.out.println(Thread.currentThread().getContextClassLoader());
        System.out.println(Thread.currentThread());
        return RANDOM.nextInt(0, 21);
    }
}
