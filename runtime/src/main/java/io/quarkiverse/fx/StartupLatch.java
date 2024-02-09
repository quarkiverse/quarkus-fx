package io.quarkiverse.fx;

import java.util.concurrent.CountDownLatch;

import jakarta.inject.Singleton;

/**
 * A singleton for a startup latch used by {@linkplain FxApplication} and {@linkplain RunOnFxThreadInterceptor}
 */
@Singleton
public class StartupLatch {
    private final CountDownLatch started = new CountDownLatch(1);

    public void await() throws InterruptedException {
        started.await();
    }

    public void countDown() {
        started.countDown();
    }
}
