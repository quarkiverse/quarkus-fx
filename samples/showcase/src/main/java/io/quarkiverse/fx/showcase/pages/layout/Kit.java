package io.quarkiverse.fx.showcase.pages.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.animation.AnimationTimer;
import javafx.css.CssParser;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

/**
 * Small building blocks shared by the layout and CSS pages.
 */
final class Kit {

    private static final String READY = "layout.ready";

    private Kit() {
    }

    /**
     * URL of a stylesheet of this group (in /showcase/layout/).
     */
    static String css(String name) {
        return Fx.resourceUrl("/showcase/layout/" + name);
    }

    /**
     * Page root, with the stylesheets shared by the group plus {@code stylesheets}.
     */
    static VBox page(String styleClass, String... stylesheets) {
        VBox root = new VBox(10);
        root.getStyleClass().addAll("layout-page", styleClass);
        root.getStylesheets().add(css("common.css"));
        for (String stylesheet : stylesheets) {
            root.getStylesheets().add(css(stylesheet));
        }
        return root;
    }

    /**
     * A captioned demo : a small caption above a framed area of a fixed size, clipped.
     */
    static VBox demo(String caption, double width, double height, Node content) {
        StackPane area = area(width, height, content);
        return captioned(caption, area);
    }

    static VBox captioned(String caption, Node content) {
        Label label = new Label(caption);
        label.getStyleClass().add("demo-caption");
        // the caption never widens the demo : it is ellipsized to the width of the content
        label.setMinWidth(0);
        label.setPrefWidth(1);
        label.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(3, label, content);
        box.setFillWidth(true);
        box.getStyleClass().add("demo");
        return box;
    }

    static StackPane area(double width, double height, Node content) {
        StackPane area = new StackPane(content);
        area.getStyleClass().add("demo-area");
        area.setMinSize(width, height);
        area.setPrefSize(width, height);
        area.setMaxSize(width, height);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(area.widthProperty());
        clip.heightProperty().bind(area.heightProperty());
        area.setClip(clip);
        return area;
    }

    /**
     * A fixed size colored block, with a centered text.
     */
    static Label block(String text, String color, double width, double height) {
        Label label = new Label(text);
        label.getStyleClass().add("block");
        label.setAlignment(Pos.CENTER);
        label.setBackground(Background.fill(Color.web(color)));
        label.setMinSize(width, height);
        label.setPrefSize(width, height);
        label.setMaxSize(width, height);
        return label;
    }

    /**
     * A block that can grow in both directions from its preferred size.
     */
    static Label growing(String text, String color, double width, double height) {
        Label label = block(text, color, width, height);
        label.setMinSize(10, 10);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return label;
    }

    /**
     * A grid of {@code columns} columns of a fixed width.
     */
    static GridPane grid(int columns, double columnWidth, double hgap, double vgap) {
        GridPane grid = new GridPane();
        grid.setHgap(hgap);
        grid.setVgap(vgap);
        for (int i = 0; i < columns; i++) {
            ColumnConstraints column = new ColumnConstraints(columnWidth, columnWidth, columnWidth);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    static void addAll(GridPane grid, int columns, Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            grid.add(nodes[i], i % columns, i / columns);
        }
    }

    /**
     * Two check tables side by side, filled once the page is shown and laid out (see {@link #whenShown}).
     */
    static HBox checksRow(double width) {
        HBox row = new HBox(16);
        row.setPrefWidth(width);
        row.getStyleClass().add("checks-row");
        return row;
    }

    static void fillChecks(HBox row, String leftTitle, List<Check> left, String rightTitle, List<Check> right) {
        VBox leftView = Checks.view(leftTitle, left);
        VBox rightView = Checks.view(rightTitle, right);
        // fixed widths : the wrapped values of the check tables are measured against them
        double width = Math.floor((row.getPrefWidth() - row.getSpacing()) / 2);
        for (VBox view : List.of(leftView, rightView)) {
            view.setMinWidth(width);
            view.setPrefWidth(width);
            view.setMaxWidth(width);
        }
        row.getChildren().setAll(leftView, rightView);
    }

    /**
     * Runs {@code action} once {@code root} is in a scene and a few pulses were rendered (CSS applied, layout done).
     * The returned stage is also what {@link #ready(Node)} returns for {@code root}.
     */
    static CompletableFuture<Void> whenShown(Node root, int pulses, Supplier<CompletionStage<?>> action) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        root.getProperties().put(READY, done);
        Fx.when(root.sceneProperty(), Objects::nonNull)
                .thenCompose(scene -> Fx.pulses(pulses))
                .thenCompose(v -> action.get())
                .whenComplete((v, error) -> {
                    if (error != null) {
                        done.completeExceptionally(error);
                    } else {
                        done.complete(null);
                    }
                });
        return done;
    }

    static CompletableFuture<Void> whenShown(Node root, int pulses, Runnable action) {
        return whenShown(root, pulses, () -> {
            action.run();
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * Completes on the first pulse where {@code condition} holds, or after {@code maxPulses} pulses.
     */
    static CompletionStage<Void> until(BooleanSupplier condition, int maxPulses) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        new AnimationTimer() {
            private int count;

            @Override
            public void handle(long now) {
                if (condition.getAsBoolean() || ++count >= maxPulses) {
                    stop();
                    done.complete(null);
                }
            }
        }.start();
        return done;
    }

    static CompletionStage<?> ready(Node content) {
        Object ready = content.getProperties().get(READY);
        return ready instanceof CompletionStage<?> stage ? stage : CompletableFuture.completedFuture(null);
    }

    static String num(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    static String bounds(Bounds b) {
        return num(b.getMinX()) + ", " + num(b.getMinY()) + " " + num(b.getWidth()) + "x" + num(b.getHeight());
    }

    static String paint(Paint paint) {
        if (paint instanceof Color c) {
            return String.format(Locale.ROOT, "#%02x%02x%02x%s", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255),
                    Math.round(c.getBlue() * 255),
                    c.getOpacity() < 1 ? String.format(Locale.ROOT, "%02x", Math.round(c.getOpacity() * 255)) : "");
        }
        return paint == null ? "null" : paint.getClass().getSimpleName();
    }

    /**
     * Fill of the first (or {@code index}) background layer of a region.
     */
    static String fill(Region region, int index) {
        Background background = region.getBackground();
        if (background == null || background.getFills().size() <= index) {
            return "none";
        }
        BackgroundFill fill = background.getFills().get(index);
        return paint(fill.getFill());
    }

    static String fill(Region region) {
        return fill(region, 0);
    }

    /**
     * Current size of the CSS error list : errors reported after this mark are those of the page.
     */
    static int cssErrorMark() {
        return CssParser.errorsProperty().size();
    }

    static List<String> cssErrorsSince(int mark) {
        List<String> errors = new ArrayList<>();
        List<CssParser.ParseError> all = CssParser.errorsProperty();
        for (int i = Math.min(mark, all.size()); i < all.size(); i++) {
            // messages may embed Object.toString() of a node : identity hash codes differ between runs
            errors.add(all.get(i).getMessage().replaceAll("@[0-9a-f]{4,}", "@…"));
        }
        return errors;
    }

    static Check cssErrorsCheck(String name, int mark) {
        List<String> errors = cssErrorsSince(mark);
        return Check.of(name, errors.isEmpty(), errors.isEmpty() ? "none" : String.join(" | ", errors));
    }

    static Pane spacer() {
        Pane pane = new Pane();
        HBox.setHgrow(pane, Priority.ALWAYS);
        VBox.setVgrow(pane, Priority.ALWAYS);
        return pane;
    }
}
