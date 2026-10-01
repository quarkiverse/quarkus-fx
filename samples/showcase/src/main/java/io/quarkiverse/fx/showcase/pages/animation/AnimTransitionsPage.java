package io.quarkiverse.fx.showcase.pages.animation;

import static io.quarkiverse.fx.showcase.pages.animation.AnimSupport.f1;
import static io.quarkiverse.fx.showcase.pages.animation.AnimSupport.f3;
import static io.quarkiverse.fx.showcase.pages.animation.AnimSupport.freeze;
import static io.quarkiverse.fx.showcase.pages.animation.AnimSupport.ghost;
import static io.quarkiverse.fx.showcase.pages.animation.AnimSupport.ms;
import static io.quarkiverse.fx.showcase.pages.animation.AnimSupport.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.StrokeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * One node per transition type, every transition frozen at a fixed time.
 */
@Singleton
public class AnimTransitionsPage implements FeaturePage {

    private static final double W = 196;
    private static final double H = 140;

    private static final Color BLUE = Color.web("#4c78a8");
    private static final Color ORANGE = Color.web("#f58518");
    private static final Color GREEN = Color.web("#54a24b");
    private static final Color RED = Color.web("#e45756");
    private static final Color PURPLE = Color.web("#b279a2");

    @Override
    public String id() {
        return "anim-transitions";
    }

    @Override
    public String title() {
        return "Transitions";
    }

    @Override
    public String category() {
        return Categories.ANIMATION;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        List<Animation> animations = new ArrayList<>();
        List<Check> values = new ArrayList<>();
        List<Check> states = new ArrayList<>();
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);

        // FadeTransition
        Rectangle fadeRect = rounded(58, 24, 80, 80, BLUE);
        FadeTransition fade = new FadeTransition(Duration.millis(2000), fadeRect);
        fade.setFromValue(1.0);
        fade.setToValue(0.15);
        fade.setInterpolator(Interpolator.LINEAR);
        add(grid, 0, animations, fade, Duration.millis(1000),
                "FadeTransition 1.0 → 0.15 · LINEAR · at 1000 of 2000 ms",
                ghost(58, 24, 80, 80), fadeRect);
        values.add(Checks.expect("fade opacity @1000", "0.575", () -> f3(fadeRect.getOpacity())));

        // TranslateTransition
        Circle moving = new Circle(30, 64, 18, ORANGE);
        TranslateTransition translate = new TranslateTransition(Duration.millis(2000), moving);
        translate.setFromX(0);
        translate.setToX(136);
        Line track = new Line(30, 64, 166, 64);
        track.setStroke(Color.web("#d0d4da"));
        track.getStrokeDashArray().setAll(4.0, 3.0);
        add(grid, 1, animations, translate, Duration.millis(1500),
                "TranslateTransition x 0 → 136 · EASE_BOTH (default) · at 1500 ms",
                track, ghostCircle(30, 64, 18), ghostCircle(166, 64, 18), moving);
        values.add(Checks.expect("translate x @1500 (EASE_BOTH)", "110.500", () -> f3(moving.getTranslateX())));

        // RotateTransition
        Group rotating = new Group(rounded(63, 29, 70, 70, GREEN), new Circle(73, 39, 5, Color.WHITE));
        RotateTransition rotate = new RotateTransition(Duration.millis(2000), rotating);
        rotate.setFromAngle(0);
        rotate.setToAngle(270);
        rotate.setInterpolator(Interpolator.LINEAR);
        add(grid, 2, animations, rotate, Duration.millis(1000),
                "RotateTransition 0 → 270° · LINEAR · at 1000 ms",
                ghost(63, 29, 70, 70), rotating);
        values.add(Checks.expect("rotate angle @1000", "135.0", () -> f1(rotating.getRotate())));

        // ScaleTransition
        Polygon star = star(98, 64, 34, 14, PURPLE);
        ScaleTransition scale = new ScaleTransition(Duration.millis(2000), star);
        scale.setFromX(0.4);
        scale.setFromY(0.4);
        scale.setToX(1.6);
        scale.setToY(1.6);
        scale.setInterpolator(Interpolator.EASE_IN);
        add(grid, 3, animations, scale, Duration.millis(1000),
                "ScaleTransition 0.4 → 1.6 · EASE_IN · at 1000 ms",
                ghostCircle(98, 64, 34 * 0.4), ghostCircle(98, 64, 34 * 1.6), star);
        values.add(Checks.run("scale x / y @1000 (EASE_IN)", () -> f3(star.getScaleX()) + " / " + f3(star.getScaleY())));

        // FillTransition
        Circle filled = new Circle(98, 64, 42);
        FillTransition fill = new FillTransition(Duration.millis(2000), filled, RED, BLUE);
        fill.setInterpolator(Interpolator.LINEAR);
        add(grid, 4, animations, fill, Duration.millis(500),
                "FillTransition red → blue · LINEAR · at 500 ms",
                swatch(8, 8, RED), swatch(170, 8, BLUE), filled);
        values.add(Checks.run("fill color @500", () -> String.valueOf(filled.getFill())));

        // StrokeTransition
        Rectangle stroked = new Rectangle(48, 29, 100, 70);
        stroked.setFill(Color.web("#f7f7f7"));
        stroked.setStrokeWidth(10);
        stroked.setArcWidth(18);
        stroked.setArcHeight(18);
        StrokeTransition stroke = new StrokeTransition(Duration.millis(2000), stroked, ORANGE, GREEN);
        stroke.setInterpolator(Interpolator.LINEAR);
        add(grid, 5, animations, stroke, Duration.millis(1000),
                "StrokeTransition orange → green · LINEAR · at 1000 ms",
                swatch(8, 8, ORANGE), swatch(170, 8, GREEN), stroked);
        values.add(Checks.run("stroke color @1000", () -> String.valueOf(stroked.getStroke())));

        // PathTransition, ORTHOGONAL_TO_TANGENT and NONE
        Path path = new Path(new MoveTo(18, 104), new CubicCurveTo(60, -30, 136, 170, 178, 22));
        path.setStroke(Color.web("#c3c8d0"));
        path.setStrokeWidth(2);
        path.getStrokeDashArray().setAll(5.0, 4.0);
        Polygon arrow = new Polygon(-12, -8, 14, 0, -12, 8);
        arrow.setFill(RED);
        PathTransition along = new PathTransition(Duration.millis(2000), path, arrow);
        along.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
        along.setInterpolator(Interpolator.LINEAR);
        Rectangle square = new Rectangle(-8, -8, 16, 16);
        square.setFill(BLUE);
        PathTransition upright = new PathTransition(Duration.millis(2000), path, square);
        upright.setInterpolator(Interpolator.LINEAR);
        add(grid, 6, animations, along, Duration.millis(700),
                "PathTransition · arrow ORTHOGONAL_TO_TANGENT at 700 ms, square NONE at 1500 ms",
                path, arrow, square);
        upright.setCycleCount(Animation.INDEFINITE);
        upright.setAutoReverse(true);
        freeze(upright, Duration.millis(1500));
        animations.add(upright);
        values.add(Checks.run("path arrow x, y, angle @700", () -> f1(arrow.getTranslateX()) + ", "
                + f1(arrow.getTranslateY()) + ", " + f1(arrow.getRotate()) + "°"));
        values.add(Checks.run("path square x, y, angle @1500 (NONE)", () -> f1(square.getTranslateX()) + ", "
                + f1(square.getTranslateY()) + ", " + f1(square.getRotate()) + "°"));

        // ParallelTransition
        Rectangle parallelRect = rounded(18, 34, 60, 60, ORANGE);
        FadeTransition pFade = new FadeTransition(Duration.millis(2000));
        pFade.setFromValue(1.0);
        pFade.setToValue(0.3);
        RotateTransition pRotate = new RotateTransition(Duration.millis(2000));
        pRotate.setByAngle(180);
        TranslateTransition pMove = new TranslateTransition(Duration.millis(1600));
        pMove.setFromX(0);
        pMove.setToX(100);
        ParallelTransition parallel = new ParallelTransition(parallelRect, pFade, pRotate, pMove);
        parallel.getChildren().forEach(child -> ((javafx.animation.Transition) child).setInterpolator(Interpolator.LINEAR));
        add(grid, 7, animations, parallel, Duration.millis(1000),
                "ParallelTransition (fade, rotate 180°, move 1600 ms) · at 1000 ms",
                ghost(18, 34, 60, 60), ghost(118, 34, 60, 60), parallelRect);
        values.add(Checks.run("parallel opacity, angle, x @1000", () -> f3(parallelRect.getOpacity()) + ", "
                + f1(parallelRect.getRotate()) + "°, " + f1(parallelRect.getTranslateX())));

        // SequentialTransition
        Circle sequenced = new Circle(34, 64, 16, RED);
        TranslateTransition sMove = new TranslateTransition(Duration.millis(1000));
        sMove.setFromX(0);
        sMove.setToX(116);
        ScaleTransition sScale = new ScaleTransition(Duration.millis(1000));
        sScale.setFromX(1);
        sScale.setFromY(1);
        sScale.setToX(2);
        sScale.setToY(2);
        FillTransition sFill = new FillTransition(Duration.millis(1000), RED, GREEN);
        SequentialTransition sequential = new SequentialTransition(sequenced, sMove, sScale, sFill);
        sequential.getChildren().forEach(child -> ((javafx.animation.Transition) child).setInterpolator(Interpolator.LINEAR));
        add(grid, 8, animations, sequential, Duration.millis(1500),
                "SequentialTransition move → scale → fill (3 x 1000 ms) · at 1500 ms",
                ghostCircle(34, 64, 16), ghostCircle(150, 64, 32), sequenced);
        values.add(Checks.run("sequential x, scale, fill @1500", () -> f1(sequenced.getTranslateX()) + ", "
                + f3(sequenced.getScaleX()) + ", " + sequenced.getFill()));

        // PauseTransition
        PauseTransition pause = new PauseTransition(Duration.millis(2000));
        ProgressBar progress = new ProgressBar();
        progress.progressProperty().bind(Bindings.createDoubleBinding(
                () -> pause.getCurrentTime().toMillis() / pause.getCycleDuration().toMillis(), pause.currentTimeProperty()));
        progress.setPrefWidth(160);
        progress.relocate(18, 40);
        Label time = new Label();
        time.textProperty().bind(Bindings.createStringBinding(
                () -> "currentTime " + Math.round(pause.getCurrentTime().toMillis()) + " ms · " + pause.getStatus(),
                pause.currentTimeProperty(), pause.statusProperty()));
        time.relocate(18, 72);
        time.setStyle("-fx-font-size: 11px;");
        add(grid, 9, animations, pause, Duration.millis(1200),
                "PauseTransition 2000 ms · progress bound to currentTime · at 1200 ms",
                progress, time);
        values.add(Checks.expect("pause progress @1200", "0.600", () -> f3(progress.getProgress())));

        // States (the same in both modes : computed while every animation is paused at its fixed time)
        states.add(Checks.expect("statuses", "PAUSED x11", () -> {
            List<String> distinct = animations.stream().map(a -> a.getStatus().name()).distinct().toList();
            return String.join(",", distinct) + " x" + animations.size();
        }));
        states.add(Checks.run("current rates while paused",
                () -> animations.stream().map(a -> String.valueOf(a.getCurrentRate())).distinct()
                        .collect(Collectors.joining(","))));
        states.add(Checks.expect("current times (ms)", "1000,1500,1000,1000,500,1000,700,1500,1000,1500,1200",
                () -> animations.stream().map(a -> String.valueOf(Math.round(a.getCurrentTime().toMillis())))
                        .collect(Collectors.joining(","))));
        states.add(Checks.expect("sequential cycle / total duration", "3000.0 ms / Infinity",
                () -> ms(sequential.getCycleDuration()) + " / " + sequential.getTotalDuration().toMillis()));
        states.add(Checks.expect("parallel cycle duration", "2000.0 ms", () -> ms(parallel.getCycleDuration())));
        states.add(Checks.run("sequential children statuses",
                () -> sequential.getChildren().stream().map(a -> a.getStatus().name())
                        .collect(Collectors.joining(","))));
        states.add(Checks.expect("default interpolator", "Interpolator.EASE_BOTH",
                () -> new TranslateTransition().getInterpolator().toString()));
        states.add(Checks.expect("default transition duration", "400.0 ms",
                () -> ms(new FadeTransition().getDuration())));
        states.add(Checks.expect("default path orientation", "NONE",
                () -> new PathTransition().getOrientation().name()));
        states.add(Checks.expect("rotate axis (default / Z)", "null / " + Rotate.Z_AXIS,
                () -> new RotateTransition().getAxis() + " / " + Rotate.Z_AXIS));
        states.add(Checks.expect("Animation.INDEFINITE / cycle count", "-1 / -1",
                () -> Animation.INDEFINITE + " / " + fade.getCycleCount()));
        states.add(Checks.run("auto reverse / target framerate",
                () -> fade.isAutoReverse() + " / " + fade.getTargetFramerate()));
        states.add(Checks.expect("2 x 1000 ms, delay 500, rate -2", "total 2000.0 ms / -2.0 / 0.0", () -> {
            FadeTransition delayed = new FadeTransition(Duration.millis(1000));
            delayed.setDelay(Duration.millis(500));
            delayed.setCycleCount(2);
            delayed.setRate(-2);
            return "total " + ms(delayed.getTotalDuration()) + " / " + delayed.getRate() + " / "
                    + delayed.getCurrentRate();
        }));

        HBox checks = new HBox(12, Checks.view("Interpolated values", values), Checks.view("Animation states", states));
        checks.getChildren().forEach(c -> ((VBox) c).setPrefWidth(508));
        VBox root = new VBox(12, grid, checks);
        AnimSupport.register(root, animations);
        return root;
    }

    @Override
    public void dispose(Node content) {
        AnimSupport.stopAll(content);
    }

    private static void add(GridPane grid, int index, List<Animation> animations, Animation animation, Duration at,
            String caption, Node... nodes) {
        animation.setCycleCount(Animation.INDEFINITE);
        animation.setAutoReverse(true);
        freeze(animation, at);
        animations.add(animation);
        Pane stage = new Pane(nodes);
        grid.add(tile(caption, stage, W, H), index % 5, index / 5);
    }

    private static Rectangle rounded(double x, double y, double w, double h, Color color) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(14);
        r.setArcHeight(14);
        r.setFill(color);
        return r;
    }

    private static Circle ghostCircle(double x, double y, double radius) {
        Circle c = new Circle(x, y, radius);
        c.setFill(null);
        c.setStroke(Color.web("#b8bec8"));
        c.getStrokeDashArray().setAll(4.0, 3.0);
        return c;
    }

    private static Rectangle swatch(double x, double y, Color color) {
        Rectangle r = new Rectangle(x, y, 18, 18);
        r.setFill(color);
        r.setStroke(Color.web("#888888"));
        r.setStrokeLineCap(StrokeLineCap.SQUARE);
        return r;
    }

    private static Polygon star(double cx, double cy, double outer, double inner, Color color) {
        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double radius = i % 2 == 0 ? outer : inner;
            double angle = Math.PI / 2 + i * Math.PI / 5;
            // rounded : the drawn geometry must not depend on the last bits of Math.sin / Math.cos
            star.getPoints().addAll(Math.rint((cx + radius * Math.cos(angle)) * 100) / 100,
                    Math.rint((cy - radius * Math.sin(angle)) * 100) / 100);
        }
        star.setFill(color);
        return star;
    }
}
