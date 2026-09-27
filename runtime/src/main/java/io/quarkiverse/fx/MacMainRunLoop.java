package io.quarkiverse.fx;

import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CLibrary;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;
import org.jboss.logging.Logger;

/**
 * The main run loop of a macOS native executable.
 * <p>
 * AppKit only runs its event loop on the first thread of the process. In JVM mode, the java launcher calls main() on
 * another thread and parks the first thread in the CoreFoundation main run loop, where Glass performs its AppKit event
 * loop. A native executable has no such launcher : main() runs on the first thread, which must serve the main run loop
 * itself.
 * <p>
 * Native executables only : the GraalVM SDK is not available in JVM mode.
 */
final class MacMainRunLoop {

    private MacMainRunLoop() {
    }

    /**
     * @return whether this is a macOS native executable
     */
    static boolean isSupported() {
        return ImageInfo.inImageRuntimeCode() && Platform.includedIn(Platform.DARWIN.class);
    }

    /**
     * Runs the main run loop on the current thread, the first thread of the process, until {@code done} returns true.
     */
    static void runUntil(BooleanSupplier done) {
        // Folded at image build time : CoreFoundation is not referenced on other platforms
        if (Platform.includedIn(Platform.DARWIN.class)) {
            CoreFoundation.runUntil(done);
        }
    }

    @Platforms(Platform.DARWIN.class)
    @CLibrary("-framework CoreFoundation")
    private static final class CoreFoundation {

        private static final Logger LOGGER = Logger.getLogger(MacMainRunLoop.class);

        private static final int K_CF_STRING_ENCODING_UTF8 = 0x08000100;
        private static final int K_CF_RUN_LOOP_RUN_FINISHED = 1;

        // Glass runs its AppKit event loop inside a run of this loop : the timeout only matters before the toolkit starts
        // and after it exits
        private static final double RUN_SECONDS = 0.1;
        private static final long NO_SOURCE_PARK_NANOS = 10_000_000L;

        @CFunction
        static native PointerBase CFStringCreateWithCString(PointerBase allocator, CCharPointer cString, int encoding);

        @CFunction
        static native int CFRunLoopRunInMode(PointerBase mode, double seconds, boolean returnAfterSourceHandled);

        static void runUntil(BooleanSupplier done) {
            PointerBase defaultMode;
            try (CTypeConversion.CCharPointerHolder name = CTypeConversion.toCString("kCFRunLoopDefaultMode")) {
                defaultMode = CFStringCreateWithCString(WordFactory.nullPointer(), name.get(), K_CF_STRING_ENCODING_UTF8);
            }
            boolean notMainThreadReported = false;
            while (!done.getAsBoolean()) {
                if (CFRunLoopRunInMode(defaultMode, RUN_SECONDS, false) == K_CF_RUN_LOOP_RUN_FINISHED) {
                    // Never on the first thread, whose run loop always has a source (the main dispatch queue) : this thread
                    // serves its own run loop, Glass waits for the main run loop that nobody serves
                    if (!notMainThreadReported) {
                        notMainThreadReported = true;
                        LOGGER.error("The JavaFX event loop cannot run : in a macOS native executable, the Quarkus FX "
                                + "application must be run on the first thread of the process");
                    }
                    LockSupport.parkNanos(NO_SOURCE_PARK_NANOS);
                }
            }
        }
    }
}
