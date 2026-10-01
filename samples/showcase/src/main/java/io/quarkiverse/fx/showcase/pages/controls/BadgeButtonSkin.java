package io.quarkiverse.fx.showcase.pages.controls;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.scene.control.Button;
import javafx.scene.control.SkinBase;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

/**
 * A custom Button skin, never instantiated by the application : controls.css selects it with
 * {@code -fx-skin: "io.quarkiverse.fx.showcase.pages.controls.BadgeButtonSkin"}, and JavaFX loads the class by name
 * and invokes its {@code (Button)} constructor through reflection (Control.loadSkinClass), hence the registration.
 */
@RegisterForReflection
public class BadgeButtonSkin extends SkinBase<Button> {

    private static final double SIZE = 38;

    private final Circle circle = new Circle();
    private final Text text = new Text();
    private final Circle dot = new Circle(5);

    public BadgeButtonSkin(Button button) {
        super(button);
        circle.getStyleClass().add("badge-skin-circle");
        text.getStyleClass().add("badge-skin-text");
        text.textProperty().bind(button.textProperty());
        dot.getStyleClass().add("badge-skin-dot");
        getChildren().addAll(circle, text, dot);
    }

    // Fixed min, pref and max sizes : the SkinBase defaults derive the min size from the current position of the
    // children, which this skin computes from its own size (the size would grow on every layout pass)

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return SIZE + leftInset + rightInset;
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return SIZE + topInset + bottomInset;
    }

    @Override
    protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        // the dot stays inside the bounds of the control
        double radius = Math.min(w, h) / 2;
        circle.setRadius(radius);
        circle.setCenterX(x + w / 2);
        circle.setCenterY(y + h / 2);
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();
        text.relocate(snapPositionX(x + (w - textWidth) / 2), snapPositionY(y + (h - textHeight) / 2));
        dot.setCenterX(x + w / 2 + radius * 0.6);
        dot.setCenterY(y + h / 2 - radius * 0.6);
    }
}
