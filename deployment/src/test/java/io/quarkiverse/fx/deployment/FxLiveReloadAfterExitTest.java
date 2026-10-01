package io.quarkiverse.fx.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.deployment.reload.ExitOnStartup;
import io.quarkiverse.fx.deployment.reload.ReloadController;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * An application that calls Platform.exit() in dev mode : JavaFX cannot be started again in the dev JVM, and the next
 * restart fails at once, with the reason, instead of waiting for JavaFX.
 */
class FxLiveReloadAfterExitTest {

    @RegisterExtension
    static final QuarkusDevModeTest devMode = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClasses(ReloadController.class, ExitOnStartup.class)
                    .addAsResource(new StringAsset("quarkus.fx.views-root=views\n"), "application.properties")
                    .addAsResource(new StringAsset("""
                            <?xml version="1.0" encoding="UTF-8"?>
                            <?import javafx.scene.layout.VBox?>
                            <?import javafx.scene.control.Label?>
                            <VBox xmlns:fx="http://javafx.com/fxml/1"
                                  fx:controller="io.quarkiverse.fx.deployment.reload.ReloadController">
                                <Label fx:id="label" text="original-view"/>
                            </VBox>
                            """), "views/Reload.fxml"))
            .setLogRecordPredicate(record -> record.getThrown() != null);

    @Test
    void restartFailsAtOnceOnceJavaFxHasExited() throws Exception {
        snapshot("version-one");
        // The thread of FxPlatform that runs Application.launch : it ends once JavaFX has exited
        Thread launcher = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> "quarkus-fx-launcher".equals(thread.getName()))
                .findFirst()
                .orElseThrow();

        // The next runtime exits JavaFX once started
        System.setProperty(ExitOnStartup.PROPERTY, "true");
        devMode.modifySourceFile(ReloadController.class, source -> source.replace("version-one", "version-two"));
        scan();
        snapshot("version-two");
        launcher.join(Duration.ofSeconds(10).toMillis());
        assertFalse(launcher.isAlive(), "JavaFX has not exited");

        devMode.modifySourceFile(ReloadController.class, source -> source.replace("version-two", "version-three"));
        long start = System.nanoTime();
        // Stops the runtime that exited JavaFX : its ShutdownEvent detaches it without waiting for JavaFX
        scan();
        Duration restart = Duration.ofNanos(System.nanoTime() - start);
        assertTrue(restart.compareTo(Duration.ofSeconds(10)) < 0, "restart took " + restart);
        // The new runtime cannot start JavaFX again : it fails at once, with the reason
        await().atMost(Duration.ofSeconds(10))
                .until(() -> devMode.getLogRecords().stream().anyMatch(record -> thrown(record, "JavaFX has exited")));
        assertFalse(devMode.getLogRecords().stream().anyMatch(record -> thrown(record, "Timed out waiting for JavaFX")),
                "waited for JavaFX");
    }

    private static boolean thrown(LogRecord record, String message) {
        for (Throwable failure = record.getThrown(); failure != null; failure = failure.getCause()) {
            if (String.valueOf(failure.getMessage()).contains(message)) {
                return true;
            }
        }
        return false;
    }

    private static void scan() throws Exception {
        DevConsoleManager.getHotReplacementContext().doScan(true);
    }

    private static void snapshot(String version) {
        await().atMost(Duration.ofSeconds(30))
                .until(() -> System.getProperty("fx.test.reload.snapshot", "").startsWith(version + "|"));
    }

    @AfterAll
    static void cleanUp() {
        System.clearProperty(ExitOnStartup.PROPERTY);
        System.clearProperty("fx.test.reload.snapshot");
        System.clearProperty("fx.test.reload.stopped");
        System.clearProperty("fx.test.reload.stop-thread");
        System.clearProperty("fx.test.reload.attempt");
    }
}
