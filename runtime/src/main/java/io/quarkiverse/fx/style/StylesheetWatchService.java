package io.quarkiverse.fx.style;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.collections.ObservableList;

/**
 * This utility class allows live CSS reload by watching filesystem changes
 * and re-setting the stylesheet upon change.
 * It is automatically used in dev mode for all {@link io.quarkiverse.fx.views.FxView}
 */
public final class StylesheetWatchService {

    private StylesheetWatchService() {
        // Utility class
    }

    public static void setStyleAndStartWatchingTask(
            final Supplier<ObservableList<String>> stylesheetsSupplier,
            final String stylesheet) throws IOException {

        // CSS live change monitoring
        // Get stylesheet URL from disk (project root)
        Path path = Path.of(stylesheet);
        URL url = path.toUri().toURL();
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        ObservableList<String> stylesheets = stylesheetsSupplier.get();
        String stylesheetExternalForm = url.toExternalForm();
        updateWithStylesheet(stylesheetExternalForm, stylesheets);

        CompletableFuture.runAsync(() -> {
            try {
                performBlockingWatch(watchService, stylesheets, stylesheetExternalForm);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        });
    }

    private static void performBlockingWatch(
            final WatchService watchService,
            final ObservableList<String> stylesheets,
            final String stylesheet) throws InterruptedException {

        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                // Reload CSS in FX thread
                updateWithStylesheet(stylesheet, stylesheets);
            }
            key.reset();
        }
    }

    private static void updateWithStylesheet(final String stylesheet, final ObservableList<String> stylesheets) {
        Platform.runLater(() -> stylesheets.setAll(stylesheet));
    }
}
