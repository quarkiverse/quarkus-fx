package io.quarkiverse.fx.views;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;
import java.util.Random;

@ApplicationScoped
public class RollService {

    private static final Random RANDOM = new SecureRandom();

    public int roll() {
        System.out.println(Thread.currentThread().getContextClassLoader());
        System.out.println(Thread.currentThread());
        System.out.println("RollService instance : " + this);
        int value = RANDOM.nextInt(0, 21);
        System.out.println("Roll : " + value);
        return value;
    }
}
