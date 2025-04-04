package io.quarkiverse.fx.deployment;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.deployment.base.FxTestBase;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.Platform;

class RunOnFxThreadTest extends FxTestBase {

    private static final int ASYNC_RUN_TIMEOUT_MS = 1_000;

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    private static Boolean regularThread;
    private static Boolean fxThread;

    @Test
    void test() {

        this.startAndWait();

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

    void runOnRegularThread() {
        regularThread = !Platform.isFxApplicationThread();
    }

    @RunOnFxThread
    void runOnFxThread() {
        fxThread = Platform.isFxApplicationThread();
    }
}
