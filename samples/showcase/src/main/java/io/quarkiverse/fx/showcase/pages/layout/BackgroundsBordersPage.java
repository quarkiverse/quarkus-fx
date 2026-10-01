package io.quarkiverse.fx.showcase.pages.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderImage;
import javafx.scene.layout.BorderRepeat;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

@Singleton
public class BackgroundsBordersPage implements FeaturePage {

    private static final double W = 248;
    private static final double H = 150;

    @Override
    public String id() {
        return "layout-backgrounds-borders";
    }

    @Override
    public String title() {
        return "Backgrounds & borders";
    }

    @Override
    public String category() {
        return Categories.LAYOUT_CSS;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        int cssMark = Kit.cssErrorMark();
        VBox root = Kit.page("backgrounds-page", "backgrounds.css");

        Image photo = new Image(Fx.resourceUrl("/showcase/images/photo.jpg"));
        Image icon = new Image(Fx.resourceUrl("/showcase/images/icon.png"));
        Image icon16 = new Image(Fx.resourceUrl("/showcase/images/icon-16.png"));
        Image pattern = new Image(Fx.resourceUrl("/showcase/images/pattern.png"));

        // 1. fills with radii and insets
        Region fills = sized(new Region(), 220, 128);
        fills.setBackground(new Background(
                new BackgroundFill(Color.web("#1565c0"), new CornerRadii(18), Insets.EMPTY),
                new BackgroundFill(Color.web("#64b5f6"), new CornerRadii(12), new Insets(6)),
                new BackgroundFill(Color.WHITE, new CornerRadii(0, 30, 0, 30, false), new Insets(14)),
                new BackgroundFill(Color.web("#ef5350"), new CornerRadii(0.5, true), new Insets(34, 56, 34, 56)),
                new BackgroundFill(Color.web("#ffca28aa"), new CornerRadii(4), new Insets(56, 150, 20, 22))));
        Node fillsDemo = Kit.demo("BackgroundFill · radii (per corner, %) + insets", W, H, fills);

        // 2-4. CSS repeat modes
        Region repeat = sized(css(new Region(), "bg-repeat"), 220, 120);
        Node repeatDemo = Kit.demo("CSS url(tile.png) · repeat", W, H, repeat);
        Region space = sized(css(new Region(), "bg-space"), 220, 120);
        Node spaceDemo = Kit.demo("CSS url(tile.png) · space", W, H, space);
        Region round = sized(css(new Region(), "bg-round"), 228, 130);
        Region repeatX = sized(css(new Region(), "bg-repeat-x"), 228, 36);
        VBox roundBox = new VBox(6, round, repeatX);
        round.setPrefHeight(90);
        round.setMinHeight(90);
        round.setMaxHeight(90);
        roundBox.setAlignment(Pos.CENTER);
        roundBox.setFillWidth(false);
        Node roundDemo = Kit.demo("CSS · round · repeat-x / repeat-y, 2 layers", W, H, roundBox);

        // 5. cover and contain
        Region cover = sized(new Region(), 112, 130);
        cover.setBackground(new Background(new BackgroundImage(photo, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER, new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, false, true))));
        Region contain = sized(new Region(), 112, 130);
        contain.setBackground(new Background(
                new BackgroundFill[] { new BackgroundFill(Color.web("#263238"), CornerRadii.EMPTY, Insets.EMPTY) },
                new BackgroundImage[] { new BackgroundImage(photo, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, false)) }));
        HBox sizes = new HBox(8, cover, contain);
        sizes.setAlignment(Pos.CENTER);
        Node sizeDemo = Kit.demo("BackgroundSize (photo.jpg) · cover · contain", W, H, sizes);

        // 6. positions
        Region positions = sized(new Region(), 228, 130);
        positions.setBackground(new Background(
                new BackgroundFill[] { new BackgroundFill(Color.web("#eceff1"), CornerRadii.EMPTY, Insets.EMPTY) },
                new BackgroundImage[] {
                        image(icon16, BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT,
                                new BackgroundPosition(Side.LEFT, 0, false, Side.BOTTOM, 4, false)),
                        image(icon16, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                new BackgroundPosition(Side.LEFT, 8, false, Side.TOP, 8, false)),
                        image(icon16, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                new BackgroundPosition(Side.RIGHT, 8, false, Side.TOP, 8, false)),
                        image(icon16, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                new BackgroundPosition(Side.LEFT, 0.25, true, Side.TOP, 0.5, true)),
                        image(icon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER),
                        image(icon16, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                new BackgroundPosition(Side.RIGHT, 0.2, true, Side.BOTTOM, 0.35, true)) }));
        Node positionDemo = Kit.demo("BackgroundPosition · sides, offsets, % · repeat-x", W, H, positions);

        // 7. stroke styles
        BorderStrokeStyle customDash = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.ROUND, StrokeLineCap.ROUND, 10, 0,
                List.of(12.0, 5.0, 1.0, 5.0));
        Region solid = stroke("SOLID", BorderStrokeStyle.SOLID, "#1e88e5", 0);
        Region dashed = stroke("DASHED", BorderStrokeStyle.DASHED, "#43a047", 8);
        Region dotted = stroke("DOTTED", BorderStrokeStyle.DOTTED, "#e53935", 0);
        Region custom = stroke("12 5 1 5", customDash, "#8e24aa", 12);
        GridPane strokes = new GridPane(10, 10);
        strokes.add(solid, 0, 0);
        strokes.add(dashed, 1, 0);
        strokes.add(dotted, 0, 1);
        strokes.add(custom, 1, 1);
        strokes.setAlignment(Pos.CENTER);
        Node strokeDemo = Kit.demo("BorderStrokeStyle · solid, dashed, dotted, dash array", W, H, strokes);

        // 8. per side stroke
        Region sides = sized(new Region(), 200, 110);
        sides.setBorder(new Border(
                new BorderStroke(Color.web("#e53935"), Color.web("#43a047"), Color.web("#1e88e5"), Color.web("#fb8c00"),
                        BorderStrokeStyle.SOLID, BorderStrokeStyle.DASHED, BorderStrokeStyle.SOLID, BorderStrokeStyle.DOTTED,
                        new CornerRadii(16), new BorderWidths(2, 4, 6, 8), Insets.EMPTY),
                new BorderStroke(Color.web("#90a4ae"), BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1),
                        new Insets(20))));
        sides.setBackground(Background.fill(Color.web("#fffde7")));
        Node sidesDemo = Kit.demo("BorderStroke · per side colors/styles/widths, 2 strokes", W, H, sides);

        // 9. border image from code (9-slice)
        Region borderImage = sized(new Region(), 104, 120);
        borderImage.setBorder(new Border(new BorderImage(pattern, new BorderWidths(24), Insets.EMPTY, new BorderWidths(64), true,
                BorderRepeat.STRETCH, BorderRepeat.STRETCH)));
        Region borderImageRepeat = sized(new Region(), 104, 120);
        borderImageRepeat.setBorder(new Border(new BorderImage(pattern, new BorderWidths(20), new Insets(0),
                new BorderWidths(64), false, BorderRepeat.REPEAT, BorderRepeat.REPEAT)));
        HBox borderImages = new HBox(12, borderImage, borderImageRepeat);
        borderImages.setAlignment(Pos.CENTER);
        Node borderImageDemo = Kit.demo("BorderImage 9-slice · stretch + fill · repeat", W, H, borderImages);

        // 10. border image from CSS
        Region cssBorderImage = sized(css(new Region(), "border-image-css"), 200, 120);
        Node cssBorderImageDemo = Kit.demo("CSS -fx-border-image · slice 64 fill · round space", W, H, cssBorderImage);

        // 11. multiple backgrounds and borders
        Region multi = sized(css(new Region(), "multi"), 220, 124);
        Node multiDemo = Kit.demo("CSS · 3 fills + image, 2 borders (insets, radii, styles)", W, H, multi);

        // 12. effects
        Region gaussian = css(new Region(), "fx-shadow-box", "shadow-gaussian");
        Region threePass = css(new Region(), "fx-shadow-box", "shadow-three-pass");
        Region onePass = css(new Region(), "fx-shadow-box", "shadow-one-pass");
        Region inner = css(new Region(), "fx-shadow-box", "shadow-inner");
        Label text = new Label("Shadowed text");
        text.getStyleClass().add("shadow-text");
        GridPane effects = new GridPane(16, 16);
        effects.add(gaussian, 0, 0);
        effects.add(threePass, 1, 0);
        effects.add(onePass, 2, 0);
        effects.add(inner, 0, 1);
        effects.add(text, 1, 1, 2, 1);
        effects.setAlignment(Pos.CENTER);
        Node effectDemo = Kit.demo("CSS -fx-effect · dropshadow (3 blurs), innershadow", W, H, effects);

        GridPane demos = Kit.grid(4, W, 12, 8);
        Kit.addAll(demos, 4, fillsDemo, repeatDemo, spaceDemo, roundDemo, sizeDemo, positionDemo, strokeDemo, sidesDemo,
                borderImageDemo, cssBorderImageDemo, multiDemo, effectDemo);

        HBox checks = Kit.checksRow(1028);
        root.getChildren().addAll(demos, checks);

        Kit.whenShown(root, 3, () -> {
            List<Check> left = new ArrayList<>();
            left.add(Checks.expect("fills (code) count · last radii", "5 · 4.0", () -> fills.getBackground().getFills().size()
                    + " · " + fills.getBackground().getFills().get(4).getRadii().getTopLeftHorizontalRadius()));
            left.add(Checks.run("CSS repeat modes", () -> repeatModes(repeat) + " · " + repeatModes(space) + " · "
                    + repeatModes(round) + " · " + repeatModes(repeatX)));
            left.add(Checks.expect("CSS url() image (tile.png)", "32x32", () -> size(space.getBackground().getImages().get(0)
                    .getImage())));
            left.add(Checks.expect("images decoded (photo, pattern)", "480x320 · 256x256", () -> {
                if (photo.isError() || pattern.isError()) {
                    throw new IllegalStateException(String.valueOf(photo.isError() ? photo.getException()
                            : pattern.getException()));
                }
                return size(photo) + " · " + size(pattern);
            }));
            left.add(Checks.run("custom dash array", () -> custom.getBorder().getStrokes().get(0).getTopStyle().getDashArray()));
            List<Check> right = new ArrayList<>();
            right.add(Checks.expect("CSS border image", "slices 64.0 fill=true ROUND/SPACE widths 24.0", () -> {
                BorderImage bi = cssBorderImage.getBorder().getImages().get(0);
                return "slices " + bi.getSlices().getTop() + " fill=" + bi.isFilled() + " " + bi.getRepeatX() + "/"
                        + bi.getRepeatY() + " widths " + bi.getWidths().getTop();
            }));
            right.add(Checks.expect("CSS multi layers (fills/images/strokes)", "3/1/2", () -> multi.getBackground().getFills()
                    .size() + "/" + multi.getBackground().getImages().size() + "/" + multi.getBorder().getStrokes().size()));
            right.add(Checks.run("CSS multi fills", () -> multi.getBackground().getFills().stream()
                    .map(f -> Kit.paint(f.getFill())).collect(Collectors.joining(", "))));
            right.add(Checks.run("CSS effects", () -> List.of(gaussian, threePass, onePass, inner, text).stream()
                    .map(n -> effect(n.getEffect())).collect(Collectors.joining(", "))));
            right.add(Kit.cssErrorsCheck("CSS errors on this page", cssMark));
            Kit.fillChecks(checks, "Resolved backgrounds", left, "Resolved CSS", right);
        });
        return root;
    }

    private static Region sized(Region region, double width, double height) {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
        return region;
    }

    private static Region css(Region region, String... styleClasses) {
        region.getStyleClass().addAll(styleClasses);
        return region;
    }

    private static BackgroundImage image(Image image, BackgroundRepeat x, BackgroundRepeat y, BackgroundPosition position) {
        return new BackgroundImage(image, x, y, position, BackgroundSize.DEFAULT);
    }

    private static Region stroke(String text, BorderStrokeStyle style, String color, double radius) {
        Label label = new Label(text);
        label.setAlignment(Pos.CENTER);
        label.setMinSize(96, 50);
        label.setPrefSize(96, 50);
        label.setMaxSize(96, 50);
        label.setBorder(new Border(new BorderStroke(Color.web(color), style, new CornerRadii(radius), new BorderWidths(3))));
        return label;
    }

    private static String repeatModes(Region region) {
        return region.getBackground().getImages().stream().map(i -> i.getRepeatX() + "/" + i.getRepeatY())
                .collect(Collectors.joining("+"));
    }

    private static String size(Image image) {
        return (int) image.getWidth() + "x" + (int) image.getHeight();
    }

    private static String effect(Effect effect) {
        if (effect instanceof DropShadow d) {
            return "drop " + d.getBlurType() + " " + (int) d.getRadius();
        }
        if (effect instanceof InnerShadow s) {
            return "inner " + s.getBlurType() + " " + (int) s.getRadius();
        }
        return String.valueOf(effect);
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Kit.ready(content);
    }
}
