package io.quarkiverse.fx.showcase.pages.graphics;

import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.W;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.row;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.section;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.tile;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

@Singleton
public class PaintPage implements FeaturePage {

    private static final double H = 44;
    private static final double SWATCH_H = 28;

    @Override
    public String id() {
        return "graphics-paint";
    }

    @Override
    public String title() {
        return "Colors, Gradients & Patterns";
    }

    @Override
    public String category() {
        return Categories.GRAPHICS;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        Image tile = Tiles.image("tile.png");
        Image pattern = Tiles.image("pattern.png");
        return Tiles.page(
                section("Color: web(), rgb(), hsb(), color(), named, derived, with opacity (over a checkerboard)"),
                row(swatch("web(\"#1e88e5\")", Color.web("#1e88e5")),
                        swatch("web(\"#1e88e580\")", Color.web("#1e88e580")),
                        swatch("web(\"rgb(229,57,53)\")", Color.web("rgb(229,57,53)")),
                        swatch("rgba(67,160,71,0.5)", Color.web("rgba(67,160,71,0.5)")),
                        swatch("hsl(270,60%,50%)", Color.web("hsl(270,60%,50%)")),
                        swatch("hsla(30,100%,50%,0.6)", Color.web("hsla(30,100%,50%,0.6)")),
                        swatch("web(\"0x795548\")", Color.web("0x795548")),
                        swatch("web(\"orchid\")", Color.web("orchid"))),
                row(swatch("web(\"steelblue\", 0.4)", Color.web("steelblue", 0.4)),
                        swatch("rgb(255,193,7)", Color.rgb(255, 193, 7)),
                        swatch("rgb(0,150,136,0.35)", Color.rgb(0, 150, 136, 0.35)),
                        swatch("hsb(190,0.8,0.9)", Color.hsb(190, 0.8, 0.9)),
                        swatch("hsb(330,0.7,0.9,0.5)", Color.hsb(330, 0.7, 0.9, 0.5)),
                        swatch("color(.2,.3,.8,.7)", Color.color(0.2, 0.3, 0.8, 0.7)),
                        swatch("CORAL.darker()", Color.CORAL.darker()),
                        swatch("TEAL.deriveColor(..)", Color.TEAL.deriveColor(40, 1, 1.6, 0.6))),
                section("LinearGradient: proportional and absolute, every CycleMethod"),
                row(linear("prop. NO_CYCLE", CycleMethod.NO_CYCLE, true),
                        linear("prop. REFLECT", CycleMethod.REFLECT, true),
                        linear("prop. REPEAT", CycleMethod.REPEAT, true),
                        linear("absolute NO_CYCLE", CycleMethod.NO_CYCLE, false),
                        linear("absolute REFLECT", CycleMethod.REFLECT, false),
                        linear("absolute REPEAT", CycleMethod.REPEAT, false),
                        rainbow(),
                        fadeOut()),
                section("RadialGradient: proportional and absolute, focus, every CycleMethod"),
                row(radial("prop. NO_CYCLE r=.25", CycleMethod.NO_CYCLE),
                        radial("prop. REFLECT r=.25", CycleMethod.REFLECT),
                        radial("prop. REPEAT r=.25", CycleMethod.REPEAT),
                        focus(),
                        radialAbsolute("absolute REPEAT r=10", CycleMethod.REPEAT),
                        radialAbsolute("absolute REFLECT r=10", CycleMethod.REFLECT),
                        sphere(),
                        radialStops()),
                section("valueOf() parsing and CSS paints"),
                row(rect("LinearGradient.valueOf 1", LinearGradient.valueOf(LINEAR_1)),
                        rect("LinearGradient.valueOf 2", LinearGradient.valueOf(LINEAR_2)),
                        rect("RadialGradient.valueOf 1", RadialGradient.valueOf(RADIAL_1)),
                        rect("RadialGradient.valueOf 2", RadialGradient.valueOf(RADIAL_2)),
                        rect("Paint.valueOf(\"#ff6f00cc\")", Paint.valueOf("#ff6f00cc")),
                        rect("Paint.valueOf(radial..)", Paint.valueOf(PAINT_RADIAL)),
                        cssShape("CSS -fx-fill linear", new Rectangle(104, 34), "css-linear-fill"),
                        cssShape("CSS -fx-fill radial", new Rectangle(104, 34), "css-radial-fill")),
                section("ImagePattern (tile.png 32x32, pattern.png 256x256) and CSS image patterns"),
                row(rect("tile.png 32px tiles", new ImagePattern(tile, 0, 0, 32, 32, false)),
                        rect("tile.png 16px tiles", new ImagePattern(tile, 0, 0, 16, 16, false)),
                        rect("pattern.png stretched", new ImagePattern(pattern)),
                        rect("pattern.png 0.5 x 0.5", new ImagePattern(pattern, 0, 0, 0.5, 0.5, true)),
                        circlePattern(pattern),
                        strokePattern(tile),
                        region("CSS image-pattern()", "css-image-pattern"),
                        region("CSS repeating-image-pattern()", "css-repeating-pattern")),
                section("CSS backgrounds, gradients on strokes and text"),
                row(region("CSS background layers", "css-background-layers"),
                        ladder(),
                        region("CSS derive()", "css-derived"),
                        gradientStroke(),
                        dashedRadialStroke(),
                        gradientText(),
                        patternText(pattern)),
                Tiles.checks(Checks.view("Colors and gradients", colorChecks()),
                        Checks.view("Images and CSS paints", cssChecks())));
    }

    private static final String LINEAR_1 = "linear-gradient(to bottom right, #ff512f, #dd2476)";
    private static final String LINEAR_2 = "linear-gradient(from 0px 0px to 14px 8px, repeat, #4facfe 0%, #00f2fe 50%, #1a237e 100%)";
    private static final String RADIAL_1 = "radial-gradient(focus-angle 45deg, focus-distance 50%, center 50% 50%, radius 50%, white, #2e7d32)";
    private static final String RADIAL_2 = "radial-gradient(center 20px 18px, radius 12px, reflect, #ffd54f, #6a1b9a)";
    private static final String PAINT_RADIAL = "radial-gradient(radius 40%, repeat, gold, crimson)";

    // ---- Colors ----

    private static Node swatch(String caption, Color color) {
        Rectangle checker = new Rectangle(W - 2, SWATCH_H - 2, Tiles.checker(5));
        Rectangle swatch = new Rectangle(12, 5, W - 26, SWATCH_H - 12);
        swatch.setFill(color);
        return tile(caption, W, SWATCH_H, checker, swatch);
    }

    // ---- Gradients ----

    private static Node rect(String caption, Paint paint) {
        return tile(caption, W, H, new Rectangle(104, 34, paint));
    }

    private static Node linear(String caption, CycleMethod cycle, boolean proportional) {
        LinearGradient gradient = proportional
                ? new LinearGradient(0.35, 0, 0.6, 0, true, cycle, new Stop(0, Color.web("#ffeb3b")), new Stop(1, Color.web("#d32f2f")))
                : new LinearGradient(0, 0, 22, 0, false, cycle, new Stop(0, Color.web("#ffeb3b")), new Stop(1, Color.web("#d32f2f")));
        return rect(caption, gradient);
    }

    private static Node rainbow() {
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED), new Stop(0.2, Color.ORANGE), new Stop(0.4, Color.YELLOW),
                new Stop(0.6, Color.GREEN), new Stop(0.8, Color.BLUE), new Stop(1, Color.VIOLET));
        return rect("diagonal, 6 stops", gradient);
    }

    private static Node fadeOut() {
        LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1565c0")), new Stop(1, Color.web("#1565c000")));
        Rectangle checker = new Rectangle(104, 34, Tiles.checker(5));
        return tile("stops with alpha", W, H, checker, new Rectangle(104, 34, gradient));
    }

    private static Node radial(String caption, CycleMethod cycle) {
        RadialGradient gradient = new RadialGradient(0, 0, 0.5, 0.5, 0.25, true, cycle,
                new Stop(0, Color.WHITE), new Stop(1, Color.web("#1565c0")));
        return rect(caption, gradient);
    }

    private static Node focus() {
        RadialGradient gradient = new RadialGradient(45, 0.7, 0.5, 0.5, 0.6, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.5, Color.web("#ffb74d")), new Stop(1, Color.web("#bf360c")));
        return rect("focus 45°, distance .7", gradient);
    }

    private static Node radialAbsolute(String caption, CycleMethod cycle) {
        RadialGradient gradient = new RadialGradient(0, 0, 30, 17, 10, false, cycle,
                new Stop(0, Color.web("#e1f5fe")), new Stop(1, Color.web("#00695c")));
        return rect(caption, gradient);
    }

    private static Node sphere() {
        RadialGradient gradient = new RadialGradient(0, 0, 0.35, 0.3, 0.7, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.25, Color.web("#ef5350")), new Stop(1, Color.web("#4a0000")));
        return tile("off-center, on Circle", W, H, new Circle(19, gradient));
    }

    private static Node radialStops() {
        RadialGradient gradient = new RadialGradient(0, 0, 52, 17, 30, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.VIOLET), new Stop(0.25, Color.BLUE), new Stop(0.5, Color.GREEN),
                new Stop(0.75, Color.YELLOW), new Stop(1, Color.RED));
        return rect("absolute NO_CYCLE, 5 stops", gradient);
    }

    // ---- Image patterns ----

    private static Node circlePattern(Image pattern) {
        Circle circle = new Circle(19, new ImagePattern(pattern));
        circle.setStroke(Color.web("#37474f"));
        return tile("pattern.png on Circle", W, H, circle);
    }

    private static Node strokePattern(Image tile) {
        Rectangle rect = new Rectangle(88, 24);
        rect.setFill(Color.web("#fff8e1"));
        rect.setStroke(new ImagePattern(tile, 0, 0, 16, 16, false));
        rect.setStrokeWidth(8);
        rect.setArcWidth(16);
        rect.setArcHeight(16);
        return tile("ImagePattern stroke", W, H, rect);
    }

    // ---- CSS ----

    private static Node cssShape(String caption, Shape shape, String styleClass) {
        shape.getStyleClass().add(styleClass);
        return tile(caption, W, H, shape);
    }

    private static Node region(String caption, String styleClass) {
        Region region = new Region();
        region.setPrefSize(104, 34);
        region.getStyleClass().add(styleClass);
        return tile(caption, W, H, region);
    }

    // ---- Strokes and text ----

    private static Node gradientStroke() {
        Rectangle rect = new Rectangle(84, 22);
        rect.setFill(null);
        rect.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8e24aa")), new Stop(1, Color.web("#00acc1"))));
        rect.setStrokeWidth(8);
        return tile("linear gradient stroke", W, H, rect);
    }

    private static Node dashedRadialStroke() {
        Circle circle = new Circle(16);
        circle.setFill(null);
        circle.setStroke(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0.6, Color.web("#fff176")), new Stop(1, Color.web("#e65100"))));
        circle.setStrokeWidth(7);
        // BUTT caps : with the default SQUARE caps (dash 6 + 7) the 4 px gaps are completely covered
        circle.setStrokeLineCap(StrokeLineCap.BUTT);
        circle.getStrokeDashArray().addAll(7.0, 5.0);
        return tile("dashed radial stroke", W, H, circle);
    }

    private static Node gradientText() {
        Text text = new Text("Gradient text");
        text.setFont(Font.font("System", FontWeight.BOLD, 30));
        text.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f12711")), new Stop(0.5, Color.web("#f5af19")), new Stop(1, Color.web("#1e88e5"))));
        text.setStroke(new RadialGradient(0, 0, 0.5, 0.5, 0.6, true, CycleMethod.REFLECT,
                new Stop(0, Color.web("#311b92")), new Stop(1, Color.web("#00bfa5"))));
        text.setStrokeWidth(1.2);
        return tile("Text: linear fill, radial stroke", W * 2 + 8, H, text);
    }

    private static Node patternText(Image pattern) {
        Text text = new Text("FX");
        text.setFont(Font.font("System", FontWeight.BOLD, 38));
        text.setFill(new ImagePattern(pattern, 0, 0, 1, 1, true));
        text.setStroke(Color.web("#263238"));
        text.setStrokeWidth(0.8);
        return tile("Text: ImagePattern fill", W, H, text);
    }

    // ---- Checks ----

    private static List<Check> colorChecks() {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.run("web(\"hsl(270,60%,50%)\")", () -> Tiles.hex(Color.web("hsl(270,60%,50%)"))));
        checks.add(Checks.run("hsb(190,.8,.9) / #1e88e580 opacity",
                () -> Tiles.hex(Color.hsb(190, 0.8, 0.9)) + " / " + Tiles.num(Color.web("#1e88e580").getOpacity())));
        checks.add(Checks.expect("CORAL equals web(\"coral\")", true, () -> Color.CORAL.equals(Color.web("coral"))));
        checks.add(Checks.run("LinearGradient.valueOf 2", () -> {
            LinearGradient g = LinearGradient.valueOf(LINEAR_2);
            return g.getCycleMethod() + (g.isProportional() ? " proportional " : " absolute ") + g.getStops().size()
                    + " stops, to " + Tiles.num(g.getEndX()) + "," + Tiles.num(g.getEndY());
        }));
        checks.add(Checks.run("RadialGradient.valueOf 1", () -> {
            RadialGradient g = RadialGradient.valueOf(RADIAL_1);
            return "focus " + Tiles.num(g.getFocusAngle()) + "° " + Tiles.num(g.getFocusDistance()) + ", r "
                    + Tiles.num(g.getRadius()) + ", " + g.getStops().size() + " stops";
        }));
        return checks;
    }

    private static List<Check> cssChecks() {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.run("Image tile.png / pattern.png",
                () -> Tiles.size(Tiles.image("tile.png")) + " / " + Tiles.size(Tiles.image("pattern.png"))));
        checks.add(Checks.run("CSS -fx-fill linear / radial", () -> {
            Paint linear = Tiles.css(new Rectangle(10, 10), null, "css-linear-fill").getFill();
            RadialGradient radial = (RadialGradient) Tiles.css(new Rectangle(10, 10), null, "css-radial-fill").getFill();
            return ((LinearGradient) linear).getStops().size() + " stops / " + radial.getCycleMethod() + " focus "
                    + Tiles.num(radial.getFocusAngle());
        }));
        checks.add(Checks.run("CSS ladder() dark / light, derive()", () -> Tiles.hex(ladderText("css-ladder")) + " / "
                + Tiles.hex(ladderText("css-ladder", "light")) + ", " + Tiles.hex(backgroundColor("css-derived", 1))));
        checks.add(Checks.run("CSS image-pattern(\"/showcase/..\")", () -> describe(background("css-image-pattern", 0))));
        checks.add(Checks.run("CSS repeating-image-pattern()", () -> describe(background("css-repeating-pattern", 0))));
        checks.add(Checks.run("CSS url(\"../images/icon-16.png\")", () -> {
            Region region = Tiles.css(new Region(), null, "css-background-layers");
            return Tiles.size(region.getBackground().getImages().get(0).getImage());
        }));
        return checks;
    }

    private static Node ladder() {
        Label dark = new Label("dark");
        dark.getStyleClass().add("css-ladder");
        Label light = new Label("light");
        light.getStyleClass().addAll("css-ladder", "light");
        light.setLayoutX(48);
        return tile("CSS ladder() text fill", W, H, dark, light);
    }

    private static Color ladderText(String... styleClasses) {
        return (Color) Tiles.css(new Label("x"), null, styleClasses).getTextFill();
    }

    private static Paint background(String styleClass, int index) {
        Region region = Tiles.css(new Region(), null, styleClass);
        return region.getBackground().getFills().get(index).getFill();
    }

    private static Color backgroundColor(String styleClass, int index) {
        return (Color) background(styleClass, index);
    }

    private static String describe(Paint paint) {
        if (paint instanceof ImagePattern pattern) {
            return "ImagePattern " + Tiles.size(pattern.getImage()) + (pattern.isProportional() ? " proportional" : " absolute");
        }
        return String.valueOf(paint);
    }
}
