package io.quarkiverse.fx.showcase.pages.animation;

import static io.quarkiverse.fx.showcase.pages.animation.AnimSupport.f3;
import static io.quarkiverse.fx.showcase.pages.animation.AnimSupport.freeze;
import static io.quarkiverse.fx.showcase.pages.animation.AnimSupport.ms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.animation.Animation;
import javafx.animation.Interpolatable;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Timeline with key frames on custom properties, and interpolator curves plotted on a canvas.
 */
@Singleton
public class AnimTimelinePage implements FeaturePage {

    private static final double PLOT_W = 120;
    private static final double PLOT_H = 96;
    private static final Color CURVE = Color.web("#4c78a8");

    /**
     * A custom {@link Interpolatable} value animated by the timeline.
     */
    public record Vec2(double x, double y) implements Interpolatable<Vec2> {
        @Override
        public Vec2 interpolate(Vec2 end, double t) {
            return new Vec2(x + (end.x - x) * t, y + (end.y - y) * t);
        }
    }

    /**
     * A custom interpolator : "bounce out", only made of polynomials.
     */
    static final Interpolator BOUNCE = new Interpolator() {
        @Override
        protected double curve(double t) {
            double n = 7.5625;
            double d = 2.75;
            if (t < 1 / d) {
                return n * t * t;
            } else if (t < 2 / d) {
                double u = t - 1.5 / d;
                return n * u * u + 0.75;
            } else if (t < 2.5 / d) {
                double u = t - 2.25 / d;
                return n * u * u + 0.9375;
            }
            double u = t - 2.625 / d;
            return n * u * u + 0.984375;
        }

        @Override
        public String toString() {
            return "BOUNCE";
        }
    };

    @Override
    public String id() {
        return "anim-timeline";
    }

    @Override
    public String title() {
        return "Timeline & interpolators";
    }

    @Override
    public String category() {
        return Categories.ANIMATION;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        Map<String, DoubleUnaryOperator> curves = curves();

        // Interpolator plots
        GridPane plots = new GridPane();
        plots.setHgap(9.7);
        plots.setVgap(6);
        int index = 0;
        for (Map.Entry<String, DoubleUnaryOperator> entry : curves.entrySet()) {
            Label caption = new Label(entry.getKey());
            caption.setStyle("-fx-font-size: 10px; -fx-text-fill: #444444;");
            caption.setMaxWidth(PLOT_W);
            plots.add(new VBox(1, plot(entry.getValue()), caption), index % 8, index / 8);
            index++;
        }

        List<Check> curveChecks = new ArrayList<>();
        for (Map.Entry<String, DoubleUnaryOperator> entry : curves.entrySet()) {
            DoubleUnaryOperator curve = entry.getValue();
            curveChecks.add(Checks.run(entry.getKey() + " @¼ ½ ¾",
                    () -> f3(curve.applyAsDouble(0.25)) + " · " + f3(curve.applyAsDouble(0.5)) + " · "
                            + f3(curve.applyAsDouble(0.75))));
        }

        // Timeline on custom properties
        DoubleProperty progress = new SimpleDoubleProperty(0);
        IntegerProperty counter = new SimpleIntegerProperty(0);
        ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.web("#e45756"));
        ObjectProperty<Vec2> point = new SimpleObjectProperty<>(new Vec2(0, 0));
        StringProperty text = new SimpleStringProperty("start");
        BooleanProperty flag = new SimpleBooleanProperty(false);
        int[] visited = { 0 };

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progress, 0), new KeyValue(counter, 0), new KeyValue(color, Color.web("#e45756")),
                        new KeyValue(point, new Vec2(0, 0)), new KeyValue(text, "start"), new KeyValue(flag, false)),
                new KeyFrame(Duration.millis(1000), "one", e -> visited[0]++,
                        new KeyValue(progress, 0.25, Interpolator.EASE_IN),
                        new KeyValue(point, new Vec2(0.8, 0.1)),
                        new KeyValue(text, "one", Interpolator.DISCRETE)),
                new KeyFrame(Duration.millis(3000),
                        new KeyValue(progress, 1.0, Interpolator.EASE_OUT),
                        new KeyValue(counter, 100),
                        new KeyValue(color, Color.web("#4c78a8"), Interpolator.EASE_BOTH),
                        new KeyValue(point, new Vec2(1, 1)),
                        new KeyValue(text, "end", Interpolator.DISCRETE),
                        new KeyValue(flag, true)));
        timeline.getCuePoints().put("mark", Duration.millis(1500));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.setAutoReverse(true);
        timeline.play();
        timeline.jumpTo("mark");
        timeline.pause();

        DoubleProperty wave = new SimpleDoubleProperty(0);
        Timeline reversing = new Timeline(new KeyFrame(Duration.millis(1000), new KeyValue(wave, 1, Interpolator.LINEAR)));
        reversing.setCycleCount(3);
        reversing.setAutoReverse(true);
        freeze(reversing, Duration.millis(1250));

        Node demo = timelineDemo(timeline, progress, counter, color, point, text, flag, reversing, wave);

        List<Check> timelineChecks = new ArrayList<>();
        timelineChecks.add(Checks.expect("jumpTo(\"mark\") / status", "1500.0 ms / PAUSED",
                () -> ms(timeline.getCurrentTime()) + " / " + timeline.getStatus()));
        timelineChecks.add(Checks.run("progress (EASE_OUT 0.25 → 1)", () -> f3(progress.get())));
        timelineChecks.add(Checks.expect("counter (int, LINEAR)", 50, counter::get));
        timelineChecks.add(Checks.run("color (EASE_BOTH)", () -> String.valueOf(color.get())));
        timelineChecks.add(Checks.run("point (Interpolatable record)",
                () -> f3(point.get().x()) + ", " + f3(point.get().y())));
        timelineChecks.add(Checks.expect("text (DISCRETE) / flag", "one / false", () -> text.get() + " / " + flag.get()));
        timelineChecks.add(Checks.expect("key frame name / handler runs", "one / 0",
                () -> timeline.getKeyFrames().get(1).getName() + " / " + visited[0]));
        timelineChecks.add(Checks.expect("cycle / total duration", "3000.0 ms / INDEFINITE",
                () -> ms(timeline.getCycleDuration()) + " / "
                        + (timeline.getTotalDuration().isIndefinite() ? "INDEFINITE" : ms(timeline.getTotalDuration()))));
        timelineChecks.add(Checks.run("auto-reverse cycle 2 @1250: time / value",
                () -> ms(reversing.getCurrentTime()) + " / " + f3(wave.get())));
        timelineChecks.add(Checks.expect("Color.interpolate / Integer LINEAR", "0x808080ff / 6", () -> Color.BLACK
                .interpolate(Color.WHITE, 0.5) + " / " + Interpolator.LINEAR.interpolate(1, 11, 0.5)));

        VBox left = new VBox(10, demo, Checks.view("Timeline checks", timelineChecks));
        left.setPrefWidth(500);
        left.setMinWidth(500);
        VBox right = Checks.view("Interpolator.interpolate(0, 1, t)", curveChecks);
        right.setPrefWidth(518);
        HBox bottom = new HBox(10, left, right);

        VBox root = new VBox(12, plots, bottom);
        AnimSupport.register(root, List.of(timeline, reversing));
        return root;
    }

    @Override
    public void dispose(Node content) {
        AnimSupport.stopAll(content);
    }

    private static Map<String, DoubleUnaryOperator> curves() {
        Map<String, DoubleUnaryOperator> curves = new LinkedHashMap<>();
        curves.put("LINEAR", of(Interpolator.LINEAR));
        curves.put("DISCRETE", of(Interpolator.DISCRETE));
        curves.put("EASE_IN", of(Interpolator.EASE_IN));
        curves.put("EASE_OUT", of(Interpolator.EASE_OUT));
        curves.put("EASE_BOTH", of(Interpolator.EASE_BOTH));
        curves.put("SPLINE(.25,.1,.25,1)", of(Interpolator.SPLINE(0.25, 0.1, 0.25, 1)));
        curves.put("SPLINE(.8,0,.2,1)", of(Interpolator.SPLINE(0.8, 0, 0.2, 1)));
        curves.put("BOUNCE (custom)", of(BOUNCE));
        // tangent interpolators only have an effect within a timeline : sample one
        curves.put("TANGENT flat", sampled(new double[] { 0, 1 }, Interpolator.TANGENT(Duration.millis(1000), 0),
                Interpolator.TANGENT(Duration.millis(1000), 1)));
        // 3 key frames : the middle one has different in and out tangents (4 argument variant)
        curves.put("TANGENT in/out, mid key", sampled(new double[] { 0, 0.5, 1 },
                Interpolator.TANGENT(Duration.millis(250), 0.2),
                Interpolator.TANGENT(Duration.millis(250), 0.45, Duration.millis(250), 0.95),
                Interpolator.TANGENT(Duration.millis(250), 1.0)));
        curves.put("STEP_START", of(Interpolator.STEP_START));
        curves.put("STEP_END", of(Interpolator.STEP_END));
        curves.put("STEPS(4, START)", of(Interpolator.STEPS(4, Interpolator.StepPosition.START)));
        curves.put("STEPS(4, END)", of(Interpolator.STEPS(4, Interpolator.StepPosition.END)));
        curves.put("STEPS(4, BOTH)", of(Interpolator.STEPS(4, Interpolator.StepPosition.BOTH)));
        curves.put("STEPS(4, NONE)", of(Interpolator.STEPS(4, Interpolator.StepPosition.NONE)));
        return curves;
    }

    private static DoubleUnaryOperator of(Interpolator interpolator) {
        return t -> interpolator.interpolate(0.0, 1.0, t);
    }

    /**
     * Samples a 1 second timeline whose key frames (evenly spaced, with the given values) use tangent interpolators.
     */
    private static DoubleUnaryOperator sampled(double[] values, Interpolator... interpolators) {
        return t -> {
            DoubleProperty value = new SimpleDoubleProperty();
            Timeline timeline = new Timeline();
            for (int k = 0; k < values.length; k++) {
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(1000.0 * k / (values.length - 1)),
                        new KeyValue(value, values[k], interpolators[k])));
            }
            timeline.play();
            timeline.jumpTo(Duration.millis(t * 1000));
            double result = value.get();
            timeline.stop();
            return result;
        };
    }

    private static Canvas plot(DoubleUnaryOperator curve) {
        Canvas canvas = new Canvas(PLOT_W, PLOT_H);
        GraphicsContext g = canvas.getGraphicsContext2D();
        double left = 8;
        double right = PLOT_W - 8;
        double bottom = PLOT_H - 16;
        double top = 16;
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, PLOT_W, PLOT_H);
        g.setStroke(Color.web("#d9dde3"));
        g.setLineWidth(1);
        g.strokeRect(0.5, 0.5, PLOT_W - 1, PLOT_H - 1);
        g.setStroke(Color.web("#e6e9ee"));
        g.strokeLine(left, top, right, top);
        g.strokeLine(left, bottom, right, bottom);
        g.setLineDashes(3, 3);
        g.setStroke(Color.web("#c3c8d0"));
        g.strokeLine(left, bottom, right, top);
        g.setLineDashes((double[]) null);
        g.setStroke(CURVE);
        g.setLineWidth(2);
        g.beginPath();
        int samples = 160;
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            double v = curve.applyAsDouble(t);
            double x = left + t * (right - left);
            double y = bottom - v * (bottom - top);
            // rounded : the plotted geometry must not depend on the last bits of the math functions
            x = Math.rint(x * 100) / 100;
            y = Math.rint(y * 100) / 100;
            if (i == 0) {
                g.moveTo(x, y);
            } else {
                g.lineTo(x, y);
            }
        }
        g.stroke();
        return canvas;
    }

    private static Node timelineDemo(Timeline timeline, DoubleProperty progress, IntegerProperty counter,
            ObjectProperty<Color> color, ObjectProperty<Vec2> point, StringProperty text, BooleanProperty flag,
            Timeline reversing, DoubleProperty wave) {
        Label title = new Label("Timeline · 3 key frames on custom properties · cue point \"mark\" = 1500 ms");
        title.setFont(Font.font("System", FontWeight.BOLD, 12));

        // time ruler with key frames and the current time
        Pane ruler = new Pane();
        ruler.setPrefSize(480, 34);
        double scale = 460.0 / 3000;
        Line axis = new Line(10, 20, 470, 20);
        axis.setStroke(Color.web("#9aa1ad"));
        ruler.getChildren().add(axis);
        for (KeyFrame frame : timeline.getKeyFrames()) {
            double x = 10 + frame.getTime().toMillis() * scale;
            Rectangle diamond = new Rectangle(x - 5, 15, 10, 10);
            diamond.setRotate(45);
            diamond.setFill(Color.web("#f58518"));
            Label label = new Label(Math.round(frame.getTime().toMillis()) + (frame.getName() == null ? "" : " " + frame.getName()));
            label.setStyle("-fx-font-size: 10px;");
            label.relocate(x - 8, 0);
            ruler.getChildren().addAll(diamond, label);
        }
        Line cursor = new Line(0, 8, 0, 32);
        cursor.setStroke(Color.web("#e45756"));
        cursor.setStrokeWidth(2);
        cursor.translateXProperty().bind(Bindings.createDoubleBinding(
                () -> 10 + timeline.getCurrentTime().toMillis() * scale, timeline.currentTimeProperty()));
        ruler.getChildren().add(cursor);

        // progress bar
        Rectangle track = new Rectangle(300, 12, Color.web("#e6e9ee"));
        Rectangle bar = new Rectangle(0, 12, Color.web("#4c78a8"));
        bar.widthProperty().bind(progress.multiply(300));
        Pane barPane = new Pane(track, bar);
        Label progressLabel = new Label();
        progressLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(Locale.ROOT, "progress %.3f", progress.get()), progress));

        Label counterLabel = new Label();
        counterLabel.textProperty().bind(Bindings.concat("counter ", counter.asString()));
        counterLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Circle swatch = new Circle(16);
        swatch.fillProperty().bind(color);

        Pane pointPane = new Pane();
        pointPane.setPrefSize(110, 60);
        pointPane.setStyle("-fx-border-color: #d9dde3; -fx-background-color: white;");
        Circle dot = new Circle(6, Color.web("#54a24b"));
        dot.centerXProperty().bind(Bindings.createDoubleBinding(() -> 8 + point.get().x() * 94, point));
        dot.centerYProperty().bind(Bindings.createDoubleBinding(() -> 8 + point.get().y() * 44, point));
        pointPane.getChildren().add(dot);

        Label textLabel = new Label();
        textLabel.textProperty().bind(Bindings.concat("text \"", text, "\" · flag ", flag.asString()));

        Rectangle waveTrack = new Rectangle(300, 8, Color.web("#e6e9ee"));
        Rectangle waveBar = new Rectangle(0, 8, Color.web("#b279a2"));
        waveBar.widthProperty().bind(wave.multiply(300));
        Pane wavePane = new Pane(waveTrack, waveBar);
        Label waveLabel = new Label("autoReverse, 3 cycles, at 1250 ms");
        waveLabel.setStyle("-fx-font-size: 11px;");

        HBox row1 = new HBox(10, barPane, progressLabel);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(16, counterLabel, swatch, pointPane, textLabel);
        row2.setAlignment(Pos.CENTER_LEFT);
        HBox row3 = new HBox(10, wavePane, waveLabel);
        row3.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(6, title, ruler, row1, row2, row3);
        box.setStyle("-fx-padding: 8; -fx-border-color: #d9dde3; -fx-background-color: #fbfbfc;");
        return box;
    }
}
