package io.quarkiverse.fx.showcase.pages.fxml;

import java.io.IOException;
import java.io.UncheckedIOException;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Custom component built with {@code <fx:root>} : it is its own root and controller. Instantiated reflectively by
 * FXMLLoader (no-arg constructor, {@code swatchColor="#1e88e5"} coerced through {@code Color.valueOf}) and copied by
 * {@code <fx:copy>} (copy constructor).
 */
@RegisterForReflection
public class ColorSwatch extends HBox {

    private final StringProperty swatchName = new SimpleStringProperty(this, "swatchName", "");
    private final ObjectProperty<Color> swatchColor = new SimpleObjectProperty<>(this, "swatchColor", Color.GRAY);

    @FXML
    private Rectangle chip;

    @FXML
    private Label nameLabel;

    public ColorSwatch() {
        FXMLLoader loader = new FXMLLoader(ColorSwatch.class.getResource("/showcase/fxml/color-swatch.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Copy constructor, used by {@code <fx:copy source="..."/>}.
     */
    public ColorSwatch(ColorSwatch other) {
        this();
        setSwatchName(other.getSwatchName() + " (copy)");
        setSwatchColor(other.getSwatchColor().deriveColor(0, 1, 0.8, 1));
    }

    public StringProperty swatchNameProperty() {
        return swatchName;
    }

    public String getSwatchName() {
        return swatchName.get();
    }

    public void setSwatchName(String value) {
        swatchName.set(value);
    }

    public ObjectProperty<Color> swatchColorProperty() {
        return swatchColor;
    }

    public Color getSwatchColor() {
        return swatchColor.get();
    }

    public void setSwatchColor(Color value) {
        swatchColor.set(value);
    }

    Rectangle chip() {
        return chip;
    }

    Label nameLabel() {
        return nameLabel;
    }
}
