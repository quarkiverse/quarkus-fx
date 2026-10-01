package io.quarkiverse.fx.showcase.pages.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.MainView;
import io.quarkiverse.fx.showcase.core.Platforms;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Axis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Helpers shared by the windows pages : captions, window capture, "live" scenarios opening real windows.
 */
final class WindowSupport {

    static final String CSS = "/showcase/windows/windows.css";

    /**
     * Width available to a page : the page frame minus its padding (16 on each side, see app.css .page-frame).
     */
    static final double PAGE_CONTENT_WIDTH = MainView.PAGE_WIDTH - 32;

    private static final String FONT_AWESOME = "/showcase/fonts/fa-solid-900.ttf";
    private static final String LIVE_KEY = "showcase.windows.live";
    private static final String SECTION_KEY = "showcase.windows.live-section";
    private static final String TITLE_KEY = "showcase.windows.live-title";
    private static final String INITIAL_KEY = "showcase.windows.live-initial";
    private static String iconFamily;
    private static Font iconFont;

    private WindowSupport() {
    }

    /**
     * The root of a page : styled, and as wide as the page frame, so that long check values wrap instead of widening
     * the page (and its snapshot).
     */
    static VBox page(double spacing) {
        VBox root = styled(new VBox(spacing));
        root.getStyleClass().add("windows-page");
        root.setPrefWidth(PAGE_CONTENT_WIDTH);
        root.setMaxWidth(PAGE_CONTENT_WIDTH);
        return root;
    }

    /**
     * Adds the stylesheet of the windows pages to {@code parent}.
     */
    static <T extends Parent> T styled(T parent) {
        parent.getStylesheets().add(Fx.resourceUrl(CSS));
        return parent;
    }

    /**
     * Applies CSS to {@code parent} (creating the skins) and disables the animation of the axes found in it : the
     * tick marks and labels of a Slider are drawn by an animated NumberAxis, captured at an arbitrary frame otherwise.
     */
    static void freezeAxes(Parent parent) {
        parent.applyCss();
        parent.layout();
        visit(parent, node -> {
            if (node instanceof Axis<?> axis) {
                axis.setAnimated(false);
            }
        });
    }

    static void visit(Node node, Consumer<Node> action) {
        action.accept(node);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                visit(child, action);
            }
        }
    }

    static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("demo-caption");
        return label;
    }

    static Label note(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("demo-note");
        label.setWrapText(true);
        return label;
    }

    static VBox demo(String caption, Node node) {
        VBox box = new VBox(4, caption(caption), node);
        box.getStyleClass().add("demo");
        return box;
    }

    /**
     * A Font Awesome (solid) glyph, the font being loaded once from the classpath.
     */
    static Label icon(char glyph, double size, String color) {
        Label label = new Label(String.valueOf(glyph));
        label.setFont(Font.font(iconFamily(), size));
        label.setStyle("-fx-text-fill: " + color + ";");
        label.getStyleClass().add("fa-icon");
        return label;
    }

    static synchronized String iconFamily() {
        if (iconFamily == null) {
            iconFont = Font.loadFont(Fx.resourceUrl(FONT_AWESOME), 14);
            iconFamily = iconFont == null ? "System" : iconFont.getFamily();
        }
        return iconFamily;
    }

    /**
     * The icon font is loaded from a classpath URL (Font.loadFont reads the stream into a temporary file registered
     * with the OS) : a failure would silently fall back to the System font, drawing missing glyph boxes.
     */
    static Check iconFontCheck() {
        return Checks.run("Font.loadFont(fa-solid-900.ttf)", () -> {
            String family = iconFamily();
            if (iconFont == null) {
                throw new IllegalStateException("Font.loadFont returned null, icons drawn with " + family);
            }
            return "family " + iconFont.getFamily() + ", name " + iconFont.getName();
        });
    }

    /**
     * The window showing {@code content} (the main window).
     */
    static Stage mainStage(Node content) {
        Scene scene = content.getScene();
        Window window = scene == null ? null : scene.getWindow();
        return window instanceof Stage stage ? stage : null;
    }

    /**
     * Snapshot of the whole scene content of a window, over the scene fill (transparent when there is none).
     * <p>
     * The last of 3 snapshots is returned : Prism renders a complex shape (a Path, like the tick marks of a Slider)
     * directly the first 2 times, then from a cached mask, which differs by +-1 on anti-aliased edges. How many times a
     * node of a window was already rendered depends on the timing of its repaints, the warm-up snapshots make sure the
     * captured one always uses the cache.
     */
    static Image capture(Scene scene) {
        Parent root = scene.getRoot();
        SnapshotParameters parameters = new SnapshotParameters();
        Paint fill = scene.getFill();
        parameters.setFill(fill == null ? Color.TRANSPARENT : fill);
        Image image = null;
        for (int i = 0; i < 3; i++) {
            image = root.snapshot(parameters, null);
        }
        return image;
    }

    /**
     * In snapshot mode, removes from a secondary window what would make its snapshot vary between runs :
     * <ul>
     * <li>hover, which depends on the mouse position : the root is made mouse transparent (already done by
     * {@link #shield(Window)} right after show()) and the hover states that were set anyway are cleared;</li>
     * <li>focus visuals, which depend on the OS window focus : the focus is moved to the root, which has no focused
     * style;</li>
     * <li>effects (the drop shadows of modena.css popups, slider thumbs, scroll bar arrows...) : Prism renders them
     * through textures taken from a pool, the one picked depends on the pool history (ties between differently shaped
     * textures are broken by list order) and its shape changes the blurred pixels by +-1, so that two snapshots of
     * the same popup in a row can differ. Effects set by the user agent stylesheet are overridden by a value set from
     * code.</li>
     * </ul>
     */
    static void stabilize(Scene scene) {
        if (ShowcaseMode.snapshot() && scene != null && scene.getRoot() != null) {
            Parent root = scene.getRoot();
            root.setMouseTransparent(true);
            visit(root, node -> {
                if (node.isHover()) {
                    // mouseTransparent stops new hover states, not the ones already set
                    Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_EXITED_TARGET, 0, 0, 0, 0, MouseButton.NONE,
                            0, false, false, false, false, false, false, false, false, false, false, null));
                }
                if (node.getEffect() != null) {
                    node.setEffect(null);
                }
            });
            root.requestFocus();
        }
    }

    /**
     * In snapshot mode, makes the content of a window that was just shown ignore the mouse. Called in the same Fx
     * thread runnable as show() : the OS mouse events of the new window (a window appearing under a still mouse
     * cursor gets an "entered" event) are only processed afterwards, so no node takes the hover state, whatever the
     * position of the mouse cursor during the run.
     */
    static void shield(Window window) {
        if (ShowcaseMode.snapshot() && window != null && window.getScene() != null
                && window.getScene().getRoot() != null) {
            window.getScene().getRoot().setMouseTransparent(true);
        }
    }

    /**
     * Identity set of the windows currently showing.
     */
    static Set<Window> showingWindows() {
        Set<Window> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(Window.getWindows());
        return set;
    }

    /**
     * The first window showing now which was not in {@code before} and matches {@code filter}.
     */
    static Window newWindow(Set<Window> before, Predicate<Window> filter) {
        for (Window window : Window.getWindows()) {
            if (!before.contains(window) && window.isShowing() && filter.test(window)) {
                return window;
            }
        }
        return null;
    }

    static String size(double width, double height) {
        return fmt(width) + "x" + fmt(height);
    }

    /**
     * Whether a coordinate or size of {@code window} is the requested one, give or take less than one device pixel :
     * with a fractional output scale (125 % or 150 % on Windows, for example), the OS places and sizes windows in whole
     * device pixels, so a logical coordinate can come back rounded. With the integer scales of macOS, only an exact
     * match passes.
     */
    static boolean near(double actual, double requested, Window window) {
        double scale = Math.max(1, Math.max(window.getOutputScaleX(), window.getOutputScaleY()));
        return Math.abs(actual - requested) < 1 / scale;
    }

    /**
     * Status of a check of the position or size of a window, {@code ok} telling whether it is the requested one. A
     * mismatch is a failure, except on Linux where the window manager has the last word on window geometry (GNOME
     * attaches modal dialogs to the center of their owner, window managers keep new windows inside the work area...) :
     * the check is informational there.
     */
    static Boolean placement(boolean ok) {
        return ok ? Boolean.TRUE : Platforms.isLinux() ? null : Boolean.FALSE;
    }

    /**
     * A coordinate or size rounded to 0.1 (layout computations may leave floating point noise).
     */
    static String fmt(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        if (rounded == 0) {
            rounded = 0; // no "-0"
        }
        return rounded == Math.rint(rounded) ? String.valueOf((long) rounded)
                : String.format(java.util.Locale.ROOT, "%.1f", rounded);
    }

    /**
     * State of a live scenario run for one page content : checks, captured images, windows to close on dispose.
     */
    static final class Live {

        final List<Check> checks = new ArrayList<>();
        final Map<String, Image> images = new LinkedHashMap<>();
        /** Facts recorded by the steps, per captured image key. */
        final Map<String, String> notes = new LinkedHashMap<>();
        /**
         * Whether the captured window is at the requested position (see {@link WindowSupport#near}), per captured image
         * key.
         */
        final Map<String, Boolean> placed = new LinkedHashMap<>();
        final List<Window> opened = new ArrayList<>();
        private final String title;
        private boolean disposed;

        Live(String title) {
            this.title = title;
        }

        /**
         * Runs {@code step}, recording a failed check named {@code name} if it throws or completes exceptionally.
         */
        CompletionStage<Void> step(CompletionStage<Void> previous, String name,
                Function<Void, CompletionStage<Void>> step) {
            return previous.thenCompose(v -> {
                if (disposed) {
                    return CompletableFuture.completedFuture(null);
                }
                CompletionStage<Void> stage;
                try {
                    stage = step.apply(null);
                } catch (Throwable t) {
                    stage = CompletableFuture.failedFuture(t);
                }
                return Fx.timeout(stage, 10_000, name).exceptionally(t -> {
                    Throwable cause = t instanceof java.util.concurrent.CompletionException && t.getCause() != null
                            ? t.getCause()
                            : t;
                    checks.add(Check.fail(name, Checks.describe(cause)));
                    return null;
                });
            });
        }

        void closeAll() {
            for (Window window : List.copyOf(opened)) {
                try {
                    if (window.isShowing()) {
                        window.hide();
                    }
                } catch (Throwable t) {
                    checks.add(Check.fail("close " + window.getClass().getSimpleName(), Checks.describe(t)));
                }
            }
            opened.clear();
        }
    }

    /**
     * A section showing the results of a live scenario, with a button running it (interactive mode). The section is
     * registered on {@code content}, where {@link #publish(Node, Live)} finds it.
     */
    static VBox liveSection(Node content, String title, List<Check> initialChecks, String hint,
            Function<Node, CompletionStage<Void>> scenario) {
        VBox results = new VBox(Checks.view(title, initialChecks));
        content.getProperties().put(TITLE_KEY, title);
        content.getProperties().put(INITIAL_KEY, List.copyOf(initialChecks));
        Button run = new Button("Run live checks");
        Label hintLabel = note(hint);
        HBox header = new HBox(10, run, hintLabel);
        HBox.setHgrow(hintLabel, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox section = new VBox(4, header, results);
        section.getStyleClass().add("live-section");
        content.getProperties().put(SECTION_KEY, results);
        run.setOnAction(e -> {
            run.setDisable(true);
            scenario.apply(content).whenComplete((v, t) -> run.setDisable(false));
        });
        return section;
    }

    static Live live(Node content) {
        Object live = content.getProperties().get(LIVE_KEY);
        return live instanceof Live l ? l : null;
    }

    /**
     * Starts a new live run on {@code content}: the previous one, if any, is cleaned up.
     */
    @SuppressWarnings("unchecked")
    static Live startLive(Node content) {
        Live previous = live(content);
        if (previous != null) {
            previous.closeAll();
        }
        Live live = new Live(String.valueOf(content.getProperties().get(TITLE_KEY)));
        Object initial = content.getProperties().get(INITIAL_KEY);
        if (initial instanceof List<?> list) {
            live.checks.addAll((List<Check>) list);
        }
        content.getProperties().put(LIVE_KEY, live);
        return live;
    }

    /**
     * Shows the checks of a finished live run in the live section of {@code content}.
     */
    static void publish(Node content, Live live) {
        Object results = content.getProperties().get(SECTION_KEY);
        if (results instanceof VBox box) {
            box.getChildren().setAll(Checks.view(live.title, live.checks));
        }
    }

    static CompletionStage<Map<String, Image>> images(Node content) {
        Live live = live(content);
        return CompletableFuture.completedFuture(live == null ? Map.of() : new LinkedHashMap<>(live.images));
    }

    /**
     * Closes the windows opened by the live run of {@code content}, and skips its remaining steps if it is running.
     */
    static void dispose(Node content) {
        Live live = live(content);
        if (live != null) {
            live.disposed = true;
            live.closeAll();
        }
    }
}
