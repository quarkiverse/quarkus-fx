package io.quarkiverse.fx.deployment;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkus.runtime.Quarkus;

public class FxTestBase {

    @Inject
    FxStartupLatch startupLatch;

    private static final AtomicBoolean eventObserved = new AtomicBoolean(false);

    protected void startAndWait() {
        // Non-blocking launch
        CompletableFuture.runAsync(() -> Quarkus.run(QuarkusFxApplication.class));

        // Wait for readiness
        try {
            this.startupLatch.await();
        } catch (InterruptedException e) {
            // Should not be interrupted
            Assertions.fail(e);
        }

        await()
                .atMost(FxTestConstants.LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .until(eventObserved::get);

        Assertions.assertTrue(eventObserved.get());
    }

    void observeEvent(@Observes final FxPostStartupEvent event) {
        eventObserved.set(true);
    }
}
