package io.quarkiverse.fx.showcase.pages.layout;

import java.util.List;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Draws a {@link Badge} from its styleable properties.
 */
class BadgeSkin extends SkinBase<Badge> {

    private Shape shape;
    private List<Node> decorations = List.of();
    private final Text text = new Text();

    BadgeSkin(Badge badge) {
        super(badge);
        text.getStyleClass().add("badge-text");
        for (ObservableValue<?> value : badge.styleableObservables()) {
            registerChangeListener(value, v -> rebuild());
        }
        rebuild();
    }

    private void rebuild() {
        Badge badge = getSkinnable();
        double size = badge.getSize();
        double half = size / 2;
        shape = switch (badge.getBadgeShape()) {
            case CIRCLE -> new Circle(half, half, half);
            case SQUARE -> {
                Rectangle r = new Rectangle(size, size);
                r.setArcWidth(size / 4);
                r.setArcHeight(size / 4);
                yield r;
            }
            case DIAMOND -> new Polygon(half, 0, size, half, half, size, 0, half);
            case STAR -> star(half, half * 0.45);
        };
        Color color = badge.getColor();
        double ring = badge.getRingWidth();
        if (badge.isOutlined()) {
            shape.setFill(Color.TRANSPARENT);
            shape.setStroke(color);
            shape.setStrokeWidth(Math.max(2, ring));
        } else {
            shape.setFill(color);
            shape.setStroke(ring > 0 ? color.deriveColor(0, 1, 0.6, 1) : null);
            shape.setStrokeWidth(ring);
        }
        text.setText(badge.getText());
        text.setFill(badge.getTextFill());
        text.setFont(Font.font("System", FontWeight.BOLD, Math.max(9, Math.round(size * 0.3))));
        decorations = decorations(badge, size);
        getChildren().setAll(decorations);
        getChildren().addAll(shape, text);
        badge.requestLayout();
    }

    /**
     * Extra nodes drawn behind the badge by sub-classes, centered on (0, 0).
     */
    protected List<Node> decorations(Badge badge, double size) {
        return List.of();
    }

    /**
     * Room taken by the decorations around the badge, on each side.
     */
    protected double decorationPadding() {
        return 0;
    }

    private static Polygon star(double outer, double inner) {
        double[] points = new double[20];
        for (int i = 0; i < 10; i++) {
            double radius = i % 2 == 0 ? outer : inner;
            double angle = Math.toRadians(-90 + i * 36);
            points[i * 2] = outer + Math.round(radius * Math.cos(angle) * 100) / 100.0;
            points[i * 2 + 1] = outer + Math.round(radius * Math.sin(angle) * 100) / 100.0;
        }
        return new Polygon(points);
    }

    private double extent() {
        Badge badge = getSkinnable();
        return badge.getSize() + badge.getRingWidth() + (badge.isOutlined() ? Math.max(2, badge.getRingWidth()) : 0)
                + 2 * decorationPadding();
    }

    @Override
    protected double computePrefWidth(double height, double top, double right, double bottom, double left) {
        return left + extent() + right;
    }

    @Override
    protected double computePrefHeight(double width, double top, double right, double bottom, double left) {
        return top + extent() + bottom;
    }

    @Override
    protected double computeMinWidth(double height, double top, double right, double bottom, double left) {
        return computePrefWidth(height, top, right, bottom, left);
    }

    @Override
    protected double computeMinHeight(double width, double top, double right, double bottom, double left) {
        return computePrefHeight(width, top, right, bottom, left);
    }

    @Override
    protected double computeMaxWidth(double height, double top, double right, double bottom, double left) {
        return computePrefWidth(height, top, right, bottom, left);
    }

    @Override
    protected double computeMaxHeight(double width, double top, double right, double bottom, double left) {
        return computePrefHeight(width, top, right, bottom, left);
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        double size = getSkinnable().getSize();
        double offsetX = x + (w - size) / 2;
        double offsetY = y + (h - size) / 2;
        shape.setLayoutX(offsetX);
        shape.setLayoutY(offsetY);
        for (Node decoration : decorations) {
            decoration.setLayoutX(x + w / 2);
            decoration.setLayoutY(y + h / 2);
        }
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();
        text.setLayoutX(x + (w - textWidth) / 2 - text.getLayoutBounds().getMinX());
        text.setLayoutY(y + (h - textHeight) / 2 - text.getLayoutBounds().getMinY());
    }
}
