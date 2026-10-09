package io.quarkiverse.fx.showcase.pages.charts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;

/**
 * Helpers shared by the chart pages.
 */
final class ChartSupport {

    static final String CSS = "/showcase/charts-anim-3d/charts.css";

    private static final String READY_KEY = "charts.ready";

    private ChartSupport() {
    }

    /**
     * A chart with a small caption below it, sized to {@code width x height} (caption included).
     */
    static VBox cell(Chart chart, String caption, double width, double height) {
        Label label = new Label(caption);
        label.getStyleClass().add("demo-caption");
        label.setMaxWidth(width);
        chart.setMinSize(width, height - 20);
        chart.setPrefSize(width, height - 20);
        chart.setMaxSize(width, height - 20);
        VBox box = new VBox(3, chart, label);
        box.setMinSize(width, height);
        box.setPrefSize(width, height);
        box.setMaxSize(width, height);
        return box;
    }

    /**
     * Chart checks need CSS and layout (axis ranges, styled nodes) : they are computed once the page is shown and
     * a few pulses were rendered, then displayed in {@code holder}.
     * <p>
     * The charts compute their axes (auto ranges, CategoryAxis categories, tick marks) in their own layout pass : if
     * the scene did not lay the page out, the checks would read a chart that was never laid out (e.g. no categories,
     * the default 0..100 range). This happens when an exception thrown by an earlier layout pass left an ancestor of
     * the page in "performing layout" state (JavaFX {@code Parent.layout()} does not reset it) : the scene never lays
     * out anything below it again. The page is then laid out here, so that the chart checks stay meaningful, and the
     * malfunction is reported as a failed check.
     */
    static void checksAfterLayout(Region root, Pane holder, String title, Supplier<List<Check>> checks) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        root.getProperties().put(READY_KEY, done);
        Runnable fill = () -> Fx.pulses(3).thenRun(() -> {
            if (done.isDone()) {
                return;
            }
            try {
                List<Check> all = new ArrayList<>();
                // sized by its parent : the scene's layout pass reached the page, and laid its charts out
                if (root.getWidth() <= 0 || root.getHeight() <= 0) {
                    all.add(Check.fail("page laid out by the scene",
                            "no : the page was not laid out within 3 pulses (layout of an ancestor blocked by an earlier "
                                    + "layout exception?), checks computed after applyCss() / layout() of the page"));
                    root.applyCss();
                    root.layout();
                }
                all.addAll(checks.get());
                holder.getChildren().setAll(Checks.view(title, all));
                done.complete(null);
            } catch (Throwable t) {
                holder.getChildren().setAll(Checks.view(title, List.of(Check.fail("checks", Checks.describe(t)))));
                done.completeExceptionally(t);
            }
        });
        if (root.getScene() != null) {
            fill.run();
        } else {
            root.sceneProperty().addListener(new ChangeListener<Scene>() {
                @Override
                public void changed(ObservableValue<? extends Scene> observable, Scene oldScene, Scene newScene) {
                    if (newScene != null) {
                        root.sceneProperty().removeListener(this);
                        fill.run();
                    }
                }
            });
        }
    }

    static CompletionStage<?> ready(Node content) {
        Object ready = content.getProperties().get(READY_KEY);
        return ready instanceof CompletionStage<?> stage ? stage : CompletableFuture.completedFuture(null);
    }

    /**
     * A number rounded to 3 decimals, without trailing zeros.
     */
    static String num(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e9) {
            return String.valueOf((long) value);
        }
        return new BigDecimal(String.format(Locale.ROOT, "%.3f", value)).stripTrailingZeros().toPlainString();
    }

    static String range(NumberAxis axis) {
        return num(axis.getLowerBound()) + ".." + num(axis.getUpperBound()) + " step " + num(axis.getTickUnit());
    }

    static String paint(Paint paint) {
        return String.valueOf(paint);
    }

    /**
     * First background fill of a region (styled chart bar, pie slice, symbol...).
     */
    static String backgroundFill(Node node) {
        if (node instanceof Region region && region.getBackground() != null && !region.getBackground().getFills().isEmpty()) {
            return paint(region.getBackground().getFills().getFirst().getFill());
        }
        return "none";
    }

    /**
     * Number of CSS properties a chart class declares on top of its super class (static CssMetaData lists, built in
     * the class initializers).
     */
    static int ownCssProperties(List<CssMetaData<? extends Styleable, ?>> own,
            List<CssMetaData<? extends Styleable, ?>> parent) {
        Set<String> inherited = new HashSet<>();
        parent.forEach(m -> inherited.add(m.getProperty()));
        return (int) own.stream().filter(m -> !inherited.contains(m.getProperty())).count();
    }

    static List<Node> lookupAll(Node root, String selector) {
        return new ArrayList<>(root.lookupAll(selector));
    }
}
