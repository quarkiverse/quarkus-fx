package io.quarkiverse.fx.showcase.pages.graphics;

import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.W;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.row;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.section;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Shear;

/**
 * Every paint type on every hardware-accelerated primitive.
 * <p>
 * With the ES2 pipeline, Prism draws each (primitive, paint) pair with its own stock shader : a GLSL resource
 * ({@code com/sun/prism/es2/glsl/<Primitive>_<Paint>.frag}) compiled through a {@code <Primitive>_<Paint>_Loader}
 * class invoked by reflection. Each cell of the matrix loads one of them, the checks render every row again through
 * {@code Node.snapshot} and verify that each cell was painted.
 */
@Singleton
public class PaintMatrixPage implements FeaturePage {

    private static final double CELL_W = 96;
    private static final double CELL_H = 28;
    private static final double LABEL_W = 170;
    private static final double GAP = 8;

    private static final String[] PAINTS = { "Color", "Linear PAD", "Linear REFLECT", "Linear REPEAT", "Radial PAD",
            "Radial REFLECT", "Radial REPEAT", "ImagePattern" };

    /** A primitive : the Prism mask type (shader prefix), how it is drawn, and a node factory for a paint. */
    private record Primitive(String shader, String caption, Function<Paint, Node> factory) {
    }

    private static final List<Primitive> PRIMITIVES = List.of(
            new Primitive("Solid", "Rectangle, smooth = false", PaintMatrixPage::solid),
            new Primitive("FillPgram", "sheared Rectangle, fill", PaintMatrixPage::fillPgram),
            new Primitive("DrawPgram", "sheared Rectangle, stroke", PaintMatrixPage::drawPgram),
            new Primitive("FillRoundRect", "Rectangle arcs 14, fill", PaintMatrixPage::fillRoundRect),
            new Primitive("DrawRoundRect", "Rectangle arcs 16, stroke 4", PaintMatrixPage::drawRoundRect),
            new Primitive("DrawSemiRoundRect", "Rectangle arcs 6, stroke 8", PaintMatrixPage::drawSemiRoundRect),
            new Primitive("FillCircle", "Circle, fill", PaintMatrixPage::fillCircle),
            new Primitive("DrawCircle", "Circle, stroke", PaintMatrixPage::drawCircle),
            new Primitive("FillEllipse", "Ellipse, fill", PaintMatrixPage::fillEllipse),
            new Primitive("DrawEllipse", "Ellipse, stroke", PaintMatrixPage::drawEllipse),
            new Primitive("Texture", "Polygon (rasterized mask)", PaintMatrixPage::texture));

    @Override
    public String id() {
        return "graphics-paint-matrix";
    }

    @Override
    public String title() {
        return "Paint × Primitive Matrix";
    }

    @Override
    public String category() {
        return Categories.GRAPHICS;
    }

    @Override
    public int order() {
        return 25;
    }

    @Override
    public Node build() {
        Image tile = Tiles.image("tile.png");
        return Tiles.page(
                section("Every paint on every primitive (each cell uses its own Prism stock shader <Primitive>_<Paint>)"),
                matrix(tile),
                section("Gradients with more than 12 stops (complex paints, rendered through a gradient texture)"),
                row(tile("13 stops, Rectangle", W, 44, complex(new Rectangle(100, 30), false)),
                        tile("13 stops, rounded stroke", W, 44, complexStroke()),
                        tile("13 stops radial, Circle", W, 44, complex(new Circle(18), true)),
                        tile("13 stops, Polygon", W, 44, complex(star(20, 8), false)),
                        tile("13 stops, Text", W * 2 + 8, 44, complex(text(), false))),
                Tiles.checks(Checks.view("Painted cells per primitive (Node.snapshot)", checks(tile, 0, 6)),
                        Checks.view("Painted cells per primitive, continued", checks(tile, 6, PRIMITIVES.size()))));
    }

    // ---- Matrix ----

    private static Paint[] paints(Image tile) {
        Stop[] warm = { new Stop(0, Color.web("#ffca28")), new Stop(1, Color.web("#d81b60")) };
        Stop[] cold = { new Stop(0, Color.web("#e3f2fd")), new Stop(1, Color.web("#283593")) };
        return new Paint[] {
                Color.web("#00897b"),
                new LinearGradient(0.3, 0, 0.7, 0, true, CycleMethod.NO_CYCLE, warm),
                new LinearGradient(0, 0, 0.2, 0, true, CycleMethod.REFLECT, warm),
                new LinearGradient(0, 0, 0.2, 0, true, CycleMethod.REPEAT, warm),
                new RadialGradient(0, 0, 0.5, 0.5, 0.4, true, CycleMethod.NO_CYCLE, cold),
                new RadialGradient(0, 0, 0.5, 0.5, 0.2, true, CycleMethod.REFLECT, cold),
                new RadialGradient(0, 0, 0.5, 0.5, 0.2, true, CycleMethod.REPEAT, cold),
                new ImagePattern(tile, 0, 0, 12, 12, false) };
    }

    private static GridPane matrix(Image tile) {
        GridPane grid = new GridPane();
        grid.setHgap(GAP);
        grid.setVgap(4);
        for (int col = 0; col < PAINTS.length; col++) {
            Label header = new Label(PAINTS[col]);
            header.getStyleClass().add("gfx-matrix-header");
            header.setMinWidth(CELL_W);
            header.setPrefWidth(CELL_W);
            header.setAlignment(Pos.CENTER);
            grid.add(header, col + 1, 0);
        }
        for (int row = 0; row < PRIMITIVES.size(); row++) {
            Primitive primitive = PRIMITIVES.get(row);
            Label name = new Label(primitive.shader());
            name.getStyleClass().add("gfx-matrix-row");
            Label caption = new Label(primitive.caption());
            caption.getStyleClass().add("gfx-caption");
            VBox label = new VBox(0, name, caption);
            label.setMinWidth(LABEL_W);
            label.setPrefWidth(LABEL_W);
            label.setAlignment(Pos.CENTER_LEFT);
            grid.add(label, 0, row + 1);
            Paint[] paints = paints(tile);
            for (int col = 0; col < paints.length; col++) {
                StackPane cell = new StackPane(new Group(primitive.factory().apply(paints[col])));
                cell.getStyleClass().add("gfx-stage");
                cell.setMinSize(CELL_W, CELL_H);
                cell.setPrefSize(CELL_W, CELL_H);
                cell.setMaxSize(CELL_W, CELL_H);
                grid.add(cell, col + 1, row + 1);
            }
        }
        return grid;
    }

    private static Node solid(Paint paint) {
        Rectangle rect = new Rectangle(64, 18, paint);
        rect.setSmooth(false);
        return rect;
    }

    private static Node fillPgram(Paint paint) {
        Rectangle rect = new Rectangle(56, 18, paint);
        rect.getTransforms().add(new Shear(0.5, 0));
        return rect;
    }

    private static Node drawPgram(Paint paint) {
        Rectangle rect = stroked(new Rectangle(52, 14), paint, 5);
        rect.getTransforms().add(new Shear(0.5, 0));
        return rect;
    }

    private static Node fillRoundRect(Paint paint) {
        Rectangle rect = new Rectangle(64, 20, paint);
        rect.setArcWidth(14);
        rect.setArcHeight(14);
        return rect;
    }

    private static Node drawRoundRect(Paint paint) {
        Rectangle rect = stroked(new Rectangle(60, 16), paint, 4);
        rect.setArcWidth(16);
        rect.setArcHeight(16);
        return rect;
    }

    private static Node drawSemiRoundRect(Paint paint) {
        // the stroke is wider than the arcs : the inner corners are square
        Rectangle rect = stroked(new Rectangle(60, 14), paint, 8);
        rect.setArcWidth(6);
        rect.setArcHeight(6);
        return rect;
    }

    private static Node fillCircle(Paint paint) {
        return new Circle(11, paint);
    }

    private static Node drawCircle(Paint paint) {
        return stroked(new Circle(9), paint, 4);
    }

    private static Node fillEllipse(Paint paint) {
        Ellipse ellipse = new Ellipse(26, 10);
        ellipse.setFill(paint);
        return ellipse;
    }

    private static Node drawEllipse(Paint paint) {
        // not too flat : the stroke shader handles ellipses whose inner curve stays close to an ellipse
        return stroked(new Ellipse(15, 9), paint, 4);
    }

    private static Node texture(Paint paint) {
        Polygon star = star(13, 6);
        star.setFill(paint);
        return star;
    }

    private static <T extends Shape> T stroked(T shape, Paint paint, double width) {
        shape.setFill(null);
        shape.setStroke(paint);
        shape.setStrokeWidth(width);
        return shape;
    }

    private static Polygon star(double outer, double inner) {
        double[] points = new double[20];
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double angle = StrictMath.toRadians(-90 + i * 36);
            points[i * 2] = r * StrictMath.cos(angle);
            points[i * 2 + 1] = r * StrictMath.sin(angle);
        }
        return new Polygon(points);
    }

    // ---- Complex paints ----

    private static Stop[] rainbow() {
        String[] colors = { "#b71c1c", "#e53935", "#fb8c00", "#fdd835", "#c0ca33", "#43a047", "#00897b", "#00acc1",
                "#1e88e5", "#3949ab", "#5e35b1", "#8e24aa", "#d81b60" };
        Stop[] stops = new Stop[colors.length];
        for (int i = 0; i < colors.length; i++) {
            stops[i] = new Stop(i / (colors.length - 1.0), Color.web(colors[i]));
        }
        return stops;
    }

    private static Paint complexPaint(boolean radial) {
        return radial
                ? new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE, rainbow())
                : new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, rainbow());
    }

    private static Node complex(Shape shape, boolean radial) {
        shape.setFill(complexPaint(radial));
        return shape;
    }

    private static Node complexStroke() {
        Rectangle rect = stroked(new Rectangle(90, 26), complexPaint(false), 6);
        rect.setArcWidth(16);
        rect.setArcHeight(16);
        return rect;
    }

    private static Text text() {
        Text text = new Text("Thirteen stops");
        text.setFont(Font.font("System", FontWeight.BOLD, 28));
        return text;
    }

    // ---- Checks ----

    private static List<Check> checks(Image tile, int from, int to) {
        List<Check> checks = new ArrayList<>();
        for (Primitive primitive : PRIMITIVES.subList(from, to)) {
            checks.add(Checks.expect(primitive.shader() + "_*", PAINTS.length + "/" + PAINTS.length,
                    () -> painted(primitive, tile)));
        }
        if (to == PRIMITIVES.size()) {
            checks.add(Checks.expect("13-stop gradients", "5/5", () -> {
                List<Node> nodes = List.of(complex(new Rectangle(100, 30), false), complexStroke(),
                        complex(new Circle(18), true), complex(star(20, 8), false), complex(text(), false));
                return count(nodes, null);
            }));
        }
        return checks;
    }

    private static String painted(Primitive primitive, Image tile) {
        List<Node> nodes = new ArrayList<>();
        for (Paint paint : paints(tile)) {
            nodes.add(primitive.factory().apply(paint));
        }
        return count(nodes, PAINTS);
    }

    /**
     * Renders {@code nodes} side by side in one snapshot and counts those that painted at least 15 non-white pixels.
     */
    private static String count(List<Node> nodes, String[] names) {
        Group row = new Group();
        List<Group> cells = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Group cell = new Group(nodes.get(i));
            cell.setLayoutX(i * 240 - cell.getLayoutBounds().getMinX());
            cell.setLayoutY(-cell.getLayoutBounds().getMinY());
            cells.add(cell);
            row.getChildren().add(cell);
        }
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.WHITE);
        WritableImage image = row.snapshot(parameters, null);
        PixelReader reader = image.getPixelReader();
        Bounds all = row.getBoundsInParent();
        int painted = 0;
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            Bounds b = cells.get(i).getBoundsInParent();
            int x0 = (int) Math.max(0, Math.floor(b.getMinX() - Math.floor(all.getMinX())));
            int y0 = (int) Math.max(0, Math.floor(b.getMinY() - Math.floor(all.getMinY())));
            int x1 = (int) Math.min(image.getWidth(), Math.ceil(b.getMaxX() - Math.floor(all.getMinX())));
            int y1 = (int) Math.min(image.getHeight(), Math.ceil(b.getMaxY() - Math.floor(all.getMinY())));
            int ink = 0;
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    int argb = reader.getArgb(x, y);
                    int r = (argb >> 16) & 0xff;
                    int g = (argb >> 8) & 0xff;
                    int bl = argb & 0xff;
                    if (r < 220 || g < 220 || bl < 220) {
                        ink++;
                    }
                }
            }
            if (ink >= 15) {
                painted++;
            } else {
                missing.add(names == null ? String.valueOf(i + 1) : names[i]);
            }
        }
        return painted + "/" + cells.size() + (missing.isEmpty() ? "" : " missing " + missing);
    }
}
