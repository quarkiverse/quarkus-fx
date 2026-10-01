package io.quarkiverse.fx.showcase.pages.controls;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

/**
 * Shared building blocks of the controls pages : captioned demo boxes, the group stylesheet and the fonts loaded from
 * the classpath.
 */
final class ControlsUi {

    static final String FONT_AWESOME = "/showcase/fonts/fa-solid-900.ttf";
    static final String DROID_KUFI = "/showcase/fonts/DroidKufi-Regular.ttf";

    static final String HOME = "\uf015";
    static final String HEART = "\uf004";
    static final String COG = "\uf013";
    static final String SEARCH = "\uf002";
    static final String SAVE = "\uf0c7";
    static final String FOLDER = "\uf07b";
    static final String TRASH = "\uf1f8";
    static final String BOLD = "\uf032";
    static final String ITALIC = "\uf033";
    static final String UNDERLINE = "\uf0cd";
    static final String CUT = "\uf0c4";
    static final String COPY = "\uf0c5";
    static final String PASTE = "\uf0ea";
    static final String UNDO = "\uf0e2";
    static final String REDO = "\uf01e";
    static final String PRINT = "\uf02f";
    static final String STAR = "\uf005";
    static final String USER = "\uf007";
    static final String BELL = "\uf0f3";
    static final String INFO = "\uf05a";

    private static Font fontAwesome;
    private static String fontAwesomeError;
    private static Font droidKufi;
    private static String droidKufiError;

    private ControlsUi() {
    }

    /**
     * The group stylesheet, loaded from the classpath.
     */
    static String stylesheet() {
        return Fx.resourceUrl("/showcase/controls/controls.css");
    }

    /**
     * Font Awesome 5 Free Solid, loaded once from a classpath URL ({@link Font#loadFont(String, double)}). When the
     * font cannot be loaded, the default font is returned (glyphs render as missing characters) and
     * {@link #fontAwesomeCheck(String)} reports the failure : the rest of the page still renders.
     */
    static synchronized Font fontAwesome() {
        if (fontAwesome == null && fontAwesomeError == null) {
            try {
                fontAwesome = Font.loadFont(Fx.resourceUrl(FONT_AWESOME), 14);
                if (fontAwesome == null) {
                    fontAwesomeError = "Font.loadFont returned null for " + FONT_AWESOME;
                }
            } catch (Throwable t) {
                fontAwesomeError = Checks.describe(t);
            }
        }
        return fontAwesome != null ? fontAwesome : Font.font(14);
    }

    static Check fontAwesomeCheck(String name) {
        Font font = fontAwesome();
        return fontAwesomeError == null ? Check.pass(name, describe(font)) : Check.fail(name, fontAwesomeError);
    }

    /**
     * Droid Arabic Kufi, loaded once from a classpath stream ({@link Font#loadFont(InputStream, double)}), or the
     * default font when it cannot be loaded (see {@link #droidKufiCheck(String)}).
     */
    static synchronized Font droidKufi() {
        if (droidKufi == null && droidKufiError == null) {
            try (InputStream in = Fx.resource(DROID_KUFI).openStream()) {
                droidKufi = Font.loadFont(in, 13);
                if (droidKufi == null) {
                    droidKufiError = "Font.loadFont returned null for " + DROID_KUFI;
                }
            } catch (Throwable t) {
                droidKufiError = Checks.describe(t);
            }
        }
        return droidKufi != null ? droidKufi : Font.font(13);
    }

    static Check droidKufiCheck(String name) {
        Font font = droidKufi();
        return droidKufiError == null ? Check.pass(name, describe(font)) : Check.fail(name, droidKufiError);
    }

    /**
     * A deterministic description of a node for checks : its type and id ({@code Node.toString()} contains an
     * identity hash code).
     */
    static String describe(Node node) {
        if (node == null) {
            return "none";
        }
        return node.getClass().getSimpleName() + (node.getId() == null ? "" : "#" + node.getId());
    }

    /**
     * Family and name of a font, or only the family when both are the same.
     */
    static String describe(Font font) {
        return font.getFamily().equals(font.getName()) ? font.getFamily()
                : font.getFamily() + " / " + font.getName();
    }

    /**
     * A Font Awesome glyph as a label.
     */
    static Label glyph(String code, double size) {
        Label label = new Label(code);
        label.setFont(new Font(fontAwesome().getName(), size));
        label.getStyleClass().add("glyph");
        return label;
    }

    static Label glyph(String code, double size, Color color) {
        Label label = glyph(code, size);
        label.setTextFill(color);
        return label;
    }

    static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("demo-caption");
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    /**
     * A bordered box : a small caption above the demo nodes.
     */
    static VBox demo(String caption, Node... content) {
        VBox box = new VBox(5);
        box.getStyleClass().add("demo-box");
        box.getChildren().add(caption(caption));
        box.getChildren().addAll(content);
        return box;
    }

    /**
     * A page root with the group stylesheet.
     */
    static VBox page(double spacing, Node... content) {
        VBox root = new VBox(spacing, content);
        root.getStyleClass().add("controls-page");
        root.getStylesheets().add(stylesheet());
        root.setPadding(Insets.EMPTY);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return root;
    }

    /**
     * First node of the given type in the tree of {@code node} (depth first, including {@code node}).
     */
    static <T> T find(Node node, Class<T> type) {
        return find(node, type, null);
    }

    /**
     * First node of the given type, and having the given style class when not null.
     */
    static <T> T find(Node node, Class<T> type, String styleClass) {
        if (type.isInstance(node) && (styleClass == null || node.getStyleClass().contains(styleClass))) {
            return type.cast(node);
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = find(child, type, styleClass);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * The tick marks and labels of a Slider are drawn by a NumberAxis, animated by default : its tick labels fade in
     * (750 ms) each time they are laid out while showing, which is longer than the snapshot settle time. In snapshot
     * mode the axis animation is disabled as soon as the skin creates the axis (before its first layout).
     */
    static Slider staticTicks(Slider slider) {
        if (ShowcaseMode.snapshot()) {
            slider.skinProperty().addListener((observable, oldSkin, newSkin) -> {
                if (newSkin != null && slider.lookup(".axis") instanceof Axis<?> axis) {
                    axis.setAnimated(false);
                }
            });
        }
        return slider;
    }

    static <T extends Node> T grow(T node) {
        javafx.scene.layout.HBox.setHgrow(node, javafx.scene.layout.Priority.ALWAYS);
        return node;
    }

    /**
     * A container whose content is replaced once the page has been laid out, for checks depending on the layout.
     */
    static final class ChecksHolder extends VBox {

        final List<Check> early = new ArrayList<>();

        ChecksHolder() {
            getStyleClass().add("checks-holder");
        }

        void show(String title, List<Check> late) {
            show(title, late, 1);
        }

        /**
         * Shows the early and late checks, split into {@code columns} tables of equal width (the checks are
         * collected in order, the first column first).
         */
        void show(String title, List<Check> late, int columns) {
            List<Check> all = new ArrayList<>(early);
            all.addAll(late);
            if (columns <= 1) {
                getChildren().setAll(Checks.view(title, all));
                return;
            }
            double gap = 12;
            HBox row = new HBox(gap);
            // the holder is laid out when the late checks are shown : a fixed column width lets the (wrapped) values
            // compute their height
            double width = getWidth() > 0 ? getWidth() : getPrefWidth();
            double columnWidth = Math.floor((width - getInsets().getLeft() - getInsets().getRight()
                    - gap * (columns - 1)) / columns);
            int perColumn = (all.size() + columns - 1) / columns;
            for (int i = 0; i < columns; i++) {
                List<Check> part = all.subList(Math.min(all.size(), i * perColumn),
                        Math.min(all.size(), (i + 1) * perColumn));
                VBox view = Checks.view(i == 0 ? title : title + " (continued)", part);
                view.setMinWidth(columnWidth);
                view.setPrefWidth(columnWidth);
                view.setMaxWidth(columnWidth);
                row.getChildren().add(view);
            }
            getChildren().setAll(row);
        }
    }

    private static final String FOCUS_BEFORE = "controls.focusBefore";

    /**
     * Remembers the focus owner of the scene when {@code content} is added to it, see {@link #restoreFocus(Node)}.
     */
    static void rememberFocus(Node content) {
        content.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                Node owner = newScene.getFocusOwner();
                if (owner != null) {
                    content.getProperties().put(FOCUS_BEFORE, owner);
                } else {
                    content.getProperties().remove(FOCUS_BEFORE);
                }
            }
        });
    }

    /**
     * Some skins request the focus (e.g. ButtonBarSkin on its default button) : a focused control is only painted as
     * such while the window is active, which is not deterministic. Gives the focus back to the node that owned it
     * before the page was shown.
     */
    static void restoreFocus(Node content) {
        javafx.scene.Scene scene = content.getScene();
        if (scene == null) {
            return;
        }
        Node owner = scene.getFocusOwner();
        boolean inside = false;
        for (Node n = owner; n != null; n = n.getParent()) {
            if (n == content) {
                inside = true;
                break;
            }
        }
        if (inside) {
            Node before = (Node) content.getProperties().get(FOCUS_BEFORE);
            if (before != null && before.getScene() == scene) {
                before.requestFocus();
            } else {
                scene.getRoot().requestFocus();
            }
        }
    }

    /**
     * An invisible node carrying checks computed after the page snapshot (e.g. while capturing popups).
     */
    static Pane hiddenChecks() {
        Pane pane = new Pane();
        pane.setManaged(false);
        pane.setVisible(false);
        pane.getStyleClass().add("hidden-checks");
        return pane;
    }

    /**
     * Makes popup windows deterministic while they are captured : every popup shown while installed ignores the mouse
     * (no hover effect whatever the pointer position) and has no focused item (a popup is focused only while its owner
     * window is, so a focused cell or square would depend on the active application). Popups shown while installed
     * are recorded, newest last.
     */
    static final class PopupGuard implements ListChangeListener<Window> {

        final List<PopupWindow> shown = new ArrayList<>();

        PopupGuard install() {
            Window.getWindows().addListener(this);
            return this;
        }

        void uninstall() {
            Window.getWindows().removeListener(this);
        }

        @Override
        public void onChanged(Change<? extends Window> change) {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) {
                    if (window instanceof PopupWindow popup && popup.getScene() != null) {
                        shown.add(popup);
                        calm(popup);
                    }
                }
            }
        }

        /**
         * Removes the mouse and the focus from {@code popup} content (skins request the focus on their content after
         * the popup is shown, e.g. ComboBoxPopupControl).
         */
        static void calm(PopupWindow popup) {
            Parent root = popup.getScene().getRoot();
            root.setMouseTransparent(true);
            root.requestFocus();
        }

        /**
         * The last popup shown while installed and still showing.
         */
        PopupWindow lastShowing() {
            for (int i = shown.size() - 1; i >= 0; i--) {
                if (shown.get(i).isShowing()) {
                    return shown.get(i);
                }
            }
            return null;
        }
    }
}
