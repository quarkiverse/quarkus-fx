package io.quarkiverse.fx.showcase.pages.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkiverse.fx.showcase.core.Fx;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * A custom control whose look is entirely driven by CSS : four properties created by a
 * {@link StyleablePropertyFactory} (color, number, boolean, enum) and two with hand written {@link CssMetaData}
 * (paint, size). Its defaults come from its own user agent stylesheet (badge.css).
 */
public class Badge extends Control {

    static final PseudoClass ALERT = PseudoClass.getPseudoClass("alert");

    private static final String USER_AGENT_STYLESHEET = Fx.resourceUrl("/showcase/layout/badge.css");

    private static final StyleablePropertyFactory<Badge> FACTORY = new StyleablePropertyFactory<>(
            Control.getClassCssMetaData());

    private static final CssMetaData<Badge, Paint> TEXT_FILL = new CssMetaData<>("-badge-text-fill",
            PaintConverter.getInstance(), Color.WHITE) {
        @Override
        public boolean isSettable(Badge badge) {
            return !badge.textFill.isBound();
        }

        @Override
        public StyleableProperty<Paint> getStyleableProperty(Badge badge) {
            return badge.textFill;
        }
    };

    private static final CssMetaData<Badge, Number> RING_WIDTH = new CssMetaData<>("-badge-ring-width",
            SizeConverter.getInstance(), 0.0) {
        @Override
        public boolean isSettable(Badge badge) {
            return !badge.ringWidth.isBound();
        }

        @Override
        public StyleableProperty<Number> getStyleableProperty(Badge badge) {
            return badge.ringWidth;
        }
    };

    private final StyleableProperty<Color> color = FACTORY.createStyleableColorProperty(this, "color", "-badge-color",
            badge -> badge.color, Color.GRAY);
    private final StyleableProperty<Number> size = FACTORY.createStyleableNumberProperty(this, "size", "-badge-size",
            badge -> badge.size, 32);
    private final StyleableProperty<Boolean> outlined = FACTORY.createStyleableBooleanProperty(this, "outlined",
            "-badge-outlined", badge -> badge.outlined, false);
    private final StyleableProperty<BadgeShape> shape = FACTORY.createStyleableEnumProperty(this, "shape", "-badge-shape",
            badge -> badge.shape, BadgeShape.class, BadgeShape.CIRCLE);
    private final StyleableObjectProperty<Paint> textFill = new SimpleStyleableObjectProperty<>(TEXT_FILL, this, "textFill",
            Color.WHITE);
    private final StyleableDoubleProperty ringWidth = new SimpleStyleableDoubleProperty(RING_WIDTH, this, "ringWidth", 0.0);
    private final StringProperty text = new SimpleStringProperty(this, "text", "");

    public Badge(String text) {
        this.text.set(text);
        getStyleClass().add("badge");
        setFocusTraversable(false);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        // the factory creates its CssMetaData lazily, with the first instance
        List<CssMetaData<? extends Styleable, ?>> all = new ArrayList<>(FACTORY.getCssMetaData());
        all.add(TEXT_FILL);
        all.add(RING_WIDTH);
        return Collections.unmodifiableList(all);
    }

    @Override
    protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    @Override
    public String getUserAgentStylesheet() {
        return USER_AGENT_STYLESHEET;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new BadgeSkin(this);
    }

    public void setAlert(boolean alert) {
        pseudoClassStateChanged(ALERT, alert);
    }

    public Color getColor() {
        return color.getValue();
    }

    public double getSize() {
        return size.getValue().doubleValue();
    }

    public boolean isOutlined() {
        return outlined.getValue();
    }

    public BadgeShape getBadgeShape() {
        return shape.getValue();
    }

    public Paint getTextFill() {
        return textFill.get();
    }

    public double getRingWidth() {
        return ringWidth.get();
    }

    public String getText() {
        return text.get();
    }

    StyleOrigin colorOrigin() {
        return color.getStyleOrigin();
    }

    @SuppressWarnings("unchecked")
    List<ObservableValue<?>> styleableObservables() {
        return List.of((ObservableValue<?>) color, (ObservableValue<?>) size, (ObservableValue<?>) outlined,
                (ObservableValue<?>) shape, textFill, ringWidth, text);
    }
}
