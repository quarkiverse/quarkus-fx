package io.quarkiverse.fx;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import javafx.application.Application;
import javafx.application.Platform;

public class QuarkusFxApplication implements QuarkusApplication {

    private static final Logger LOGGER = Logger.getLogger(QuarkusFxApplication.class);

    /**
     * The system property that Quarkus Desktop sets when it keeps the first thread of a macOS native executable in the
     * main run loop itself, and runs the Quarkus application on another thread
     * ({@code quarkus.desktop.awt.macos.park-main-thread}, the default).
     */
    static final String QUARKUS_DESKTOP_MAIN_THREAD_PARKED = "io.quarkiverse.desktop.main-thread-parked";

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

        if (mustServeMainRunLoop()) {
            launchServingMainRunLoop(args);
        } else {
            // Launch in a new thread to prevent blocking
            new Thread(() -> launch(args)).start();
        }

        Quarkus.waitForExit();
        return 0;
    }

    /**
     * On macOS, AppKit only runs its event loop on the first thread of the process.
     * In JVM mode, the java launcher calls main() on another thread and keeps the first thread in the main run loop,
     * where Glass performs its event loop.
     * A native executable has no such launcher : this method is invoked on the first thread itself.
     * If this thread did not serve the main run loop, Glass would wait forever for it, and no window would ever be shown.
     * With Quarkus Desktop, which keeps the first thread in the main run loop as the java launcher does, this method is
     * invoked on another thread : JavaFX is launched as in JVM mode.
     */
    private static boolean mustServeMainRunLoop() {
        return ImageMode.current() == ImageMode.NATIVE_RUN
                && System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac")
                && MacMainRunLoop.isSupported()
                && System.getProperty(QUARKUS_DESKTOP_MAIN_THREAD_PARKED) == null;
    }

    private static void launchServingMainRunLoop(String... args) {
        // Stop the toolkit when Quarkus is asked to exit, so that this thread is released
        Thread exitWatcher = new Thread(() -> {
            Quarkus.waitForExit();
            Platform.exit();
        }, "quarkus-fx-exit-watcher");
        exitWatcher.setDaemon(true);
        exitWatcher.start();

        // Launched from another thread as in JVM mode : Glass performs its event loop on this thread, through the main run loop.
        // Starting the toolkit on this thread instead (Platform::startup) makes Glass run its event loop in place, and
        // JavaFX 24+ then frees its application delegate while that loop still uses it (NullPointerException at exit).
        AtomicBoolean toolkitExited = new AtomicBoolean();
        new Thread(() -> {
            try {
                launch(args);
            } finally {
                toolkitExited.set(true);
            }
        }, "quarkus-fx-launcher").start();

        // What the java launcher does with the first thread, until the toolkit exits
        MacMainRunLoop.runUntil(toolkitExited::get);
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
