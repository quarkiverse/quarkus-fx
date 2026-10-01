package io.quarkiverse.fx.showcase.pages.graphics;

import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.BLUE;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.BLUE_L;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.GREEN;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.GREEN_L;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.GUIDE;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.ORANGE;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.ORANGE_L;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.PURPLE;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.PURPLE_L;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.RED;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.RED_L;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.TEAL;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.TEAL_L;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.W;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.paint;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.row;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.section;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.tile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.shape.VLineTo;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

@Singleton
public class ShapesPage implements FeaturePage {

    private static final double H = 72;

    @Override
    public String id() {
        return "graphics-shapes";
    }

    @Override
    public String title() {
        return "Shapes & Strokes";
    }

    @Override
    public String category() {
        return Categories.GRAPHICS;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        Properties paths = paths();
        return Tiles.page(
                section("Shapes and paths"),
                row(roundedRectangles(), circle(), ellipse(), lines(), polyline(), polygon(), arc(ArcType.OPEN),
                        arc(ArcType.CHORD)),
                row(arc(ArcType.ROUND), quadCurve(), cubicCurve(), path(), badge(paths), union(), intersect(), subtract()),
                section("Strokes"),
                row(strokeWidths(), dashArrays(), dashOffsets(), lineCaps(), lineJoins(), strokeTypes(), miterLimits(),
                        cssStroke()),
                section("Fill rules, boolean operations, CSS shapes"),
                row(pathFillRules(), svgFillRules(paths), textSubtract(), xor(), unionOfThree(), smooth(), cssShape(),
                        heartStripes(paths)),
                Tiles.checks(Checks.view("Geometry", geometryChecks(paths)),
                        Checks.view("CSS and resources", cssChecks())));
    }

    // ---- Shapes and paths ----

    private static Node roundedRectangles() {
        Rectangle a = paint(new Rectangle(0, 0, 46, 56), BLUE_L, BLUE, 2);
        a.setArcWidth(20);
        a.setArcHeight(20);
        Rectangle b = paint(new Rectangle(54, 0, 46, 56), BLUE_L, BLUE, 2);
        b.setArcWidth(40);
        b.setArcHeight(14);
        return tile("Rectangle arcs 20/20 | 40/14", W, H, a, b);
    }

    private static Node circle() {
        return tile("Circle r=28", W, H, paint(new Circle(28), RED_L, RED, 2));
    }

    private static Node ellipse() {
        return tile("Ellipse 46 x 24", W, H, paint(new Ellipse(46, 24), GREEN_L, GREEN, 2));
    }

    private static Node lines() {
        Line a = new Line(0, 0, 96, 54);
        a.setStroke(BLUE);
        a.setStrokeWidth(3);
        Line b = new Line(0, 54, 96, 0);
        b.setStroke(RED);
        b.setStrokeWidth(2);
        Line c = new Line(0, 27, 96, 27);
        c.setStroke(GREEN);
        Line d = new Line(48, 0, 48, 54);
        d.setStroke(ORANGE);
        d.setStrokeWidth(5);
        return tile("Line (widths 1-5)", W, H, a, b, c, d);
    }

    private static Node polyline() {
        Polyline zigzag = new Polyline(0, 50, 14, 8, 28, 50, 42, 8, 56, 50, 70, 8, 84, 50, 98, 8);
        zigzag.setStroke(PURPLE);
        zigzag.setStrokeWidth(3);
        zigzag.setFill(null);
        return tile("Polyline", W, H, zigzag);
    }

    private static Node polygon() {
        Polygon star = new Polygon(starPoints(5, 30, 13));
        paint(star, Color.GOLD, ORANGE, 2);
        star.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return tile("Polygon (star)", W, H, star);
    }

    private static Node arc(ArcType type) {
        Arc arc = new Arc(0, 0, 30, 30, 30, 270);
        arc.setType(type);
        paint(arc, GREEN_L, GREEN, 3);
        return tile("Arc " + type + " 30° + 270°", W, H, arc);
    }

    private static Node quadCurve() {
        QuadCurve curve = paint(new QuadCurve(0, 54, 45, -10, 90, 54), ORANGE_L, ORANGE, 2);
        return tile("QuadCurve + control", W, H, controls(0, 54, 45, -10), controls(45, -10, 90, 54), handle(45, -10),
                curve);
    }

    private static Node cubicCurve() {
        CubicCurve curve = paint(new CubicCurve(0, 32, 28, -4, 62, 68, 90, 32), null, PURPLE, 3);
        return tile("CubicCurve + controls", W, H, controls(0, 32, 28, -4), controls(62, 68, 90, 32), handle(28, -4),
                handle(62, 68), curve);
    }

    private static Node path() {
        LineTo relative = new LineTo(12, 0);
        relative.setAbsolute(false);
        Path path = new Path(
                new MoveTo(4, 60),
                new LineTo(18, 16),
                new HLineTo(40),
                new ArcTo(12, 12, 0, 64, 16, false, true),
                new VLineTo(32),
                relative,
                new QuadCurveTo(104, 30, 100, 52),
                new CubicCurveTo(80, 76, 40, 36, 4, 60),
                new ClosePath());
        paint(path, PURPLE_L, PURPLE, 2);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return tile("Path: Move Line HLine Arc VLine rel.Line Quad Cubic Close", W, H, path);
    }

    private static Node badge(Properties paths) {
        SVGPath svg = new SVGPath();
        svg.setContent(path(paths, "badge"));
        paint(svg, TEAL_L, TEAL, 2);
        svg.setFillRule(FillRule.EVEN_ODD);
        return tile("SVGPath (resource), 3 sub-paths", W, H, svg);
    }

    private static Shape unionShape() {
        return Shape.union(new Circle(28, 30, 26), new Rectangle(30, 10, 60, 40));
    }

    private static Node union() {
        return tile("Shape.union", W, H, paint(unionShape(), BLUE_L, BLUE, 2));
    }

    private static Node intersect() {
        Shape shape = Shape.intersect(new Circle(28, 30, 26), new Rectangle(30, 10, 60, 40));
        return tile("Shape.intersect", W, H, outline(new Circle(28, 30, 26)), outline(new Rectangle(30, 10, 60, 40)),
                paint(shape, RED_L, RED, 2));
    }

    private static Node subtract() {
        Shape shape = Shape.subtract(new Rectangle(30, 10, 60, 40), new Circle(28, 30, 26));
        return tile("Shape.subtract", W, H, outline(new Circle(28, 30, 26)), paint(shape, GREEN_L, GREEN, 2));
    }

    // ---- Strokes ----

    private static Node strokeWidths() {
        Group group = new Group();
        double[] widths = { 1, 2, 4, 8 };
        for (int i = 0; i < widths.length; i++) {
            Line line = new Line(0, 6 + i * 16, 96, 6 + i * 16);
            line.setStroke(BLUE);
            line.setStrokeWidth(widths[i]);
            group.getChildren().add(line);
        }
        return tile("strokeWidth 1 / 2 / 4 / 8", W, H, group);
    }

    private static Node dashArrays() {
        Group group = new Group();
        // BUTT caps : the default SQUARE caps extend every dash by half the stroke width and hide short gaps
        double[][] arrays = { { 10, 5 }, { 2, 4 }, { 16, 4, 4, 4 }, { 0, 7 } };
        for (int i = 0; i < arrays.length; i++) {
            Line line = new Line(0, 6 + i * 16, 96, 6 + i * 16);
            line.setStroke(RED);
            line.setStrokeWidth(i == 3 ? 5 : 3);
            // zero-length dashes with ROUND caps draw dots
            line.setStrokeLineCap(i == 3 ? StrokeLineCap.ROUND : StrokeLineCap.BUTT);
            for (double d : arrays[i]) {
                line.getStrokeDashArray().add(d);
            }
            group.getChildren().add(line);
        }
        return tile("dash 10,5 | 2,4 | 16,4,4,4 | 0,7 round", W, H, group);
    }

    private static Node dashOffsets() {
        Group group = new Group();
        for (int i = 0; i < 4; i++) {
            Line line = new Line(0, 6 + i * 16, 96, 6 + i * 16);
            line.setStroke(GREEN);
            line.setStrokeWidth(5);
            line.setStrokeLineCap(StrokeLineCap.BUTT);
            line.getStrokeDashArray().addAll(12.0, 6.0);
            line.setStrokeDashOffset(i * 4);
            group.getChildren().add(line);
        }
        Line guide = new Line(0, 0, 0, 60);
        guide.setStroke(Color.BLACK);
        group.getChildren().add(guide);
        return tile("dash 12,6 offset 0 / 4 / 8 / 12", W, H, group);
    }

    private static Node lineCaps() {
        Group group = new Group();
        StrokeLineCap[] caps = StrokeLineCap.values();
        for (int i = 0; i < caps.length; i++) {
            double y = 8 + i * 22;
            Line line = new Line(18, y, 82, y);
            line.setStroke(TEAL_L);
            line.setStrokeWidth(14);
            line.setStrokeLineCap(caps[i]);
            Line center = new Line(18, y, 82, y);
            center.setStroke(TEAL);
            group.getChildren().addAll(line, center);
        }
        group.getChildren().addAll(guide(18, 0, 18, 60), guide(82, 0, 82, 60));
        return tile("cap SQUARE / BUTT / ROUND", W, H, group);
    }

    private static Node lineJoins() {
        Group group = new Group();
        StrokeLineJoin[] joins = StrokeLineJoin.values();
        for (int i = 0; i < joins.length; i++) {
            double x = i * 34;
            Polyline line = new Polyline(x, 52, x + 13, 10, x + 26, 52);
            line.setFill(null);
            line.setStroke(ORANGE);
            line.setStrokeWidth(9);
            line.setStrokeLineJoin(joins[i]);
            Polyline center = new Polyline(x, 52, x + 13, 10, x + 26, 52);
            center.setFill(null);
            center.setStroke(Color.WHITE);
            group.getChildren().addAll(line, center);
        }
        return tile("join MITER / BEVEL / ROUND", W, H, group);
    }

    private static Node strokeTypes() {
        Group group = new Group();
        StrokeType[] types = { StrokeType.INSIDE, StrokeType.OUTSIDE, StrokeType.CENTERED };
        // geometry 20x20 at x = 0 / 36 / 76 : the OUTSIDE (32 px) and CENTERED (26 px) strokes do not touch
        double[] xs = { 0, 36, 76 };
        for (int i = 0; i < types.length; i++) {
            Rectangle rect = paint(new Rectangle(xs[i], 0, 20, 20), ORANGE_L, Color.web("#e65100b0"), 6);
            rect.setStrokeType(types[i]);
            Rectangle geometry = new Rectangle(xs[i], 0, 20, 20);
            geometry.setFill(null);
            geometry.setStroke(Color.BLACK);
            geometry.getStrokeDashArray().addAll(2.0, 2.0);
            group.getChildren().addAll(rect, geometry);
        }
        return tile("StrokeType INSIDE / OUTSIDE / CENTERED", W, H, group);
    }

    private static Node miterLimits() {
        Group group = new Group();
        double[] limits = { 10, 2 };
        for (int i = 0; i < limits.length; i++) {
            double x = i * 48;
            Polyline line = new Polyline(x, 56, x + 14, 12, x + 28, 56);
            line.setFill(null);
            line.setStroke(PURPLE);
            line.setStrokeWidth(8);
            line.setStrokeMiterLimit(limits[i]);
            group.getChildren().add(line);
        }
        return tile("miterLimit 10 | 2", W, H, group);
    }

    private static Node cssStroke() {
        Rectangle rect = new Rectangle(64, 36);
        rect.getStyleClass().add("css-stroke");
        return tile("CSS: dash, cap, join, OUTSIDE", W, H, rect);
    }

    // ---- Fill rules, boolean operations, CSS shapes ----

    private static Path pentagram(double cx, FillRule rule) {
        Path path = new Path();
        for (int k = 0; k < 5; k++) {
            int index = (k * 2) % 5;
            double angle = StrictMath.toRadians(-90 + index * 72);
            double x = cx + 26 * StrictMath.cos(angle);
            double y = 26 * StrictMath.sin(angle);
            path.getElements().add(k == 0 ? new MoveTo(x, y) : new LineTo(x, y));
        }
        path.getElements().add(new ClosePath());
        path.setFillRule(rule);
        return paint(path, BLUE_L, BLUE, 1.5);
    }

    private static Node pathFillRules() {
        return tile("Path NON_ZERO | EVEN_ODD", W, H, pentagram(0, FillRule.NON_ZERO), pentagram(58, FillRule.EVEN_ODD));
    }

    private static Node svgFillRules(Properties paths) {
        SVGPath nonZero = paint(new SVGPath(), RED_L, RED, 1.5);
        nonZero.setContent(path(paths, "rings"));
        SVGPath evenOdd = paint(new SVGPath(), RED_L, RED, 1.5);
        evenOdd.setContent(path(paths, "rings"));
        evenOdd.setFillRule(FillRule.EVEN_ODD);
        evenOdd.setTranslateX(60);
        return tile("SVGPath NON_ZERO | EVEN_ODD", W, H, nonZero, evenOdd);
    }

    private static Shape fxText() {
        Text text = new Text(12, 47, "FX");
        text.setFont(Font.font("System", FontWeight.BOLD, 46));
        return text;
    }

    private static Node textSubtract() {
        Shape shape = Shape.subtract(new Rectangle(0, 0, 96, 58), fxText());
        return tile("subtract(rect, Text \"FX\")", W, H, paint(shape, BLUE, Color.web("#0d2a5c"), 1));
    }

    private static Node xor() {
        Circle a = new Circle(28, 30, 26);
        Circle b = new Circle(62, 30, 26);
        Shape xor = Shape.subtract(Shape.union(a, b), Shape.intersect(new Circle(28, 30, 26), new Circle(62, 30, 26)));
        return tile("xor = subtract(union, intersect)", W, H, paint(xor, TEAL_L, TEAL, 2));
    }

    private static Node unionOfThree() {
        Shape shape = Shape.union(Shape.union(new Circle(20, 38, 20), new Circle(46, 20, 20)), new Circle(72, 38, 20));
        paint(shape, ORANGE_L, ORANGE, 2);
        shape.setStrokeLineCap(StrokeLineCap.BUTT);
        shape.getStrokeDashArray().addAll(6.0, 3.0);
        return tile("union x3, dashed stroke", W, H, shape);
    }

    private static Node smooth() {
        Group group = new Group();
        for (int i = 0; i < 2; i++) {
            Circle circle = paint(new Circle(24 + i * 54, 28, 22), GREEN_L, GREEN, 2);
            Line line = new Line(i * 54, 58, 48 + i * 54, 0);
            line.setStroke(GREEN);
            line.setStrokeWidth(2);
            circle.setSmooth(i == 0);
            line.setSmooth(i == 0);
            group.getChildren().addAll(circle, line);
        }
        return tile("smooth true | false", W, H, group);
    }

    private static Node cssShape() {
        Region region = new Region();
        region.getStyleClass().add("css-shape");
        return tile("Region -fx-shape (CSS)", W, H, region);
    }

    private static Node heartStripes(Properties paths) {
        SVGPath heart = new SVGPath();
        heart.setContent(path(paths, "heart"));
        Shape stripes = new Rectangle(0, 0, 100, 5);
        for (int i = 1; i < 9; i++) {
            stripes = Shape.union(stripes, new Rectangle(0, i * 11, 100, 5));
        }
        Shape shape = Shape.intersect(heart, stripes);
        shape.setFill(RED);
        SVGPath outline = new SVGPath();
        outline.setContent(path(paths, "heart"));
        outline.setFill(null);
        outline.setStroke(RED);
        outline.setStrokeWidth(2);
        Group group = new Group(shape, outline);
        group.setScaleX(0.7);
        group.setScaleY(0.7);
        return tile("intersect(SVGPath, stripes)", W, H, group);
    }

    // ---- Checks ----

    private static List<Check> geometryChecks(Properties paths) {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.run("union(circle, rect) bounds", () -> Tiles.bounds(unionShape().getBoundsInLocal())));
        checks.add(Checks.run("subtract(rect, Text) elements",
                () -> ((Path) Shape.subtract(new Rectangle(0, 0, 96, 58), fxText())).getElements().size()));
        checks.add(Checks.expect("pentagram NON_ZERO contains center", true,
                () -> pentagram(0, FillRule.NON_ZERO).contains(0, 0)));
        checks.add(Checks.expect("pentagram EVEN_ODD contains center", false,
                () -> pentagram(0, FillRule.EVEN_ODD).contains(0, 0)));
        checks.add(Checks.run("SVGPath badge bounds", () -> {
            SVGPath svg = new SVGPath();
            svg.setContent(path(paths, "badge"));
            return Tiles.bounds(svg.getBoundsInLocal());
        }));
        checks.add(Checks.expect("OUTSIDE stroke 8 on 24x24: width", "40.00", () -> {
            Rectangle rect = new Rectangle(24, 24);
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(8);
            rect.setStrokeType(StrokeType.OUTSIDE);
            return Tiles.num(rect.getLayoutBounds().getWidth());
        }));
        return checks;
    }

    private static List<Check> cssChecks() {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("paths.properties keys", "[badge, heart, rings]",
                () -> new java.util.TreeSet<>(paths().stringPropertyNames()).toString()));
        checks.add(Checks.expect("-fx-stroke-dash-array", "[12.0, 8.0, 2.0, 8.0]",
                () -> Tiles.css(new Rectangle(10, 10), null, "css-stroke").getStrokeDashArray().toString()));
        checks.add(Checks.expect("-fx-stroke-line-cap/join/type", "ROUND BEVEL OUTSIDE", () -> {
            Rectangle rect = Tiles.css(new Rectangle(10, 10), null, "css-stroke");
            return rect.getStrokeLineCap() + " " + rect.getStrokeLineJoin() + " " + rect.getStrokeType();
        }));
        checks.add(Checks.expect("-fx-stroke-dash-offset / -fx-stroke-miter-limit", "3.00 / 4.00", () -> {
            Rectangle rect = Tiles.css(new Rectangle(10, 10), null, "css-stroke");
            return Tiles.num(rect.getStrokeDashOffset()) + " / " + Tiles.num(rect.getStrokeMiterLimit());
        }));
        checks.add(Checks.expect("inline -fx-fill / -fx-stroke-width", "#ff000080 7.00", () -> {
            Rectangle rect = Tiles.css(new Rectangle(10, 10), "-fx-fill: #ff000080; -fx-stroke-width: 7;");
            return Tiles.hex((Color) rect.getFill()) + " " + Tiles.num(rect.getStrokeWidth());
        }));
        checks.add(Checks.run("-fx-shape (Region)", () -> {
            Region region = Tiles.css(new Region(), null, "css-shape");
            Shape shape = region.getShape();
            return shape.getClass().getSimpleName() + " " + Tiles.bounds(shape.getBoundsInLocal());
        }));
        return checks;
    }

    // ---- Helpers ----

    private static double[] starPoints(int spikes, double outer, double inner) {
        double[] points = new double[spikes * 4];
        for (int i = 0; i < spikes * 2; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double angle = StrictMath.toRadians(-90 + i * 180.0 / spikes);
            points[i * 2] = r * StrictMath.cos(angle);
            points[i * 2 + 1] = r * StrictMath.sin(angle);
        }
        return points;
    }

    private static Line controls(double x1, double y1, double x2, double y2) {
        Line line = guide(x1, y1, x2, y2);
        line.getStrokeDashArray().addAll(3.0, 3.0);
        return line;
    }

    private static Line guide(double x1, double y1, double x2, double y2) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(GUIDE);
        return line;
    }

    private static Circle handle(double x, double y) {
        return paint(new Circle(x, y, 3), Color.WHITE, Color.web("#37474f"), 1);
    }

    private static Shape outline(Shape shape) {
        shape.setFill(null);
        shape.setStroke(GUIDE);
        shape.getStrokeDashArray().addAll(3.0, 3.0);
        return shape;
    }

    /**
     * SVG path data of {@code /showcase/graphics/paths.properties}. A missing or unreadable resource leaves the paths
     * empty (the page still renders) and fails the "paths.properties keys" check.
     */
    static Properties paths() {
        Properties properties = new Properties();
        URL url = ShapesPage.class.getResource("/showcase/graphics/paths.properties");
        if (url != null) {
            try (InputStream in = url.openStream()) {
                properties.load(in);
            } catch (IOException e) {
                properties.clear();
            }
        }
        return properties;
    }

    static String path(Properties paths, String key) {
        return paths.getProperty(key, "");
    }
}
