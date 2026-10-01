package io.quarkiverse.fx.showcase.pages.graphics;

import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.BLUE;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.BLUE_L;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.GUIDE;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.W;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.row;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.section;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

@Singleton
public class TransformsClipPage implements FeaturePage {

    private static final double H = 80;

    @Override
    public String id() {
        return "graphics-transforms-clip";
    }

    @Override
    public String title() {
        return "Transforms, Clips & Caching";
    }

    @Override
    public String category() {
        return Categories.GRAPHICS;
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public Node build() {
        Image photo = Tiles.image("photo.jpg", 96, 64);
        Image texture = Tiles.image("texture.png");
        Image icon = Tiles.image("icon.png");
        return Tiles.page(
                section("Transforms (dashed: untransformed geometry)"),
                row(transformed("Rotate 30° (pivot center)", new Rotate(30, 25, 16)),
                        transformed("Scale 1.5 x 0.6", new Scale(1.5, 0.6)),
                        transformed("Shear 0.5, 0", new Shear(0.5, 0)),
                        transformed("Translate 22, 12", new Translate(22, 12)),
                        transformed("Affine [.8 .4 6; -.3 .9 14]", new Affine(0.8, 0.4, 6, -0.3, 0.9, 14)),
                        transformOrder(), nodeProperties(), fan()),
                section("Rotated and transformed text"),
                row(rotatedText("Rotate -30°", -30), rotatedText("Rotate 90°", 90), textFan(), mirroredText(), shearedText(),
                        rotatedText("Rotate 180°", 180), cssTransform(), scaledText()),
                section("3D-looking rotations around the X / Y axes, without a 3D scene"),
                row(rotated3d("Rotate Y 30°", texture, 30, Rotate.Y_AXIS), rotated3d("Rotate Y 60°", texture, 60, Rotate.Y_AXIS),
                        rotated3d("Rotate X 45°", texture, 45, Rotate.X_AXIS), rotated3d("Rotate X 70°", texture, 70, Rotate.X_AXIS),
                        rotated3d("Rotate 50° axis (1,1,0)", texture, 50, new Point3D(1, 1, 0)),
                        rotated3d("Rotate Y 150° (back face)", texture, 150, Rotate.Y_AXIS),
                        cardFlip(icon), nested3d(texture)),
                section("Clips, caching, opacity"),
                row(circleClip(photo), textClip(photo), svgClip(texture), roundedClip(photo), groupClip(),
                        cached("setCache QUALITY", CacheHint.QUALITY, 1), cached("setCache SPEED, scale 1.3", CacheHint.SPEED, 1.3),
                        opacities()),
                Tiles.checks(Checks.view("Transform math", transformChecks()),
                        Checks.view("Nodes: bounds, CSS, clips, cache", nodeChecks())));
    }

    // ---- Transforms ----

    private static Group subject() {
        Rectangle rect = Tiles.paint(new Rectangle(0, 0, 50, 32), BLUE_L, BLUE, 1.5);
        Text text = new Text(8, 24, "FX");
        text.setFont(Font.font("System", FontWeight.BOLD, 20));
        text.setFill(BLUE);
        return new Group(rect, text);
    }

    private static Rectangle ghost() {
        Rectangle rect = new Rectangle(0, 0, 50, 32);
        rect.setFill(null);
        rect.setStroke(GUIDE);
        rect.getStrokeDashArray().addAll(3.0, 3.0);
        return rect;
    }

    private static Node transformed(String caption, Transform transform) {
        Group subject = subject();
        subject.getTransforms().add(transform);
        return tile(caption, W, H, ghost(), subject);
    }

    private static Node transformOrder() {
        Group a = subject();
        a.getTransforms().addAll(new Translate(4, 4), new Rotate(20), new Scale(0.7, 0.7));
        Group b = subject();
        b.getTransforms().addAll(new Scale(0.7, 0.7), new Rotate(20), new Translate(66, 4));
        return tile("T·R·S vs S·R·T", W, H, a, b);
    }

    private static Node nodeProperties() {
        Group subject = subject();
        subject.setRotate(-25);
        subject.setScaleX(1.2);
        subject.setScaleY(0.8);
        subject.setTranslateX(6);
        return tile("setRotate / setScaleX,Y / setTranslateX", W, H, ghost(), subject);
    }

    private static Node fan() {
        Group group = new Group();
        String[] colors = { "#e3f2fd", "#90caf9", "#42a5f5", "#1565c0" };
        for (int i = 0; i < 4; i++) {
            Rectangle rect = Tiles.paint(new Rectangle(0, 0, 60, 14), Color.web(colors[i]), BLUE, 1);
            rect.getTransforms().add(new Rotate(i * 25, 0, 0));
            group.getChildren().add(rect);
        }
        return tile("Rotate 0/25/50/75° pivot (0,0)", W, H, group);
    }

    // ---- Text ----

    private static Text text(String value, double size) {
        Text text = new Text(value);
        text.setFont(Font.font("System", FontWeight.BOLD, size));
        text.setFill(Color.web("#37474f"));
        return text;
    }

    private static Node rotatedText(String caption, double angle) {
        Text text = text("Rotated", 16);
        text.setRotate(angle);
        return tile(caption, W, H, text);
    }

    private static Node textFan() {
        Group group = new Group();
        for (int i = 0; i < 8; i++) {
            Text text = text("FX", 11);
            text.setFill(Color.hsb(i * 45, 0.8, 0.75));
            text.getTransforms().addAll(new Rotate(i * 45, 0, 0), new Translate(10, 4));
            group.getChildren().add(text);
        }
        return tile("8 texts, Rotate + Translate", W, H, group);
    }

    private static Node mirroredText() {
        Text normal = text("Mirror", 18);
        Text mirrored = text("Mirror", 18);
        mirrored.setFill(Color.web("#90a4ae"));
        mirrored.getTransforms().addAll(new Translate(0, 28), new Scale(-1, 1, 30, 0));
        return tile("Scale(-1, 1)", W, H, normal, mirrored);
    }

    private static Node shearedText() {
        Text text = text("Sheared", 20);
        text.getTransforms().add(new Shear(-0.5, 0));
        return tile("Shear(-0.5, 0)", W, H, text);
    }

    private static Node cssTransform() {
        Group subject = subject();
        subject.getStyleClass().add("css-transform");
        return tile("CSS rotate / scale / translate", W, H, ghost(), subject);
    }

    private static Node scaledText() {
        Text small = text("Aa", 10);
        small.getTransforms().add(new Scale(3, 3));
        Text large = text("Aa", 30);
        large.setX(54);
        return tile("10px scaled x3 | 30px font", W, H, small, large);
    }

    // ---- 3D-looking ----

    private static ImageView textureView(Image texture, double size) {
        ImageView view = new ImageView(texture);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setSmooth(true);
        return view;
    }

    private static Node rotated3d(String caption, Image texture, double angle, Point3D axis) {
        ImageView view = textureView(texture, 64);
        view.getTransforms().add(new Rotate(angle, 32, 32, 0, axis));
        return Tiles.unclipped(tile(caption, W, H, view));
    }

    private static Node cardFlip(Image icon) {
        Group group = new Group();
        double[] angles = { 0, 30, 60, 80 };
        for (int i = 0; i < angles.length; i++) {
            ImageView view = new ImageView(icon);
            view.setFitWidth(24);
            view.setFitHeight(24);
            view.setX(i * 27);
            view.getTransforms().add(new Rotate(angles[i], i * 27 + 12, 12, 0, Rotate.Y_AXIS));
            group.getChildren().add(view);
        }
        // with a clip on the tile, JavaFX cuts the right part of the 60° image view
        return Tiles.unclipped(tile("flip Y 0 / 30 / 60 / 80°", W, H, group));
    }

    private static Node nested3d(Image texture) {
        ImageView view = textureView(texture, 60);
        view.getTransforms().add(new Rotate(40, 30, 30, 0, Rotate.Y_AXIS));
        Group parent = new Group(view);
        parent.getTransforms().add(new Rotate(40, 30, 30, 0, Rotate.X_AXIS));
        return Tiles.unclipped(tile("parent X 40° ⊃ child Y 40°", W, H, parent));
    }

    // ---- Clips, cache, opacity ----

    private static Node circleClip(Image photo) {
        ImageView view = new ImageView(photo);
        view.setClip(new Circle(48, 32, 30));
        return tile("clip: Circle", W, H, view);
    }

    private static Text clipText() {
        Text text = new Text(0, 50, "CLIP");
        text.setFont(Font.font("System", FontWeight.BLACK, 38));
        return text;
    }

    private static Node textClip(Image photo) {
        ImageView view = new ImageView(photo);
        view.setClip(clipText());
        return tile("clip: Text", W, H, view);
    }

    private static Node svgClip(Image texture) {
        ImageView view = new ImageView(texture);
        view.setFitWidth(100);
        view.setFitHeight(100);
        SVGPath heart = new SVGPath();
        heart.setContent(ShapesPage.path(ShapesPage.paths(), "heart"));
        view.setClip(heart);
        Group group = new Group(view);
        group.setScaleX(0.75);
        group.setScaleY(0.75);
        return tile("clip: SVGPath (resource)", W, H, group);
    }

    private static Node roundedClip(Image photo) {
        ImageView view = new ImageView(photo);
        Rectangle clip = new Rectangle(96, 64);
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        view.setClip(clip);
        return tile("clip: rounded Rectangle", W, H, view);
    }

    private static Node groupClip() {
        Rectangle rect = new Rectangle(96, 64, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7b1fa2")), new Stop(1, Color.web("#ff6f00"))));
        rect.setClip(new Group(new Circle(22, 22, 20), new Circle(58, 30, 26), new Circle(28, 52, 12)));
        return tile("clip: Group of 3 circles", W, H, rect);
    }

    private static Node cached(String caption, CacheHint hint, double scale) {
        Text text = text("Cached", 18);
        text.setFill(Color.web("#00897b"));
        text.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.6), 6, 0, 2, 2));
        Group group = new Group(text);
        group.setRotate(-15);
        group.setScaleX(scale);
        group.setScaleY(scale);
        group.setCache(true);
        group.setCacheHint(hint);
        return tile(caption, W, H, group);
    }

    private static Node opacities() {
        Group group = new Group(Tiles.rect(0, 0, 100, 60, Tiles.checker(5)));
        double[] values = { 1, 0.75, 0.5, 0.25 };
        for (int i = 0; i < values.length; i++) {
            Rectangle rect = Tiles.rect(6 + i * 23, 10, 20, 40, Color.web("#d81b60"));
            rect.setOpacity(values[i]);
            group.getChildren().add(rect);
        }
        return tile("opacity 1 / .75 / .5 / .25", W, H, group);
    }

    // ---- Checks ----

    private static String point(Point2D p) {
        return String.format(Locale.ROOT, "(%.2f, %.2f)", p.getX(), p.getY());
    }

    private static String point(Point3D p) {
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", p.getX(), p.getY(), p.getZ());
    }

    private static List<Check> transformChecks() {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("Rotate(30).transform(10, 0)", "(8.66, 5.00)",
                () -> point(new Rotate(30).transform(10, 0))));
        checks.add(Checks.expect("Translate(5,5)·Rotate(90) of (10, 0)", "(5.00, 15.00)",
                () -> point(new Translate(5, 5).createConcatenation(new Rotate(90)).transform(10, 0))));
        checks.add(Checks.run("Affine inverse", () -> {
            Transform inverse = new Affine(0.8, 0.4, 6, -0.3, 0.9, 14).createInverse();
            return String.format(Locale.ROOT, "[%.3f %.3f %.3f; %.3f %.3f %.3f]", inverse.getMxx(), inverse.getMxy(),
                    inverse.getTx(), inverse.getMyx(), inverse.getMyy(), inverse.getTy());
        }));
        checks.add(Checks.expect("Scale(0, 1).createInverse()", "NonInvertibleTransformException", () -> {
            try {
                new Scale(0, 1).createInverse();
                return "invertible";
            } catch (NonInvertibleTransformException e) {
                return e.getClass().getSimpleName();
            }
        }));
        checks.add(Checks.expect("Shear(0.5, 0) determinant", "1.00", () -> Tiles.num(new Shear(0.5, 0).determinant())));
        checks.add(Checks.expect("Rotate 90° axis (1,1,0) of (1, 0, 0)", "(0.50, 0.50, -0.71)",
                () -> point(new Rotate(90, new Point3D(1, 1, 0)).transform(new Point3D(1, 0, 0)))));
        return checks;
    }

    private static List<Check> nodeChecks() {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("40x20 setRotate(90) boundsInParent", "[10.0, -10.0  20.0 x 40.0]", () -> {
            Rectangle rect = new Rectangle(40, 20);
            rect.setRotate(90);
            return Tiles.bounds(rect.getBoundsInParent());
        }));
        checks.add(Checks.expect("100x100 Rotate Y 60° width in parent", "50.00", () -> {
            Rectangle rect = new Rectangle(100, 100);
            rect.getTransforms().add(new Rotate(60, 50, 50, 0, Rotate.Y_AXIS));
            return Tiles.num(rect.getBoundsInParent().getWidth());
        }));
        checks.add(Checks.expect("CSS -fx-rotate / -fx-scale / -fx-translate", "30.00 1.50 0.50 12.00 -4.00", () -> {
            Group group = Tiles.css(new Group(new Rectangle(10, 10)), null, "css-transform");
            return Tiles.num(group.getRotate()) + " " + Tiles.num(group.getScaleX()) + " " + Tiles.num(group.getScaleY())
                    + " " + Tiles.num(group.getTranslateX()) + " " + Tiles.num(group.getTranslateY());
        }));
        checks.add(Checks.expect("ImageView 96x64 + Circle clip bounds", "[18.0, 2.0  60.0 x 60.0]", () -> {
            ImageView view = new ImageView(Tiles.image("photo.jpg", 96, 64));
            view.setClip(new Circle(48, 32, 30));
            return Tiles.bounds(view.getBoundsInParent());
        }));
        checks.add(Checks.run("Text clip bounds", () -> {
            Rectangle rect = new Rectangle(200, 100);
            rect.setClip(clipText());
            return Tiles.bounds(rect.getBoundsInLocal());
        }));
        checks.add(Checks.expect("default cache / cacheHint", "false DEFAULT", () -> {
            Rectangle rect = new Rectangle();
            return rect.isCache() + " " + rect.getCacheHint();
        }));
        return checks;
    }
}
