package io.quarkiverse.fx.showcase.pages.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

/**
 * JFXPanel : a JavaFX scene embedded in a Swing component (the reverse of SwingNode). The JFXPanel is in a JFrame that
 * is made displayable but never shown : its content, rendered by JavaFX into the pixel buffer of the panel, is painted
 * into a BufferedImage at the display scale and shown in the page. A Swing mouse click on the JavaFX button is
 * forwarded to the JavaFX scene.
 */
@Singleton
public class JfxPanelPage implements FeaturePage {

    private static final String STATE = JfxPanelPage.class.getName();

    private static final int WIDTH = 460;
    private static final int HEIGHT = 300;

    @Override
    public String id() {
        return "swing-jfxpanel";
    }

    @Override
    public String title() {
        return "JFXPanel: JavaFX in Swing";
    }

    @Override
    public String category() {
        return Categories.SWING;
    }

    @Override
    public int order() {
        return 20;
    }

    private static final class State {
        final List<Check> checks = new ArrayList<>();
        final CompletableFuture<JFXPanel> created = new CompletableFuture<>();
        final CompletableFuture<String> clicked = new CompletableFuture<>();
        final ImageView capture = new ImageView();
        final VBox checksBox = new VBox();
        final Label status = new Label("Waiting for the JFXPanel...");
        Button button;
        Label clickLabel;
        volatile JFrame frame;
        volatile JFXPanel panel;
        CompletionStage<?> ready;
    }

    @Override
    public Node build() {
        State state = new State();
        Scene scene = embeddedScene(state);

        StackPane holder = new StackPane(state.status, state.capture);
        holder.setAlignment(Pos.TOP_LEFT);
        holder.setMinSize(WIDTH + 2, HEIGHT + 2);
        holder.setMaxSize(WIDTH + 2, HEIGHT + 2);
        holder.setStyle("-fx-border-color: #9fb3c8; -fx-border-width: 1;");
        state.capture.setFitWidth(WIDTH);
        state.capture.setFitHeight(HEIGHT);
        state.capture.setSmooth(true);

        VBox left = new VBox(4, caption("JFXPanel content, painted by Swing into a BufferedImage (display scale)"),
                holder);
        state.checksBox.getChildren().add(new Label("Waiting for the checks..."));
        state.checksBox.setPrefWidth(540);
        HBox root = new HBox(16, left, state.checksBox);
        root.getProperties().put(STATE, state);

        SwingUtilities.invokeLater(() -> createFrame(state));
        state.ready = Fx.timeout(state.created, 20_000, "JFXPanel created")
                .thenAcceptAsync(panel -> {
                    // setScene on the JavaFX Application Thread, as applications usually do
                    state.checks.add(Checks.run("JFXPanel.setScene(scene)", () -> {
                        panel.setScene(scene);
                        return scene.getWindow().getClass().getSimpleName() + ", " + (int) scene.getWidth() + "x"
                                + (int) scene.getHeight();
                    }));
                }, Fx.FX_THREAD)
                .thenCompose(v -> Fx.pulses(10))
                .thenCompose(v -> stableCapture(state, 10_000))
                .thenCompose(v -> click(state))
                .thenCompose(v -> Fx.pulses(10))
                .thenCompose(v -> stableCapture(state, 10_000))
                .handleAsync((image, error) -> {
                    if (error != null) {
                        state.checks.add(Check.fail("JFXPanel content", describe(error)));
                    } else {
                        state.capture.setImage(SwingFXUtils.toFXImage(image, null));
                        state.status.setVisible(false);
                        double scale = image.getWidth() / (double) WIDTH;
                        double screenScale = Screen.getPrimary().getOutputScaleX();
                        double outputScale = state.button.getScene().getWindow().getOutputScaleX();
                        state.checks.add(Check.of("embedded window output scale", outputScale == screenScale,
                                outputScale + " (screen " + screenScale + ")"));
                        state.checks.add(Checks.run("content painted by Swing (device pixels)", () -> image.getWidth()
                                + "x" + image.getHeight() + " (scale " + scale + ")"));
                        // background, then the middle of the gradient rectangle (logical coordinates)
                        state.checks.add(Checks.run("pixels (10,10) / (116,104)", () -> hex(image.getRGB(
                                (int) (10 * scale), (int) (10 * scale))) + " / " + hex(image.getRGB((int) (116 * scale),
                                        (int) (104 * scale)))));
                    }
                    state.checksBox.getChildren().setAll(Checks.view("JFXPanel", state.checks));
                    return null;
                }, Fx.FX_THREAD)
                .thenCompose(v -> Fx.pulses(5));
        return root;
    }

    private static Scene embeddedScene(State state) {
        Label title = new Label("A JavaFX scene in a JFXPanel");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#0d47a1"));

        Rectangle gradient = new Rectangle(200, 90);
        gradient.setArcWidth(24);
        gradient.setArcHeight(24);
        gradient.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#43a047")), new Stop(1, Color.web("#1e88e5"))));
        Circle circle = new Circle(45, Color.web("#ffb300"));
        circle.setStroke(Color.web("#e65100"));
        circle.setStrokeWidth(4);
        HBox shapes = new HBox(20, gradient, circle);
        shapes.setAlignment(Pos.CENTER_LEFT);

        Slider slider = new Slider(0, 100, 40);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        slider.setFocusTraversable(false);
        ProgressBar progress = new ProgressBar(0.65);
        progress.setPrefWidth(180);
        HBox controls = new HBox(16, slider, progress);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button("JavaFX Button");
        button.setFocusTraversable(false);
        Label clickLabel = new Label("not clicked");
        button.setOnAction(e -> {
            String thread = Platform.isFxApplicationThread() ? "JavaFX Application Thread" : Thread.currentThread()
                    .getName();
            clickLabel.setText("clicked (Swing mouse events)");
            state.clicked.complete("onAction on the " + thread);
        });
        HBox buttons = new HBox(12, button, clickLabel);
        buttons.setAlignment(Pos.CENTER_LEFT);
        state.button = button;
        state.clickLabel = clickLabel;

        VBox content = new VBox(14, title, shapes, controls, buttons);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #fffde7, #e3f2fd);");
        return new Scene(content, WIDTH, HEIGHT);
    }

    // --- Swing (EDT) ------------------------------------------------------------------------------------------------

    private static void createFrame(State state) {
        try {
            JFXPanel panel = new JFXPanel();
            panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            JFrame frame = new JFrame("JFXPanel");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(panel, BorderLayout.CENTER);
            // displayable (peers created, laid out), never shown
            frame.pack();
            state.frame = frame;
            state.panel = panel;
            state.checks.add(Checks.expect("new JFXPanel() on the EDT", "true / JFXPanel",
                    () -> SwingUtilities.isEventDispatchThread() + " / " + panel.getClass().getSimpleName()));
            state.checks.add(Checks.expect("JFrame displayable / showing", "true / false",
                    () -> frame.isDisplayable() + " / " + frame.isShowing()));
            state.checks.add(Checks.expect("JFXPanel size", WIDTH + "x" + HEIGHT,
                    () -> panel.getWidth() + "x" + panel.getHeight()));
            // The scene is set once the JFXPanel handled the COMPONENT_RESIZED event posted by pack() (queued before
            // this task) : its handler computes the display scale of the panel. When the scene is set before, the
            // embedded window takes the default scale 1.0 and keeps it (a race in JavaFX, in the JVM too), the content
            // is then rendered at scale 1 and stretched.
            SwingUtilities.invokeLater(() -> state.created.complete(panel));
        } catch (Throwable t) {
            state.created.completeExceptionally(t);
        }
    }

    /**
     * The content of the JFXPanel painted into a BufferedImage by Swing, at the display scale (the JFXPanel keeps its
     * pixel buffer at the scale of the graphics it is painted with).
     */
    private static BufferedImage paint(JFXPanel panel) {
        double scale = panel.getGraphicsConfiguration().getDefaultTransform().getScaleX();
        BufferedImage image = new BufferedImage((int) Math.ceil(panel.getWidth() * scale),
                (int) Math.ceil(panel.getHeight() * scale), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.scale(scale, scale);
            panel.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Completes with the content of the JFXPanel once two paints, 4 pulses apart, gave the same non-empty pixels (the
     * JavaFX content reaches the panel asynchronously). A timeout returns the latest paint.
     */
    private static CompletionStage<BufferedImage> stableCapture(State state, double timeoutMillis) {
        CompletableFuture<BufferedImage> done = new CompletableFuture<>();
        long start = System.nanoTime();
        new Object() {
            int[] previous;

            void next() {
                Fx.pulses(4).thenRun(() -> SwingUtilities.invokeLater(() -> {
                    try {
                        BufferedImage image = paint(state.panel);
                        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0,
                                image.getWidth());
                        boolean empty = Arrays.stream(pixels).allMatch(p -> p == 0);
                        boolean same = !empty && Arrays.equals(pixels, previous);
                        previous = pixels;
                        if (same || System.nanoTime() - start > timeoutMillis * 1_000_000) {
                            done.complete(image);
                        } else {
                            Platform.runLater(this::next);
                        }
                    } catch (Throwable t) {
                        done.completeExceptionally(t);
                    }
                }));
            }
        }.next();
        return done;
    }

    /**
     * Presses and releases the mouse over the JavaFX button through Swing : JFXPanel forwards the events to the scene.
     */
    private static CompletionStage<Void> click(State state) {
        Bounds bounds = state.button.localToScene(state.button.getBoundsInLocal());
        int x = (int) Math.round(bounds.getCenterX());
        int y = (int) Math.round(bounds.getCenterY());
        SwingUtilities.invokeLater(() -> {
            JFXPanel panel = state.panel;
            long when = System.currentTimeMillis();
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, when, 0, x, y, 0, false,
                    MouseEvent.NOBUTTON));
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, when, InputEvent.BUTTON1_DOWN_MASK, x,
                    y, 1, false, MouseEvent.BUTTON1));
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, when + 1, 0, x, y, 1, false,
                    MouseEvent.BUTTON1));
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, when + 1, 0, x, y, 1, false,
                    MouseEvent.BUTTON1));
            // no hover left on the button
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_EXITED, when + 2, 0, -1, -1, 0, false,
                    MouseEvent.NOBUTTON));
        });
        return Fx.timeout(state.clicked, 10_000, "JavaFX button action")
                .handle((thread, error) -> {
                    state.checks.add(error == null ? Check.pass("Swing mouse click → JavaFX Button", thread)
                            : Check.fail("Swing mouse click → JavaFX Button", describe(error)));
                    state.checks.add(Checks.expect("JavaFX label after the click", "clicked (Swing mouse events)",
                            state.clickLabel::getText));
                    return null;
                });
    }

    private static Label caption(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #52606d; -fx-font-weight: bold;");
        return label;
    }

    private static String describe(Throwable error) {
        while (error instanceof java.util.concurrent.CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        return Checks.describe(error);
    }

    private static String hex(int argb) {
        return String.format(java.util.Locale.ROOT, "%08x", argb);
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ((State) content.getProperties().get(STATE)).ready;
    }

    @Override
    public void dispose(Node content) {
        State state = (State) content.getProperties().get(STATE);
        if (state == null) {
            return;
        }
        JFXPanel panel = state.panel;
        JFrame frame = state.frame;
        if (panel != null) {
            panel.setScene(null);
        }
        if (frame != null) {
            SwingUtilities.invokeLater(frame::dispose);
        }
    }
}
