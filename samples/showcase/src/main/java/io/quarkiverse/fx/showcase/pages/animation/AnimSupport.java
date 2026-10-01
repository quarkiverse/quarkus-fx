package io.quarkiverse.fx.showcase.pages.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.animation.Animation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Helpers shared by the animation pages.
 * <p>
 * Every animation is first played, jumped to a fixed time and paused, so that the same values are rendered and
 * checked on every run. Outside snapshot mode, the animations are then resumed.
 */
final class AnimSupport {

    private static final String ANIMATIONS_KEY = "anim.animations";

    private AnimSupport() {
    }

    /**
     * Plays {@code animation}, jumps to {@code at} and pauses it.
     */
    static void freeze(Animation animation, Duration at) {
        animation.play();
        animation.jumpTo(at);
        animation.pause();
    }

    /**
     * Remembers the animations of a page : resumed outside snapshot mode, stopped by {@link #stopAll(Node)}.
     */
    static void register(Node root, List<? extends Animation> animations) {
        root.getProperties().put(ANIMATIONS_KEY, new ArrayList<Animation>(animations));
        if (!ShowcaseMode.snapshot()) {
            animations.forEach(Animation::play);
        }
    }

    @SuppressWarnings("unchecked")
    static void stopAll(Node root) {
        Object animations = root.getProperties().remove(ANIMATIONS_KEY);
        if (animations instanceof List<?> list) {
            ((List<Animation>) list).forEach(Animation::stop);
        }
    }

    /**
     * A demo tile : a clipped stage with a light frame and a caption below it.
     */
    static VBox tile(String caption, Pane stage, double width, double height) {
        stage.setMinSize(width, height);
        stage.setPrefSize(width, height);
        stage.setMaxSize(width, height);
        stage.setClip(new Rectangle(width, height));
        stage.setStyle("-fx-background-color: white; -fx-border-color: #d9dde3;");
        Label label = new Label(caption);
        label.setWrapText(true);
        label.setMaxWidth(width);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555;");
        return new VBox(3, stage, label);
    }

    static Rectangle ghost(double x, double y, double w, double h) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setFill(null);
        r.setStroke(Color.web("#b8bec8"));
        r.getStrokeDashArray().setAll(4.0, 3.0);
        return r;
    }

    static String f3(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    static String f1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    static String ms(Duration duration) {
        return f1(duration.toMillis()) + " ms";
    }
}
