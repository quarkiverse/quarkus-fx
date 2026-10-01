package io.quarkiverse.fx.deployment.fxviews;

import static org.awaitility.Awaitility.await;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import io.quarkiverse.fx.FxPlatform;
import io.quarkiverse.fx.deployment.base.FxTestBase;
import io.quarkiverse.fx.deployment.fxviews.controllers.ComponentWithStyleController;
import io.quarkiverse.fx.style.StylesheetWatchService;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkiverse.fx.views.StylesheetReloadStrategy;
import io.quarkus.test.QuarkusUnitTest;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Parent;

class FxStyleReloadTest extends FxTestBase {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> {
                jar.addClass(ComponentWithStyleController.class);
                jar.addAsResource("views");
            })
            .overrideConfigKey("quarkus.fx.views-root", "views")
            .overrideConfigKey("quarkus.fx.stylesheet-reload-strategy", StylesheetReloadStrategy.ALWAYS.name())
            .overrideConfigKey("quarkus.fx.target-resources", "app-root/")
            .overrideConfigKey("quarkus.fx.source-resources", "src/test/resources/");

    @Inject
    FxViewRepository viewRepository;

    // Can't really test that modifications are effective,
    // but we can test that stylesheet has been replaced by the one from sources directory
    @Test
    void testLiveReload(@TempDir Path directory) throws Exception {

        this.startAndWait();

        // Check that substitution has been done
        Parent component = this.viewRepository.getViewData("ComponentWithStyle").getRootNode();
        ObservableList<String> stylesheets = component.getStylesheets();
        Assertions.assertEquals(1, stylesheets.size());
        Assertions.assertTrue(stylesheets.get(0).endsWith("src/test/resources/views/ComponentWithStyle.css"));

        Path first = Files.writeString(directory.resolve("first.css"), ".root { -fx-opacity: 1; }");
        Path second = Files.writeString(directory.resolve("second.css"), ".root { -fx-opacity: 1; }");
        ObservableList<String> watched = FXCollections.observableArrayList();
        AtomicInteger changes = new AtomicInteger();
        watched.addListener((ListChangeListener<String>) change -> changes.incrementAndGet());
        try (AutoCloseable firstWatch = StylesheetWatchService.watch(() -> watched, first.toString());
                AutoCloseable secondWatch = StylesheetWatchService.watch(() -> watched, second.toString())) {
            FxPlatform platform = FxPlatform.launch();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // invoke also drains the initial refreshes queued by watch().
            platform.invoke(loader, application -> Assertions.assertEquals(2, watched.size()));
            int initialChanges = changes.get();
            Files.writeString(first, ".root { -fx-opacity: 0.5; }");
            await().atMost(Duration.ofSeconds(5)).until(() -> changes.get() > initialChanges);
            platform.invoke(loader, application -> {
                Assertions.assertEquals(2, watched.size());
                Assertions.assertEquals(first.toUri().toString(), watched.get(0));
                Assertions.assertEquals(second.toUri().toString(), watched.get(1));
            });
        }
        StylesheetWatchService.stopAll();
        await().atMost(Duration.ofSeconds(5)).until(() -> Thread.getAllStackTraces().keySet().stream()
                .noneMatch(thread -> thread.isAlive() && thread.getName().equals("quarkus-fx-css-watch")));
    }
}
