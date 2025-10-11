package io.quarkiverse.fx;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import java.util.concurrent.CountDownLatch;

/**
 * A singleton for a startup latch used by {@linkplain FxApplication} and {@linkplain RunOnFxThreadInterceptor}
 */
@Singleton
public class FxStartupLatch {

    private final CountDownLatch latch = new CountDownLatch(1);

    public void await() throws InterruptedException {
        this.latch.await();
    }

    void onFxStartup(@Observes FxApplicationStartupEvent event) {
        this.latch.countDown();
    }
}
