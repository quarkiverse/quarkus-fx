package io.quarkiverse.fx.showcase.pages.graphics;

import java.util.Locale;

import io.quarkiverse.fx.showcase.core.Fx;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.TextAlignment;

/**
 * Layout helpers shared by the graphics pages : captioned demo tiles, rows, sections, check columns.
 */
final class Tiles {

    static final String STYLESHEET = "/showcase/graphics/graphics.css";

    /** Page frame minus its padding. */
    static final double CONTENT_WIDTH = 1028;

    static final double CONTENT_HEIGHT = 728;

    /** Width of a tile when 8 tiles share a row. */
    static final double W = 118;

    static final Color BLUE = Color.web("#1565c0");
    static final Color BLUE_L = Color.web("#90caf9");
    static final Color RED = Color.web("#c62828");
    static final Color RED_L = Color.web("#ef9a9a");
    static final Color GREEN = Color.web("#2e7d32");
    static final Color GREEN_L = Color.web("#a5d6a7");
    static final Color PURPLE = Color.web("#6a1b9a");
    static final Color PURPLE_L = Color.web("#ce93d8");
    static final Color ORANGE = Color.web("#e65100");
    static final Color ORANGE_L = Color.web("#ffcc80");
    static final Color TEAL = Color.web("#00695c");
    static final Color TEAL_L = Color.web("#80cbc4");
    static final Color GUIDE = Color.web("#90a4ae");

    private Tiles() {
    }

    /**
     * Page root : a vertical box using the graphics stylesheet, starting with a {@link #maskPrimer()}.
     */
    static VBox page(Node... children) {
        VBox root = new VBox(5, children);
        root.getChildren().add(0, maskPrimer());
        root.getStyleClass().add("gfx-page");
        root.getStylesheets().add(Fx.resourceUrl(STYLESHEET));
        return root;
    }

    /**
     * An invisible shape covering the page, rendered before anything else, that makes the rendering of the page
     * independent of what the window rendered before.
     * <p>
     * Prism rasterizes complex shapes (paths, polygons, arcs, stroked text, ...) into coverage masks packed in a shared
     * mask texture, and the right-most column of a mask can pick up (one color level) from the texel next to it, which
     * holds whatever an earlier rendering left there : an extra on-screen repaint (focus change, window expose) was
     * enough to change a few edge pixels of the next snapshot. The mask of this primer (more than 512 px wide, so never
     * cached, and fully transparent) overwrites the shared texture with the same content at the start of every page
     * rendering. Measured : without it, snapshots of stroked text taken after unrelated renderings differ in about 150
     * pixels (max delta 1); with it, they are identical.
     */
    static Node maskPrimer() {
        Polygon primer = new Polygon(0, 0, CONTENT_WIDTH, 0, CONTENT_WIDTH, CONTENT_HEIGHT, 0, CONTENT_HEIGHT);
        primer.setFill(Color.TRANSPARENT);
        primer.setManaged(false);
        primer.setMouseTransparent(true);
        return primer;
    }

    static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("gfx-section");
        return label;
    }

    /**
     * A demo : {@code content} centered on a white stage of {@code w} x {@code h}, with a caption below.
     */
    static VBox tile(String caption, double w, double h, Node... content) {
        return tile(caption, w, h, "gfx-stage", content);
    }

    static VBox tile(String caption, double w, double h, String stageClass, Node... content) {
        StackPane stage = new StackPane(new Group(content));
        stage.getStyleClass().add(stageClass);
        stage.setMinSize(w, h);
        stage.setPrefSize(w, h);
        stage.setMaxSize(w, h);
        stage.setClip(new Rectangle(w, h));
        Label label = new Label(caption);
        label.getStyleClass().add("gfx-caption");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.TOP_CENTER);
        label.setMinWidth(w);
        label.setPrefWidth(w);
        label.setMaxWidth(w);
        VBox box = new VBox(2, stage, label);
        box.setMinWidth(w);
        box.setMaxWidth(w);
        return box;
    }

    /**
     * Removes the clip of a tile's stage (an ancestor clip cuts parts of 3D-transformed image views).
     */
    static VBox unclipped(VBox tile) {
        tile.getChildren().get(0).setClip(null);
        return tile;
    }

    static HBox row(Node... tiles) {
        return new HBox(8, tiles);
    }

    /**
     * Check tables side by side, each taking an equal share of the width.
     */
    static HBox checks(VBox... views) {
        double gap = 10;
        double width = Math.floor((CONTENT_WIDTH - gap * (views.length - 1)) / views.length);
        HBox box = new HBox(gap);
        for (VBox view : views) {
            view.setMinWidth(width);
            view.setPrefWidth(width);
            view.setMaxWidth(width);
            box.getChildren().add(view);
        }
        return box;
    }

    static Rectangle rect(double x, double y, double w, double h, Paint fill) {
        Rectangle rect = new Rectangle(x, y, w, h);
        rect.setFill(fill);
        return rect;
    }

    static <T extends Shape> T paint(T shape, Paint fill, Paint stroke, double strokeWidth) {
        shape.setFill(fill);
        shape.setStroke(stroke);
        shape.setStrokeWidth(strokeWidth);
        return shape;
    }

    /**
     * Applies the graphics stylesheet (plus an inline style and style classes) to {@code node} in a detached scene,
     * so CSS values can be verified at build time.
     */
    static <T extends Node> T css(T node, String style, String... styleClasses) {
        if (style != null) {
            node.setStyle(style);
        }
        node.getStyleClass().addAll(styleClasses);
        Scene scene = new Scene(new Group(node));
        scene.getStylesheets().add(Fx.resourceUrl(STYLESHEET));
        node.applyCss();
        return node;
    }

    /**
     * Light checkerboard, to make translucency visible.
     */
    static ImagePattern checker(int cell) {
        WritableImage image = new WritableImage(cell * 2, cell * 2);
        PixelWriter writer = image.getPixelWriter();
        Color light = Color.WHITE;
        Color dark = Color.web("#cfd8dc");
        for (int y = 0; y < cell * 2; y++) {
            for (int x = 0; x < cell * 2; x++) {
                writer.setColor(x, y, ((x / cell) + (y / cell)) % 2 == 0 ? light : dark);
            }
        }
        return new ImagePattern(image, 0, 0, cell * 2, cell * 2, false);
    }

    static Image image(String name) {
        return new Image(Fx.resourceUrl("/showcase/images/" + name));
    }

    static Image image(String name, double width, double height) {
        return new Image(Fx.resourceUrl("/showcase/images/" + name), width, height, false, true);
    }

    /**
     * Size of a loaded image, failing if it could not be loaded.
     */
    static String size(Image image) {
        if (image.isError()) {
            throw new IllegalStateException("image error: " + image.getException());
        }
        return (int) image.getWidth() + "x" + (int) image.getHeight();
    }

    static String num(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    static String bounds(Bounds b) {
        return String.format(Locale.ROOT, "[%.1f, %.1f  %.1f x %.1f]", b.getMinX(), b.getMinY(), b.getWidth(),
                b.getHeight());
    }

    static String hex(Color c) {
        return String.format(Locale.ROOT, "#%02x%02x%02x%02x", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255), Math.round(c.getOpacity() * 255));
    }

    /**
     * Renders {@code node} (in a detached scene) and returns the color of the pixel at {@code (x, y)} of the snapshot.
     */
    static String pixel(Node node, int x, int y) {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.WHITE);
        WritableImage image = node.snapshot(parameters, null);
        return hex(image.getPixelReader().getColor(x, y)) + " of " + (int) image.getWidth() + "x"
                + (int) image.getHeight();
    }
}
