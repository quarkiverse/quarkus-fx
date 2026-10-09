package io.quarkiverse.fx.showcase.pages.text;

import java.util.Locale;

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.VLineTo;

/**
 * Small layout helpers shared by the text and images pages : captioned demo tiles and deterministic number formatting.
 */
public final class Ui {

    public static final String TILE_STYLE = "-fx-background-color: white; -fx-border-color: #cfd4da; "
            + "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5 6 6 6;";

    private Ui() {
    }

    /**
     * A small grey caption.
     */
    public static Label caption(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #5f6b7a;");
        label.setMinHeight(Label.USE_PREF_SIZE);
        return label;
    }

    /**
     * A white bordered box : caption on top, then the content nodes.
     */
    public static VBox tile(String caption, Node... content) {
        VBox box = new VBox(4);
        box.getChildren().add(caption(caption));
        box.getChildren().addAll(content);
        box.setStyle(TILE_STYLE);
        return box;
    }

    public static <T extends Node> T grow(T node) {
        javafx.scene.layout.HBox.setHgrow(node, Priority.ALWAYS);
        javafx.scene.layout.VBox.setVgrow(node, Priority.ALWAYS);
        return node;
    }

    /**
     * Rounds to one decimal, formatted with a dot whatever the default locale.
     */
    public static String num(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        if (rounded == 0) {
            rounded = 0; // no negative zero
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    public static String rect(double x, double y, double w, double h) {
        return "[" + num(x) + ", " + num(y) + ", " + num(w) + " x " + num(h) + "]";
    }

    public static String bounds(Bounds b) {
        return rect(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
    }

    public static String rect(Rectangle2D r) {
        return rect(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
    }

    public static String size(Bounds b) {
        return num(b.getWidth()) + " x " + num(b.getHeight());
    }

    /**
     * Compact, deterministic description of path elements (as returned by caretShape / rangeShape).
     */
    public static String path(PathElement[] elements) {
        if (elements == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (PathElement e : elements) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            switch (e) {
                case MoveTo m -> sb.append("M").append(num(m.getX())).append(',').append(num(m.getY()));
                case LineTo l -> sb.append("L").append(num(l.getX())).append(',').append(num(l.getY()));
                case HLineTo h -> sb.append("H").append(num(h.getX()));
                case VLineTo v -> sb.append("V").append(num(v.getY()));
                case QuadCurveTo q -> sb.append("Q").append(num(q.getX())).append(',').append(num(q.getY()));
                case CubicCurveTo c -> sb.append("C").append(num(c.getX())).append(',').append(num(c.getY()));
                case ClosePath c -> sb.append("Z");
                default -> sb.append(e.getClass().getSimpleName());
            }
        }
        return sb.toString();
    }

    /**
     * ARGB as #AARRGGBB.
     */
    public static String argb(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }
}
