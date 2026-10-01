package io.quarkiverse.fx.showcase.pages.layout;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * An alternative {@link Badge} skin, only selected from CSS ({@code -fx-skin: "...BadgeRingSkin"}) : Control loads
 * the class by name and invokes its public {@code (Badge)} constructor through reflection.
 */
@RegisterForReflection
public class BadgeRingSkin extends BadgeSkin {

    private static final double GAP = 7;

    public BadgeRingSkin(Badge badge) {
        super(badge);
    }

    @Override
    protected List<Node> decorations(Badge badge, double size) {
        Color color = badge.getColor();
        Circle halo = new Circle(size / 2 + GAP, color.deriveColor(0, 1, 1, 0.25));
        Circle ring = new Circle(size / 2 + GAP - 2.5, null);
        ring.setStroke(color);
        ring.setStrokeWidth(1.5);
        return List.of(halo, ring);
    }

    @Override
    protected double decorationPadding() {
        return GAP;
    }
}
