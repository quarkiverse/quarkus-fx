package io.quarkiverse.fx.showcase.pages.windows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PopupControl;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Popup windows : a custom {@link Popup}, the popups of ComboBox, DatePicker, ColorPicker, ChoiceBox and MenuButton, a
 * ContextMenu and a Tooltip, each shown at a fixed position relative to its owner node, captured and hidden. Also the
 * system menu bar used by the main window.
 */
@Singleton
public class PopupsPage implements FeaturePage {

    private static final String LIVE_TITLE = "Popup windows (shown, captured, hidden) and menu bars";
    private static final String CONTROLS_KEY = "windows.popups.controls";
    private static final String GALLERY_KEY = "windows.popups.gallery";
    private static final List<String> GALLERY = List.of("popup", "combo-box", "date-picker", "color-picker",
            "choice-box", "menu-button", "context-menu", "tooltip");

    /**
     * The nodes of one page content that own popups.
     */
    record Controls(Button popupButton, ComboBox<String> comboBox, DatePicker datePicker, ColorPicker colorPicker,
            ChoiceBox<String> choiceBox, MenuButton menuButton, Label contextTarget, ContextMenu contextMenu,
            Label tooltipTarget, MenuBar menuBar) {
    }

    @Override
    public String id() {
        return "windows-popups";
    }

    @Override
    public String title() {
        return "Popups, Menus & Tooltips";
    }

    @Override
    public String category() {
        return Categories.WINDOWS;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Node build() {
        VBox root = WindowSupport.page(12);

        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("Mercury", "Venus", "Earth", "Mars",
                "Jupiter", "Saturn", "Uranus", "Neptune"));
        comboBox.setValue("Earth");
        comboBox.setVisibleRowCount(5);
        comboBox.setPrefWidth(150);

        DatePicker datePicker = new DatePicker(LocalDate.of(2024, 3, 15));
        datePicker.setShowWeekNumbers(true);
        datePicker.setPrefWidth(150);

        ColorPicker colorPicker = new ColorPicker(Color.web("#3a7bd5"));
        colorPicker.setPrefWidth(150);

        ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList("Small", "Medium", "Large"));
        choiceBox.setValue("Medium");
        choiceBox.setPrefWidth(120);

        MenuButton menuButton = new MenuButton("Actions", WindowSupport.icon('\uf013', 13, "#2f6fb5"));
        menuButton.getItems().setAll(menuItems());

        Label contextTarget = new Label("Right-click: ContextMenu");
        contextTarget.getStyleClass().addAll("popup-demo-target", "css-icon");
        ContextMenu contextMenu = contextMenu();
        contextTarget.setContextMenu(contextMenu);

        Label tooltipTarget = new Label("Hover: Tooltip");
        tooltipTarget.getStyleClass().addAll("popup-demo-target", "css-icon");
        tooltipTarget.setTooltip(tooltip());

        Button popupButton = new Button("Show Popup");
        popupButton.setOnAction(e -> {
            if (!ShowcaseMode.snapshot()) {
                Popup popup = popup(true);
                Bounds b = popupButton.localToScreen(popupButton.getBoundsInLocal());
                popup.show(popupButton, b.getMinX(), b.getMaxY() + 4);
            }
        });

        MenuBar menuBar = embeddedMenuBar();

        GridPane controls = new GridPane();
        controls.setHgap(22);
        controls.setVgap(10);
        controls.addRow(0,
                WindowSupport.demo("Popup (custom content)", popupButton),
                WindowSupport.demo("ComboBox · 5 visible rows", comboBox),
                WindowSupport.demo("DatePicker · week numbers", datePicker),
                WindowSupport.demo("ColorPicker", colorPicker),
                WindowSupport.demo("ChoiceBox", choiceBox),
                WindowSupport.demo("MenuButton", menuButton));
        VBox menuBarDemo = WindowSupport.demo("MenuBar in the scene (useSystemMenuBar = false)", menuBar);
        menuBar.setPrefWidth(320);
        controls.add(WindowSupport.demo("ContextMenu", contextTarget), 0, 1, 2, 1);
        controls.add(WindowSupport.demo("Tooltip", tooltipTarget), 2, 1);
        controls.add(menuBarDemo, 3, 1, 3, 1);

        HBox gallery = new HBox(9);
        Map<String, ImageView> views = new LinkedHashMap<>();
        for (String key : GALLERY) {
            ImageView view = new ImageView();
            view.setFitWidth(116);
            view.setFitHeight(124);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            views.put(key, view);
            StackPane frame = new StackPane(view);
            frame.getStyleClass().add("gallery-cell");
            frame.setPrefSize(120, 128);
            frame.setMinSize(120, 128);
            frame.setMaxSize(120, 128);
            frame.setAlignment(Pos.TOP_CENTER);
            gallery.getChildren().add(WindowSupport.demo(key, frame));
        }
        root.getProperties().put(GALLERY_KEY, views);
        root.getProperties().put(CONTROLS_KEY, new Controls(popupButton, comboBox, datePicker, colorPicker, choiceBox,
                menuButton, contextTarget, contextMenu, tooltipTarget, menuBar));

        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("new MenuBar().setUseSystemMenuBar(true)", "useSystemMenuBar=true", () -> {
            MenuBar systemBar = new MenuBar(new Menu("File"));
            systemBar.setUseSystemMenuBar(true);
            return systemBar.useSystemMenuBarProperty().getName() + "=" + systemBar.isUseSystemMenuBar();
        }));
        checks.add(Checks.expect("MenuBar -fx-use-system-menu-bar (CSS)", true, () -> {
            // in a scene without window : never becomes the system menu bar of a stage
            MenuBar cssBar = new MenuBar(new Menu("File"));
            cssBar.setStyle("-fx-use-system-menu-bar: true;");
            new Scene(new Group(cssBar));
            cssBar.applyCss();
            return cssBar.isUseSystemMenuBar();
        }));
        checks.add(Checks.run("Tooltip defaults", () -> {
            Tooltip tooltip = new Tooltip("x");
            return "showDelay " + millis(tooltip.getShowDelay()) + ", showDuration " + millis(tooltip.getShowDuration())
                    + ", hideDelay " + millis(tooltip.getHideDelay()) + ", autoHide " + tooltip.isAutoHide();
        }));

        VBox live = WindowSupport.liveSection(root, LIVE_TITLE, checks,
                "Shows each popup below its owner, captures its scene (gallery above), then hides it.", this::runLive);

        root.getChildren().addAll(controls, WindowSupport.caption("Captured popup windows (scaled)"), gallery, live);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return runLive(content);
    }

    @Override
    public CompletionStage<Map<String, Image>> extraSnapshots(Node content) {
        return WindowSupport.images(content);
    }

    @Override
    public void dispose(Node content) {
        WindowSupport.dispose(content);
        if (content.getProperties().get(CONTROLS_KEY) instanceof Controls c) {
            c.comboBox().hide();
            c.datePicker().hide();
            c.colorPicker().hide();
            c.choiceBox().hide();
            c.menuButton().hide();
            c.contextMenu().hide();
        }
    }

    private static String millis(Duration duration) {
        return WindowSupport.fmt(duration.toMillis()) + "ms";
    }

    // ------------------------------------------------------------------------------------------------------- content

    private static List<MenuItem> menuItems() {
        MenuItem newItem = new MenuItem("New", WindowSupport.icon('\uf15b', 12, "#4a5363"));
        newItem.setAccelerator(KeyCombination.keyCombination("Shortcut+N"));
        MenuItem open = new MenuItem("Open…", WindowSupport.icon('\uf07c', 12, "#4a5363"));
        open.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        CheckMenuItem grid = new CheckMenuItem("Show grid");
        grid.setSelected(true);
        ToggleGroup theme = new ToggleGroup();
        RadioMenuItem light = new RadioMenuItem("Light theme");
        light.setToggleGroup(theme);
        light.setSelected(true);
        RadioMenuItem dark = new RadioMenuItem("Dark theme");
        dark.setToggleGroup(theme);
        Menu export = new Menu("Export", null, new MenuItem("PNG"), new MenuItem("PDF"));
        MenuItem disabled = new MenuItem("Disabled item");
        disabled.setDisable(true);
        Slider zoom = new Slider(0, 100, 40);
        zoom.setPrefWidth(110);
        CustomMenuItem custom = new CustomMenuItem(new HBox(6, new Label("Zoom"), zoom), false);
        return List.of(newItem, open, new SeparatorMenuItem(), grid, light, dark, new SeparatorMenuItem(), export,
                disabled, custom);
    }

    private static ContextMenu contextMenu() {
        MenuItem cut = new MenuItem("Cut");
        cut.setAccelerator(KeyCombination.keyCombination("Shortcut+X"));
        MenuItem copy = new MenuItem("Copy");
        copy.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        MenuItem paste = new MenuItem("Paste");
        paste.setAccelerator(KeyCombination.keyCombination("Shortcut+V"));
        paste.setDisable(true);
        MenuItem selectAll = new MenuItem("Select all");
        return new ContextMenu(cut, copy, paste, new SeparatorMenuItem(), selectAll);
    }

    private static Tooltip tooltip() {
        Tooltip tooltip = new Tooltip("Tooltips are PopupControls: shown after showDelay, hidden after showDuration.");
        tooltip.setWrapText(true);
        tooltip.setPrefWidth(220);
        tooltip.setGraphic(WindowSupport.icon('\uf05a', 16, "#8ec5ff"));
        return tooltip;
    }

    private static Popup popup(boolean autoHide) {
        Label title = new Label("Custom Popup");
        title.getStyleClass().add("popup-title");
        Label text = new Label("Any node graph in a PopupWindow, owned by the main window.");
        text.setWrapText(true);
        text.setPrefWidth(200);
        Slider slider = new Slider(0, 100, 65);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        Button apply = new Button("Apply");
        apply.setDefaultButton(true);
        Button dismiss = new Button("Dismiss");
        HBox buttons = new HBox(8, apply, dismiss);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        VBox box = new VBox(title, text, slider, buttons);
        box.getStyleClass().add("custom-popup");
        StackPane shadow = WindowSupport.styled(new StackPane(box));
        shadow.getStyleClass().add("custom-popup-shadow");

        Popup popup = new Popup();
        popup.getContent().add(shadow);
        popup.setAutoHide(autoHide);
        // the anchor is the top left corner of the content (not of the shadow), styled before showing so that the
        // window does not move when CSS adds the effect
        popup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
        WindowSupport.freezeAxes(shadow);
        dismiss.setOnAction(e -> popup.hide());
        apply.setOnAction(e -> popup.hide());
        return popup;
    }

    private static MenuBar embeddedMenuBar() {
        Menu file = new Menu("File", null, new MenuItem("New"), new MenuItem("Open…"), new SeparatorMenuItem(),
                new MenuItem("Quit"));
        Menu edit = new Menu("Edit", null, new MenuItem("Undo"), new MenuItem("Redo"));
        Menu view = new Menu("View", null, new CheckMenuItem("Status bar"));
        Menu help = new Menu("Help", null, new MenuItem("About"));
        MenuBar menuBar = new MenuBar(file, edit, view, help);
        menuBar.setUseSystemMenuBar(false);
        return menuBar;
    }

    // -------------------------------------------------------------------------------------------------- live checks

    /**
     * One popup to capture : how to show it, how to find its window, how to describe it, how to hide it.
     */
    private record Target(String key, String name, Runnable show, Predicate<Window> kind,
            Function<PopupWindow, String> describe, Runnable hide) {
    }

    private CompletionStage<Void> runLive(Node content) {
        Stage main = WindowSupport.mainStage(content);
        WindowSupport.Live live = WindowSupport.startLive(content);
        if (main == null || !(content.getProperties().get(CONTROLS_KEY) instanceof Controls c)) {
            live.checks.add(Check.fail("main window", "page not showing"));
            WindowSupport.publish(content, live);
            return CompletableFuture.completedFuture(null);
        }
        boolean mainFocused = main.isFocused();

        Popup popup = popup(false);
        Tooltip tooltip = tooltip();
        // Tooltip durations are styleable : parsed by the Duration converter, applied once the tooltip is shown
        tooltip.setStyle("-fx-show-delay: 250ms; -fx-show-duration: 7s; -fx-hide-delay: 0.05s;");
        List<Target> targets = List.of(
                new Target("popup", "Popup", () -> {
                    Bounds b = screenBounds(c.popupButton());
                    popup.show(c.popupButton(), b.getMinX(), b.getMaxY() + 4);
                }, w -> w == popup, w -> anchor(w, c.popupButton()) + ", " + popup.getContent().size()
                        + " content node, autoHide " + popup.isAutoHide(), popup::hide),
                new Target("combo-box", "ComboBox popup", c.comboBox()::show, PopupControl.class::isInstance,
                        w -> anchor(w, c.comboBox()) + ", " + filledCells(w) + " cells for "
                                + c.comboBox().getItems().size() + " items, selected " + selectedCell(w),
                        c.comboBox()::hide),
                new Target("date-picker", "DatePicker popup", c.datePicker()::show, PopupControl.class::isInstance,
                        w -> anchor(w, c.datePicker()) + ", " + text(w, ".month-year-pane .spinner-label") + " "
                                + lookupAll(w, ".month-year-pane .spinner-label").stream().skip(1).findFirst()
                                        .map(n -> ((Labeled) n).getText()).orElse("?")
                                + ", " + lookupCount(w, ".day-cell") + " days, " + lookupCount(w, ".week-number-cell")
                                + " week numbers, selected " + text(w, ".day-cell.selected"),
                        c.datePicker()::hide),
                new Target("color-picker", "ColorPicker popup", c.colorPicker()::show, PopupControl.class::isInstance,
                        w -> anchor(w, c.colorPicker()) + ", " + lookupCount(w, ".color-square") + " color squares, link "
                                + text(w, ".hyperlink"),
                        c.colorPicker()::hide),
                new Target("choice-box", "ChoiceBox popup", c.choiceBox()::show, ContextMenu.class::isInstance,
                        w -> anchor(w, c.choiceBox()) + ", " + ((ContextMenu) w).getItems().size() + " items, "
                                + ((ContextMenu) w).getItems().stream()
                                        .filter(i -> i instanceof RadioMenuItem r && r.isSelected())
                                        .map(MenuItem::getText).findFirst().orElse("none")
                                + " checked",
                        c.choiceBox()::hide),
                new Target("menu-button", "MenuButton popup", c.menuButton()::show, ContextMenu.class::isInstance,
                        w -> anchor(w, c.menuButton()) + ", " + ((ContextMenu) w).getItems().size() + " items, "
                                + lookupCount(w, ".accelerator-text") + " accelerators, side "
                                + c.menuButton().getPopupSide(),
                        c.menuButton()::hide),
                new Target("context-menu", "ContextMenu", () -> c.contextMenu().show(c.contextTarget(), Side.BOTTOM, 0, 4),
                        w -> w == c.contextMenu(), w -> anchor(w, c.contextTarget()) + ", "
                                + c.contextMenu().getItems().size() + " items, "
                                + c.contextMenu().getItems().stream().filter(MenuItem::isDisable).count() + " disabled",
                        c.contextMenu()::hide),
                new Target("tooltip", "Tooltip", () -> {
                    Bounds b = screenBounds(c.tooltipTarget());
                    tooltip.show(c.tooltipTarget(), b.getMinX(), b.getMaxY() + 6);
                }, w -> w == tooltip, w -> anchor(w, c.tooltipTarget()) + ", wrap " + tooltip.isWrapText()
                        + ", graphic " + (tooltip.getGraphic() != null), tooltip::hide));

        List<PopupWindow> shown = new ArrayList<>();
        // ready() is invoked right after build(): let the page get its skins and layout first
        CompletionStage<Void> chain = Fx.pulses(3);
        for (Target target : targets) {
            chain = live.step(chain, target.name(), v -> capture(live, target, main, shown));
        }
        return chain.thenRun(() -> {
            long showing = shown.stream().filter(Window::isShowing).count();
            live.checks.add(Check.of("All popups hidden", showing == 0 && shown.size() == targets.size(),
                    showing + " of " + shown.size() + " captured popups still showing"));
            live.checks.add(Checks.expect("Tooltip durations from CSS", "showDelay 250ms, showDuration 7000ms, "
                    + "hideDelay 50ms", () -> "showDelay " + millis(tooltip.getShowDelay()) + ", showDuration "
                            + millis(tooltip.getShowDuration()) + ", hideDelay " + millis(tooltip.getHideDelay())));
            live.checks.add(mainMenuBarCheck(main));
            live.checks.add(Checks.run("MenuBar in the scene", () -> "useSystemMenuBar=" + c.menuBar().isUseSystemMenuBar()
                    + ", " + c.menuBar().lookupAll(".menu").stream().filter(Labeled.class::isInstance)
                            .map(n -> ((Labeled) n).getText()).toList() + " menu buttons"));
            live.checks.add(cssIconCheck(c.tooltipTarget()));
            live.closeAll();
            if (mainFocused && !main.isFocused()) {
                main.requestFocus();
            }
            showGallery(content, live);
            WindowSupport.publish(content, live);
        });
    }

    private static CompletionStage<Void> capture(WindowSupport.Live live, Target target, Stage main,
            List<PopupWindow> shown) {
        Set<Window> before = WindowSupport.showingWindows();
        target.show().run();
        Window found = WindowSupport.newWindow(before, target.kind());
        if (!(found instanceof PopupWindow window)) {
            target.hide().run();
            throw new IllegalStateException("no popup window appeared");
        }
        WindowSupport.shield(window);
        shown.add(window);
        live.opened.add(window);
        boolean showing = window.isShowing();
        boolean owned = window.getOwnerWindow() == main;
        return Fx.pulses(3).thenCompose(v -> {
            WindowSupport.stabilize(window.getScene());
            return Fx.pulses(2);
        }).thenAccept(v -> {
            try {
                live.images.put(target.key(), WindowSupport.capture(window.getScene()));
                String description = target.describe().apply(window);
                live.checks.add(Check.of(target.name(), showing && owned,
                        (showing ? "showing" : "NOT showing") + ", " + (owned ? "owned by main" : "not owned") + ", "
                                + description));
            } finally {
                target.hide().run();
            }
        });
    }

    /**
     * The MenuBar of the main window asks for the system menu bar. Only macOS has one : there the menus move to the
     * screen menu bar and no menu button is left in the scene, elsewhere (Windows, Linux) the property is ignored and
     * the MenuBar shows one button per menu in the scene.
     */
    private static Check mainMenuBarCheck(Stage main) {
        String name = "Main window MenuBar";
        try {
            MenuBar bar = main.getScene().getRoot().lookupAll(".menu-bar").stream()
                    .filter(n -> n instanceof MenuBar m && m.isUseSystemMenuBar()).map(MenuBar.class::cast)
                    .findFirst().orElseThrow(() -> new IllegalStateException("no MenuBar with useSystemMenuBar"));
            int buttons = bar.lookupAll(".menu").size();
            int expected = Platforms.isMac() ? 0 : bar.getMenus().size();
            String value = "useSystemMenuBar=" + bar.isUseSystemMenuBar() + ", menus "
                    + bar.getMenus().stream().map(Menu::getText).toList() + ", " + buttons
                    + " menu buttons left in the scene" + (Platforms.isMac() ? "" : " (no system menu bar)");
            return Check.of(name, buttons == expected,
                    buttons == expected ? value : value + ", expected " + expected);
        } catch (Throwable t) {
            return Check.fail(name, Checks.describe(t));
        }
    }

    /**
     * The -fx-graphic of .css-icon (windows.css) : an url relative to the stylesheet, resolved against its URL.
     */
    private static Check cssIconCheck(Labeled labeled) {
        return Checks.run("CSS url(../images/icon-16.png)", () -> {
            if (!(labeled.getGraphic() instanceof ImageView view) || view.getImage() == null) {
                throw new IllegalStateException("no image graphic: " + labeled.getGraphic());
            }
            Image image = view.getImage();
            String url = image.getUrl();
            int index = url == null ? -1 : url.lastIndexOf("/showcase/");
            if (index < 0) {
                throw new IllegalStateException("url outside /showcase/");
            }
            // the scheme and the location of the application differ between runs, not the resource path
            return url.substring(index) + " " + WindowSupport.size(image.getWidth(), image.getHeight())
                    + (image.isError() ? " ERROR " + Checks.describe(image.getException()) : "");
        });
    }

    private static void showGallery(Node content, WindowSupport.Live live) {
        if (content.getProperties().get(GALLERY_KEY) instanceof Map<?, ?> views) {
            live.images.forEach((key, image) -> {
                if (views.get(key) instanceof ImageView view) {
                    view.setImage(image);
                }
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------ helpers

    private static Bounds screenBounds(Node node) {
        return node.localToScreen(node.getBoundsInLocal());
    }

    /**
     * Position of the popup anchor relative to the bottom left corner of its owner node, and size of the popup content.
     * <p>
     * Not the window size : it is updated when the native window reports its new frame, rounded to whole points on
     * macOS (a 220.5 high content gives a 221 high window, some time after show()), so it depends on timing.
     */
    private static String anchor(Window window, Node owner) {
        Bounds b = screenBounds(owner);
        PopupWindow popup = (PopupWindow) window;
        Bounds content = content(window).getLayoutBounds();
        return "anchor " + signed(popup.getAnchorX() - b.getMinX()) + "," + signed(popup.getAnchorY() - b.getMaxY())
                + " " + WindowSupport.size(content.getWidth(), content.getHeight());
    }

    /**
     * The content node of a popup window : the skin node of a PopupControl (ListView, DatePickerContent,
     * ContextMenuContent...), the first content node of a Popup.
     */
    private static Node content(Window window) {
        if (window instanceof PopupControl control && control.getSkin() != null && control.getSkin().getNode() != null) {
            return control.getSkin().getNode();
        }
        if (window instanceof Popup popup && !popup.getContent().isEmpty()) {
            return popup.getContent().get(0);
        }
        return window.getScene().getRoot();
    }

    private static String signed(double value) {
        return (value >= 0 ? "+" : "") + WindowSupport.fmt(value);
    }

    private static long filledCells(Window window) {
        return window.getScene().getRoot().lookupAll(".list-cell").stream()
                .filter(n -> n instanceof ListCell<?> cell && !cell.isEmpty() && cell.isVisible()).count();
    }

    private static String selectedCell(Window window) {
        return window.getScene().getRoot().lookupAll(".list-cell").stream()
                .filter(n -> n instanceof ListCell<?> cell && !cell.isEmpty() && cell.isSelected())
                .map(n -> ((ListCell<?>) n).getText()).findFirst().orElse("none");
    }

    private static List<Node> lookupAll(Window window, String selector) {
        return new ArrayList<>(window.getScene().getRoot().lookupAll(selector));
    }

    private static int lookupCount(Window window, String selector) {
        return (int) window.getScene().getRoot().lookupAll(selector).stream().filter(Node::isVisible).count();
    }

    private static String text(Window window, String selector) {
        Node node = window.getScene().getRoot().lookup(selector);
        return node instanceof Labeled labeled ? labeled.getText() : "?";
    }
}
