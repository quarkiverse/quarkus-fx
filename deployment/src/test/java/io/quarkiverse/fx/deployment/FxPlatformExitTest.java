package io.quarkiverse.fx.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxPlatform;
import io.quarkiverse.fx.FxShutdownEvent;
import io.quarkiverse.fx.deployment.base.FxTestBase;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.Platform;

/**
 * An application that exits JavaFX before Quarkus, as the usual "Quit" of a JavaFX application does
 * ({@code Platform.exit(); Quarkus.asyncExit();}) : Quarkus FX does not wait for JavaFX, which no longer runs anything.
 * Its own class : JavaFX can only be launched once per JVM.
 */
class FxPlatformExitTest extends FxTestBase {

    private static final String SHUTDOWN_STARTED = "fx.test.exit.shutdown-started";

    private static final String FX_SHUTDOWN = "fx.test.exit.fx-shutdown";

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            // Once Quarkus has stopped : its ShutdownEvent detached Quarkus FX from JavaFX
            .setAfterUndeployListener(FxPlatformExitTest::assertPromptShutdown);

    void onFxShutdown(@Observes FxShutdownEvent event) {
        System.setProperty(FX_SHUTDOWN, Boolean.toString(Platform.isFxApplicationThread()));
    }

    @Test
    @Timeout(20)
    void doesNotWaitForJavaFxOnceItHasExited() throws InterruptedException {
        this.startAndWait();
        FxPlatform platform = FxPlatform.launch();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // The thread of FxPlatform that runs Application.launch : it ends once JavaFX has exited (looked up before)
        Thread launcher = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> "quarkus-fx-launcher".equals(thread.getName()))
                .findFirst()
                .orElseThrow();

        // Platform.exit() in an FX event handler, then JavaFX exits before Quarkus shuts down
        assertTrue(platform.invoke(loader, application -> Platform.exit()));
        launcher.join(TimeUnit.SECONDS.toMillis(10));
        assertFalse(launcher.isAlive(), "JavaFX has not exited");

        // JavaFX silently drops the actions posted once it has exited : invoke tells so at once, and JavaFX cannot be
        // launched again
        long start = System.nanoTime();
        assertFalse(platform.invoke(loader, application -> fail("run once JavaFX has exited")));
        IllegalStateException relaunch = assertThrows(IllegalStateException.class, FxPlatform::launch);
        assertTrue(relaunch.getMessage().startsWith("JavaFX has exited"), relaunch.getMessage());
        assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(5), "waited for JavaFX");

        System.setProperty(SHUTDOWN_STARTED, Long.toString(System.nanoTime()));
    }

    static void assertPromptShutdown() {
        try {
            String started = System.getProperty(SHUTDOWN_STARTED);
            assertNotNull(started, "the test did not complete");
            long elapsed = System.nanoTime() - Long.parseLong(started);
            assertTrue(elapsed < TimeUnit.SECONDS.toNanos(10),
                    "Quarkus stopped in " + TimeUnit.NANOSECONDS.toMillis(elapsed) + " ms");
            // FxShutdownEvent needs the FX thread : not fired once JavaFX has exited, and never on another thread
            assertNull(System.getProperty(FX_SHUTDOWN), "FxShutdownEvent fired");
        } finally {
            System.clearProperty(SHUTDOWN_STARTED);
            System.clearProperty(FX_SHUTDOWN);
        }
    }
}
