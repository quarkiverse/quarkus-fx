package io.quarkiverse.fx.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.deployment.reload.PreserveTestMain;
import io.quarkiverse.fx.deployment.reload.ReloadController;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.test.QuarkusDevModeTest;

class FxLiveReloadPreserveTest {

    @RegisterExtension
    static final QuarkusDevModeTest devMode = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClasses(ReloadController.class, PreserveTestMain.class)
                    .addAsResource(new StringAsset("quarkus.fx.views-root=views\nquarkus.fx.hot-reload-strategy=preserve\n"),
                            "application.properties")
                    .addAsResource(new StringAsset("""
                            <?xml version="1.0" encoding="UTF-8"?>
                            <?import javafx.scene.layout.VBox?>
                            <?import javafx.scene.control.Label?>
                            <VBox xmlns:fx="http://javafx.com/fxml/1"
                                  fx:controller="io.quarkiverse.fx.deployment.reload.ReloadController">
                                <Label fx:id="label" text="original-view"/>
                            </VBox>
                            """), "views/Reload.fxml"));

    @Test
    void retainsOriginalUiAcrossQuarkusRestarts() throws Exception {
        String initialWindow = checkpoint().split("\\|")[1];
        String initial = System.getProperty("fx.test.reload.snapshot");

        devMode.modifySourceFile(ReloadController.class, source -> source.replace("version-one", "version-two"));
        System.clearProperty("fx.test.reload.checkpoint");
        DevConsoleManager.getHotReplacementContext().doScan(true);
        assertEquals("version-two|" + initialWindow + "|original-view", checkpoint());
        assertEquals(initial, System.getProperty("fx.test.reload.snapshot"));
        assertNull(System.getProperty("fx.test.reload.stopped"));

        devMode.modifyResourceFile("views/Reload.fxml", source -> source.replace("original-view", "updated-view"));
        System.clearProperty("fx.test.reload.checkpoint");
        DevConsoleManager.getHotReplacementContext().doScan(true);
        assertEquals("version-two|" + initialWindow + "|original-view", checkpoint());
        assertEquals(initial, System.getProperty("fx.test.reload.snapshot"));
        assertNull(System.getProperty("fx.test.reload.stopped"));
    }

    @AfterAll
    static void cleanUp() {
        System.clearProperty("fx.test.reload.snapshot");
        System.clearProperty("fx.test.reload.attempt");
        System.clearProperty("fx.test.reload.checkpoint");
    }

    private static String checkpoint() {
        await().atMost(Duration.ofSeconds(30)).until(() -> System.getProperty("fx.test.reload.checkpoint") != null);
        return System.getProperty("fx.test.reload.checkpoint");
    }
}
