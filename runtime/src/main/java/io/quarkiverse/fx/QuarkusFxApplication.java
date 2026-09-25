package io.quarkiverse.fx;

import java.util.Locale;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import javafx.application.Application;
import javafx.application.Platform;

public class QuarkusFxApplication implements QuarkusApplication {

    private static final Logger LOGGER = Logger.getLogger(QuarkusFxApplication.class);

    private static boolean launched = false;

    @Override
    public int run(String... args) {

        // Prevent launching more than once
        if (launched) {
            LOGGER.warn("Fx application already launched : skipping call to Application::launch");
            Quarkus.waitForExit();
            return 0;
        }

        launched = true;

        if (mustStartToolkitOnCurrentThread()) {
            startToolkitOnCurrentThread(args);
        } else {
            // Launch in a new thread to prevent blocking
            new Thread(() -> launch(args)).start();
        }

        Quarkus.waitForExit();
        return 0;
    }

    /**
     * On macOS, AppKit only runs its event loop on the first thread of the process.
     * In JVM mode, the java launcher keeps that thread free for Cocoa and calls main() on another thread.
     * A native executable has no such launcher : this method is invoked on the first thread itself.
     * If the toolkit was started from another thread, Glass would wait forever for this (blocked) thread
     * to run its event loop, and no window would ever be shown.
     */
    private static boolean mustStartToolkitOnCurrentThread() {
        return ImageMode.current() == ImageMode.NATIVE_RUN
                && System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");
    }

    private static void startToolkitOnCurrentThread(String... args) {
        // Stop the toolkit when Quarkus is asked to exit, so that this thread is released
        Thread exitWatcher = new Thread(() -> {
            Quarkus.waitForExit();
            Platform.exit();
        }, "quarkus-fx-exit-watcher");
        exitWatcher.setDaemon(true);
        exitWatcher.start();

        // Glass runs the AppKit event loop in place when started on the first thread : this call returns once the toolkit exits.
        // The application is then launched as usual, Application::launch reusing the running toolkit.
        Platform.startup(() -> new Thread(() -> launch(args), "quarkus-fx-launcher").start());
    }

    private static void launch(String... args) {
        try {
            Application.launch(FxApplication.class, args);
        } catch (Exception e) {
            LOGGER.error("An exception occurred in Fx application launch", e);
            // The application cannot run : do not keep the process waiting for an exit that will never be requested
            Quarkus.asyncExit(1);
        }
    }
}
