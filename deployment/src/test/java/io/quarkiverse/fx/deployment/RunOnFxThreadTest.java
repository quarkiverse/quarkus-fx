package io.quarkiverse.fx.deployment;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxStartupEvent;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkiverse.fx.RunOnFxThread;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.Platform;

public class RunOnFxThreadTest {

    private static final int ASYNC_RUN_TIMEOUT_MS = 500;

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    private static boolean primaryStageObserved = false;
    private static Boolean regularThread;
    private static Boolean fxThread;

    @Test
    void test() {
        // Non-blocking JavaFX launch
        CompletableFuture.runAsync(() -> Quarkus.run(QuarkusFxApplication.class));

        await()
                .atMost(FxTestConstants.LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .until(() -> primaryStageObserved);

        // Synchronous regular thread run
        this.runOnRegularThread();
        Assertions.assertTrue(regularThread);

        // Asynchronous FX thread run
        this.runOnFxThread();
        Assertions.assertNull(fxThread);

        await().atMost(ASYNC_RUN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .until(() -> fxThread != null);

        Assertions.assertTrue(fxThread);
    }

    void observePrimaryStage(@Observes final FxStartupEvent event) {
        Assertions.assertNotNull(event.getPrimaryStage());
        primaryStageObserved = true;
    }

    void runOnRegularThread() {
        regularThread = !Platform.isFxApplicationThread();
    }

    @RunOnFxThread
    void runOnFxThread() {
        fxThread = Platform.isFxApplicationThread();
    }
}
