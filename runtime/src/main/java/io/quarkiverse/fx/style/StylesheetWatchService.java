package io.quarkiverse.fx.style;

import java.io.IOException;
import java.net.URL;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.quarkus.logging.Log;
import javafx.application.Platform;
import javafx.collections.ObservableList;

/**
 * This utility class allows live CSS reload by watching filesystem changes
 * and re-setting the stylesheet upon change.
 * It is automatically used in dev mode for all {@link io.quarkiverse.fx.views.FxView}
 */
public final class StylesheetWatchService {

    private static final Map<ObservableList<String>, List<WatchService>> WATCH_SERVICES = new IdentityHashMap<>();

    private StylesheetWatchService() {
        // Utility class
    }

    public static void setStyleAndStartWatchingTask(
            Supplier<ObservableList<String>> stylesheetsSupplier,
            String stylesheet) throws IOException {

        // CSS live change monitoring
        // Get stylesheet URL from disk (project root)
        Path path = Path.of(stylesheet);
        URL url = path.toUri().toURL();
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        ObservableList<String> stylesheets = stylesheetsSupplier.get();
        synchronized (WATCH_SERVICES) {
            WATCH_SERVICES.computeIfAbsent(stylesheets, ignored -> new ArrayList<>()).add(watchService);
        }
        String stylesheetExternalForm = url.toExternalForm();
        updateWithStylesheet(stylesheetExternalForm, stylesheets);

        CompletableFuture.runAsync(() -> {
            try {
                performBlockingWatch(watchService, stylesheets, stylesheetExternalForm);
            } catch (ClosedWatchServiceException ignored) {
                // The associated view was replaced during live reload.
            } catch (InterruptedException e) {
                Log.error("Stylesheet file watch got interrupted", e);
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Stop all watchers associated with a stylesheet list that is no longer displayed.
     */
    public static void stopWatching(ObservableList<String> stylesheets) {
        List<WatchService> watchServices;
        synchronized (WATCH_SERVICES) {
            watchServices = WATCH_SERVICES.remove(stylesheets);
        }
        if (watchServices != null) {
            watchServices.forEach(StylesheetWatchService::close);
        }
    }

    private static void close(WatchService watchService) {
        try {
            watchService.close();
        } catch (IOException e) {
            Log.warn("Failed to close stylesheet watch service", e);
        }
    }

    private static void performBlockingWatch(
            WatchService watchService,
            ObservableList<String> stylesheets,
            String stylesheet) throws InterruptedException {

        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                // Reload CSS in FX thread
                updateWithStylesheet(stylesheet, stylesheets);
            }
            key.reset();
        }
    }

    private static void updateWithStylesheet(String stylesheet, ObservableList<String> stylesheets) {
        Platform.runLater(() -> stylesheets.setAll(stylesheet));
    }
}
