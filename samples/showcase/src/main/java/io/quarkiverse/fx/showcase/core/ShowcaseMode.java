package io.quarkiverse.fx.showcase.core;

import java.util.Locale;

import io.quarkus.runtime.ImageMode;

/**
 * Global state of the showcase run.
 */
public final class ShowcaseMode {

    private static volatile boolean snapshot;

    private ShowcaseMode() {
    }

    /**
     * {@code true} when pages are rendered to be compared : pages must then be deterministic
     * (animations paused at a fixed time, no caret, no hover, no clock...).
     */
    public static boolean snapshot() {
        return snapshot;
    }

    static void enableSnapshot() {
        snapshot = true;
    }

    /**
     * {@code JVM} or {@code NATIVE}.
     */
    public static String runtime() {
        return ImageMode.current().isNativeImage() ? "NATIVE" : "JVM";
    }

    /**
     * The Prism pipeline rendering the scenes ({@code d3d}, {@code es2}, {@code mtl} or {@code sw}) : the runs of a
     * comparison must use the same one, which prism.order or a missing native library could change.
     */
    public static String graphicsPipeline() {
        com.sun.prism.GraphicsPipeline pipeline = com.sun.prism.GraphicsPipeline.getPipeline();
        if (pipeline == null) {
            return "none";
        }
        String name = pipeline.getClass().getSimpleName();
        return (name.endsWith("Pipeline") ? name.substring(0, name.length() - "Pipeline".length()) : name)
                .toLowerCase(Locale.ROOT);
    }
}
