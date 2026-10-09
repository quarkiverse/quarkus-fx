package io.quarkiverse.fx.showcase.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.util.Duration;

/**
 * JavaFX helpers shared by pages.
 */
public final class Fx {

    /**
     * Runs asynchronous stages on the JavaFX Application Thread.
     */
    public static final Executor FX_THREAD = Platform::runLater;

    private Fx() {
    }

    /**
     * Completes (on the Fx thread) after {@code count} pulses were rendered.
     */
    public static CompletionStage<Void> pulses(int count) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        new AnimationTimer() {
            private int remaining = count;

            @Override
            public void handle(long now) {
                if (--remaining <= 0) {
                    stop();
                    done.complete(null);
                }
            }
        }.start();
        return done;
    }

    /**
     * Completes (on the Fx thread) after {@code millis}.
     */
    public static CompletionStage<Void> delay(double millis) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        PauseTransition pause = new PauseTransition(Duration.millis(millis));
        pause.setOnFinished(e -> done.complete(null));
        pause.play();
        return done;
    }

    /**
     * Completes when {@code stage} completes, or exceptionally with a {@link TimeoutException} after {@code millis}.
     */
    public static <T> CompletionStage<T> timeout(CompletionStage<T> stage, double millis, String what) {
        CompletableFuture<T> result = new CompletableFuture<>();
        PauseTransition pause = new PauseTransition(Duration.millis(millis));
        pause.setOnFinished(e -> result.completeExceptionally(new TimeoutException("Timeout waiting for " + what)));
        pause.play();
        stage.whenComplete((value, error) -> Platform.runLater(() -> {
            pause.stop();
            if (error != null) {
                result.completeExceptionally(error);
            } else {
                result.complete(value);
            }
        }));
        return result;
    }

    /**
     * Completes when {@code value} holds a value matching {@code predicate}.
     * <p>
     * {@code value} and its listener stay strongly reachable until then ({@link #PENDING}) : a binding (e.g.
     * {@code Bindings.createBooleanBinding}) is only weakly referenced by its dependencies, and would otherwise be
     * garbage collected with the listener before it changes, whenever a garbage collection happens during the wait.
     */
    public static <T> CompletionStage<T> when(ObservableValue<T> value, java.util.function.Predicate<? super T> predicate) {
        CompletableFuture<T> done = new CompletableFuture<>();
        if (predicate.test(value.getValue())) {
            done.complete(value.getValue());
            return done;
        }
        javafx.beans.value.ChangeListener<T> listener = (observable, oldValue, newValue) -> {
            if (predicate.test(newValue)) {
                done.complete(newValue);
            }
        };
        PENDING.add(done);
        value.addListener(listener);
        done.whenComplete((result, error) -> {
            value.removeListener(listener);
            PENDING.remove(done);
        });
        return done;
    }

    /**
     * The stages of {@link #when} waiting for their value : each one references its value and listener.
     */
    private static final java.util.Set<CompletableFuture<?>> PENDING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Classpath resource, {@code path} being absolute (e.g. {@code /showcase/images/pattern.png}).
     */
    public static URL resource(String path) {
        return Objects.requireNonNull(Fx.class.getResource(path), () -> "Resource not found: " + path);
    }

    public static String resourceUrl(String path) {
        return resource(path).toExternalForm();
    }

    public static String resourceText(String path) {
        try (InputStream in = resource(path).openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Copies a classpath resource to a temporary file (for APIs that cannot read classpath URLs, like Media).
     */
    public static Path resourceToTempFile(String path) {
        try (InputStream in = resource(path).openStream()) {
            String name = path.substring(path.lastIndexOf('/') + 1);
            Path dir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "quarkus-fx-showcase"));
            Path file = dir.resolve(name);
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
