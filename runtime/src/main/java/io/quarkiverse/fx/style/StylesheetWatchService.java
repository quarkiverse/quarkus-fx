package io.quarkiverse.fx.style;

import io.quarkus.logging.Log;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** Watches source stylesheets. All registrations are closed when the FX runtime detaches. */
public final class StylesheetWatchService {

    private static final Set<Registration> REGISTRATIONS = ConcurrentHashMap.newKeySet();

    private StylesheetWatchService() {
    }

    public static void setStyleAndStartWatchingTask(
            Supplier<ObservableList<String>> stylesheetsSupplier, String stylesheet) throws IOException {
        watch(stylesheetsSupplier, stylesheet);
    }

    /** Creates a registration that callers can close before the application shuts down. */
    public static AutoCloseable watch(Supplier<ObservableList<String>> stylesheetsSupplier, String stylesheet)
            throws IOException {
        Path path = Path.of(stylesheet).toAbsolutePath();
        WatchService service = FileSystems.getDefault().newWatchService();
        try {
            path.getParent().register(service, StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            Registration registration = new Registration(service, path, stylesheetsSupplier.get());
            REGISTRATIONS.add(registration);
            registration.refresh();
            Thread watcher = new Thread(registration::run, "quarkus-fx-css-watch");
            watcher.setDaemon(true);
            watcher.setContextClassLoader(Platform.class.getClassLoader());
            watcher.start();
            return registration;
        } catch (IOException | RuntimeException e) {
            service.close();
            throw e;
        }
    }

    public static void stopAll() {
        for (Registration registration : Set.copyOf(REGISTRATIONS)) {
            registration.close();
        }
    }

    private static final class Registration implements AutoCloseable {
        private final WatchService service;
        private final Path path;
        private final String stylesheet;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile ObservableList<String> stylesheets;

        private Registration(WatchService service, Path path, ObservableList<String> stylesheets) {
            this.service = service;
            this.path = path;
            this.stylesheet = path.toUri().toString();
            this.stylesheets = stylesheets;
        }

        private void refresh() {
            Platform.runLater(() -> {
                ObservableList<String> target = this.stylesheets;
                if (!this.closed.get() && target != null) {
                    // Re-add this stylesheet while retaining the other stylesheets and their order.
                    int index = target.indexOf(this.stylesheet);
                    if (index >= 0) {
                        target.remove(index);
                        target.add(index, this.stylesheet);
                    } else {
                        target.add(this.stylesheet);
                    }
                }
            });
        }

        private void run() {
            try {
                while (!this.closed.get()) {
                    WatchKey key = this.service.take();
                    boolean changed = key.pollEvents().stream()
                            .anyMatch(event -> event.kind() == StandardWatchEventKinds.OVERFLOW
                                    || this.path.getFileName().equals(event.context()));
                    if (changed) {
                      this.refresh();
                    }
                    if (!key.reset()) {
                        break;
                    }
                }
            } catch (ClosedWatchServiceException expected) {
                // Closing the registration unblocks take().
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
              this.close();
            }
        }

        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true)) {
                this.stylesheets = null;
                REGISTRATIONS.remove(this);
                try {
                  this.service.close();
                } catch (IOException e) {
                    Log.warn("Could not close stylesheet watcher", e);
                }
            }
        }
    }
}
