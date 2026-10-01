package io.quarkiverse.fx.showcase.core;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javafx.scene.Node;
import javafx.scene.image.Image;

/**
 * A page of the showcase, exercising one JavaFX feature area.
 * <p>
 * Implementations are CDI beans ({@code @Singleton}) discovered through {@code Instance<FeaturePage>}.
 * Every method is invoked on the JavaFX Application Thread.
 * <p>
 * In snapshot mode ({@link ShowcaseMode#snapshot()}), the rendered page is saved as an image and compared between
 * JVM and native mode : a page must render the same pixels on every run (no running animation captured at a random
 * frame, no clock, no random values, no caret, no hover).
 */
public interface FeaturePage {

    /**
     * Unique, stable and file-name safe identifier ({@code group-name}, lower case, dashes).
     */
    String id();

    String title();

    /**
     * One of {@link Categories#ORDER}.
     */
    String category();

    /**
     * Order within the category.
     */
    default int order() {
        return 100;
    }

    /**
     * Builds a fresh content node for this page.
     * Checks can be attached to the returned node with {@link Checks#attach(Node, java.util.List)}.
     */
    Node build() throws Exception;

    /**
     * Completes once the content is fully rendered (e.g. web page loaded, media ready, background image loaded).
     */
    default CompletionStage<?> ready(Node content) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Additional images to compare, for content outside the page node (popup windows, dialogs, other stages, ...).
     * Invoked after {@link #ready(Node)} completed. Keys must be file-name safe.
     */
    default CompletionStage<Map<String, Image>> extraSnapshots(Node content) {
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * {@code true} for a page that shows where the runtime legitimately makes a difference (JVM or native image) :
     * its images and check values are expected to differ between the runs, and are reported as {@code EXPECTED} by
     * tools/Compare.java. Failed checks and errors are still reported.
     */
    default boolean runtimeDependent() {
        return false;
    }

    /**
     * Releases resources (stops media, closes windows, ...) when the page is left.
     */
    default void dispose(Node content) {
    }
}
