package io.quarkiverse.fx.showcase.pages.graphics;

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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.ImageInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

@Singleton
public class BlendModesPage implements FeaturePage {

    private static final double TW = 106;
    private static final double H = 66;

    private static final Color BOTTOM = Color.web("#3366cc");
    // every channel differs from the bottom color, so that RED, GREEN and BLUE each change the result
    private static final Color TOP = Color.web("#cc9933");

    @Override
    public String id() {
        return "graphics-blend-modes";
    }

    @Override
    public String title() {
        return "Blend Modes & Opacity";
    }

    @Override
    public String category() {
        return Categories.GRAPHICS;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Node build() {
        Image photo = Tiles.image("photo.jpg", 78, 52);
        Image wheel = colorWheel(52);

        List<Node> effects = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        for (BlendMode mode : BlendMode.values()) {
            effects.add(blendEffect(mode, photo, wheel));
            nodes.add(nodeBlend(mode));
        }
        effects.add(inputs(photo, wheel));
        nodes.add(reference());

        return Tiles.page(
                section("Blend effect: bottomInput = ImageInput(photo), topInput = ImageInput(color wheel)"),
                grid(effects),
                section("Node.setBlendMode: a circle over a gradient rectangle, in a Group isolated with SRC_OVER"),
                grid(nodes),
                section("Opacity and group blending"),
                row(individualOpacity(), groupOpacity(), groupBlend(), nonIsolated(), isolated(), blendOpacity(photo, wheel),
                        cssBlend(), nestedBlend()),
                Tiles.checks(Checks.view("Blend API and CSS", apiChecks()),
                        Checks.view("Blend of #3366cc (bottom) and #cc9933 (top), all 17 modes", pixelChecks())));
    }

    private static VBox grid(List<Node> tiles) {
        HBox first = new HBox(8);
        HBox second = new HBox(8);
        for (int i = 0; i < tiles.size(); i++) {
            (i < 9 ? first : second).getChildren().add(tiles.get(i));
        }
        return new VBox(5, first, second);
    }

    /**
     * A color wheel with transparent corners (hue by angle, saturation by radius), generated pixel by pixel.
     */
    static WritableImage colorWheel(int size) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();
        double r = size / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - r;
                double dy = y + 0.5 - r;
                double distance = StrictMath.sqrt(dx * dx + dy * dy);
                if (distance > r) {
                    writer.setArgb(x, y, 0);
                } else {
                    double hue = (StrictMath.toDegrees(StrictMath.atan2(dy, dx)) + 360) % 360;
                    writer.setColor(x, y, Color.hsb(hue, distance / r, 1.0));
                }
            }
        }
        return image;
    }

    private static Blend blend(BlendMode mode, Image photo, Image wheel) {
        return new Blend(mode, new ImageInput(photo, 0, 12), new ImageInput(wheel, 42, 0));
    }

    private static Node blendEffect(BlendMode mode, Image photo, Image wheel) {
        Rectangle node = new Rectangle(94, 64, Color.TRANSPARENT);
        node.setEffect(blend(mode, photo, wheel));
        return tile(mode.name(), TW, H, node);
    }

    private static Node inputs(Image photo, Image wheel) {
        ImageView bottom = new ImageView(photo);
        bottom.setFitWidth(52);
        bottom.setFitHeight(35);
        bottom.setY(10);
        ImageView top = new ImageView(wheel);
        top.setFitWidth(40);
        top.setFitHeight(40);
        top.setX(58);
        top.setY(8);
        return tile("inputs: bottom | top", TW, H, bottom, top);
    }

    private static Rectangle gradientRect() {
        return Tiles.rect(0, 8, 68, 50, new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffeb3b")), new Stop(0.5, Color.web("#26a69a")), new Stop(1, Color.web("#283593"))));
    }

    private static Circle topCircle() {
        return new Circle(66, 33, 26, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ff4081")), new Stop(1, Color.web("#ffab40"))));
    }

    private static Node nodeBlend(BlendMode mode) {
        Circle circle = topCircle();
        circle.setBlendMode(mode);
        Group group = new Group(gradientRect(), circle);
        group.setBlendMode(BlendMode.SRC_OVER);
        return tile(mode.name(), TW, H, group);
    }

    private static Node reference() {
        return tile("reference: blendMode null", TW, H, new Group(gradientRect(), topCircle()));
    }

    // ---- Opacity and group blending ----

    private static Circle circle(double x, double y, String color) {
        return new Circle(x, y, 22, Color.web(color));
    }

    private static Node individualOpacity() {
        Circle a = circle(24, 26, "#e53935");
        Circle b = circle(52, 26, "#1e88e5");
        Circle c = circle(38, 48, "#43a047");
        a.setOpacity(0.5);
        b.setOpacity(0.5);
        c.setOpacity(0.5);
        return tile("each node opacity 0.5", Tiles.W, H, a, b, c);
    }

    private static Node groupOpacity() {
        Group group = new Group(circle(24, 26, "#e53935"), circle(52, 26, "#1e88e5"), circle(38, 48, "#43a047"));
        group.setOpacity(0.5);
        return tile("Group opacity 0.5", Tiles.W, H, group);
    }

    private static Node groupBlend() {
        Rectangle background = gradientRect();
        Group group = new Group(circle(50, 22, "#e53935"), circle(76, 40, "#1e88e5"));
        group.setBlendMode(BlendMode.MULTIPLY);
        return tile("Group MULTIPLY over a sibling", Tiles.W, H, background, group);
    }

    private static Node nonIsolated() {
        Rectangle background = Tiles.rect(0, 0, 90, 60, Color.web("#ffe082"));
        Circle a = circle(30, 30, "#7e57c2");
        Circle b = circle(58, 30, "#26c6da");
        b.setBlendMode(BlendMode.DIFFERENCE);
        Group group = new Group(a, b);
        return tile("DIFFERENCE, group mode null", Tiles.W, H, background, group);
    }

    private static Node isolated() {
        Rectangle background = Tiles.rect(0, 0, 90, 60, Color.web("#ffe082"));
        Circle a = circle(30, 30, "#7e57c2");
        Circle b = circle(58, 30, "#26c6da");
        b.setBlendMode(BlendMode.DIFFERENCE);
        Group group = new Group(a, b);
        group.setBlendMode(BlendMode.SRC_OVER);
        return tile("DIFFERENCE, group SRC_OVER", Tiles.W, H, background, group);
    }

    private static Node blendOpacity(Image photo, Image wheel) {
        Blend blend = blend(BlendMode.MULTIPLY, photo, wheel);
        blend.setOpacity(0.5);
        Rectangle node = new Rectangle(94, 64, Color.TRANSPARENT);
        node.setEffect(blend);
        return tile("Blend MULTIPLY opacity 0.5", Tiles.W, H, node);
    }

    private static Node cssBlend() {
        Rectangle background = gradientRect();
        Circle top = topCircle();
        top.getStyleClass().add("css-blend");
        Group group = new Group(background, top);
        group.setBlendMode(BlendMode.SRC_OVER);
        return tile("CSS -fx-blend-mode: multiply", Tiles.W, H, group);
    }

    private static Node nestedBlend() {
        Blend inner = new Blend(BlendMode.SCREEN, new ColorInput(0, 0, 60, 60, Color.web("#1a237e")),
                new ColorInput(20, 10, 60, 50, Color.web("#c62828")));
        Blend outer = new Blend(BlendMode.EXCLUSION, inner, new ColorInput(40, 20, 50, 40, Color.web("#fdd835")));
        Rectangle node = new Rectangle(90, 60, Color.TRANSPARENT);
        node.setEffect(outer);
        return tile("EXCLUSION of (SCREEN of inputs)", Tiles.W, H, node);
    }

    // ---- Checks ----

    private static List<Check> apiChecks() {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("BlendMode.values()", 17, () -> BlendMode.values().length));
        checks.add(Checks.expect("default Node blendMode / Blend mode", "null / SRC_OVER",
                () -> new Rectangle().getBlendMode() + " / " + new Blend().getMode()));
        checks.add(Checks.expect("stylesheet -fx-blend-mode, -fx-opacity", "MULTIPLY 0.80", () -> {
            Rectangle rect = Tiles.css(new Rectangle(10, 10), null, "css-blend");
            return rect.getBlendMode() + " " + Tiles.num(rect.getOpacity());
        }));
        checks.add(Checks.expect("inline -fx-blend-mode: soft-light", BlendMode.SOFT_LIGHT,
                () -> Tiles.css(new Rectangle(10, 10), "-fx-blend-mode: soft-light;").getBlendMode()));
        checks.add(Checks.expect("inline -fx-blend-mode: color-dodge", BlendMode.COLOR_DODGE,
                () -> Tiles.css(new Rectangle(10, 10), "-fx-blend-mode: color-dodge;").getBlendMode()));
        // the top input overflows the bottom one on the right : SRC_OVER keeps it, SRC_ATOP drops it
        checks.add(Checks.expect("top input only: SRC_OVER / SRC_ATOP", "#cc9933 / #ffffff",
                () -> blendPixel(BlendMode.SRC_OVER, 25) + " / " + blendPixel(BlendMode.SRC_ATOP, 25)));
        return checks;
    }

    /*
     * Every BlendMode runs its own Decora peer (PPSBlend_<MODE>Peer, loaded by reflection) and GLSL program
     * (Blend_<MODE>.frag, loaded as a resource) : each of the 17 modes is rendered here through Node.snapshot.
     */
    private static List<Check> pixelChecks() {
        List<Check> checks = new ArrayList<>();
        BlendMode[][] groups = {
                { BlendMode.SRC_OVER, BlendMode.SRC_ATOP },
                { BlendMode.ADD, BlendMode.MULTIPLY },
                { BlendMode.SCREEN, BlendMode.OVERLAY },
                { BlendMode.DARKEN, BlendMode.LIGHTEN },
                { BlendMode.COLOR_DODGE, BlendMode.COLOR_BURN },
                { BlendMode.HARD_LIGHT, BlendMode.SOFT_LIGHT },
                { BlendMode.DIFFERENCE, BlendMode.EXCLUSION },
                { BlendMode.RED, BlendMode.GREEN, BlendMode.BLUE } };
        for (BlendMode[] group : groups) {
            StringBuilder name = new StringBuilder();
            for (BlendMode mode : group) {
                name.append(name.isEmpty() ? "" : " / ").append(mode);
            }
            checks.add(Checks.run(name.toString(), () -> {
                StringBuilder value = new StringBuilder();
                for (BlendMode mode : group) {
                    value.append(value.isEmpty() ? "" : " / ").append(blendPixel(mode, 15));
                }
                return value.toString();
            }));
        }
        return checks;
    }

    /**
     * Blends a 20x20 top input over a 20x20 bottom input shifted 10 px left, returns the pixel at {@code (x, 10)}.
     */
    private static String blendPixel(BlendMode mode, int x) {
        Rectangle node = new Rectangle(30, 20, Color.TRANSPARENT);
        node.setEffect(new Blend(mode, new ColorInput(0, 0, 20, 20, BOTTOM), new ColorInput(10, 0, 20, 20, TOP)));
        // #rrggbb : the snapshot is filled with white, every pixel is opaque
        String pixel = Tiles.pixel(node, x, 10);
        return pixel.substring(0, 7);
    }
}
