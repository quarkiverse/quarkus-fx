package io.quarkiverse.fx.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.deployment.reload.ReloadController;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.test.QuarkusDevModeTest;

class FxLiveReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest devMode = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClass(ReloadController.class)
                    .addAsResource(new StringAsset("quarkus.fx.views-root=views\n"), "application.properties")
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
    void rebuildsJavaAndFxmlWithoutRelaunchingJavaFx() throws Exception {
        String[] initial = snapshot("version-one", "original-view");

        devMode.modifySourceFile(ReloadController.class, source -> source.replace("version-one", "version-two"));
        scan();
        String[] javaReload = snapshot("version-two", "original-view");
        assertReload(initial, javaReload);

        devMode.modifyResourceFile("views/Reload.fxml", source -> source.replace("original-view", "updated-view"));
        scan();
        String[] fxmlReload = snapshot("version-two", "updated-view");
        assertReload(javaReload, fxmlReload);

        // A compile failure must leave the dev session able to recover using the same toolkit.
        devMode.modifySourceFile(ReloadController.class,
                source -> source.replace("return \"version-two\";", "return doesNotCompile;"));
        scan();
        assertNotNull(DevConsoleManager.getHotReplacementContext().getDeploymentProblem());
        devMode.modifySourceFile(ReloadController.class,
                source -> source.replace("return doesNotCompile;", "return \"version-three\";"));
        scan();
        String[] recovered = snapshot("version-three", "updated-view");
        assertReload(fxmlReload, recovered);

        // A failure after CDI startup must also detach the partial UI and permit another reload.
        devMode.modifyResourceFile("views/Reload.fxml", source -> source.replace("<Label ", "<UnknownFxNode "));
        scan();
        await().atMost(Duration.ofSeconds(30)).until(() -> {
            String attempt = System.getProperty("fx.test.reload.attempt");
            return !recovered[2].equals(attempt) && attempt.equals(System.getProperty("fx.test.reload.stopped"));
        });
        String failedInstance = System.getProperty("fx.test.reload.attempt");
        devMode.modifyResourceFile("views/Reload.fxml", source -> source.replace("<UnknownFxNode ", "<Label ")
                .replace("updated-view", "recovered-view"));
        scan();
        String[] recoveredView = snapshot("version-three", "recovered-view");
        assertEquals(recovered[3], recoveredView[3]);
        assertEquals(recovered[4], recoveredView[4]);
        assertNotEquals(recovered[5], recoveredView[5]);
        assertEquals(failedInstance, System.getProperty("fx.test.reload.stopped"));
    }

    private static void scan() throws Exception {
        DevConsoleManager.getHotReplacementContext().doScan(true);
    }

    private static String[] snapshot(String version, String view) {
        await().atMost(Duration.ofSeconds(30)).until(() -> System.getProperty("fx.test.reload.snapshot", "")
                .startsWith(version + "|" + view + "|"));
        return System.getProperty("fx.test.reload.snapshot").split("\\|");
    }

    private static void assertReload(String[] previous, String[] current) {
        assertNotEquals(previous[2], current[2], "CDI controller must be recreated");
        assertEquals(previous[3], current[3], "Application must survive reload");
        assertEquals(previous[4], current[4], "FX thread must survive reload");
        assertNotEquals(previous[5], current[5], "Callbacks must use the new runtime classloader");
        assertEquals("true", current[6]);
        assertEquals(previous[2], System.getProperty("fx.test.reload.stopped"));
        assertEquals("true", System.getProperty("fx.test.reload.stop-thread"));
    }

    @AfterAll
    static void cleanUp() {
        System.clearProperty("fx.test.reload.snapshot");
        System.clearProperty("fx.test.reload.stopped");
        System.clearProperty("fx.test.reload.stop-thread");
        System.clearProperty("fx.test.reload.attempt");
    }
}
