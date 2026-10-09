package io.quarkiverse.fx.showcase.pages.controls;

import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.caption;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.demo;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.grow;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.glyph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

@Singleton
public class MenusToolbarPage implements FeaturePage {

    private static final String CONTEXT_MENU = "controls.contextMenu";
    private static final String TOOLTIP = "controls.tooltip";
    private static final String VIEW_MENU = "controls.viewMenu";

    @Override
    public String id() {
        return "controls-menus-toolbar";
    }

    @Override
    public String title() {
        return "Menus, ToolBar & Popups";
    }

    @Override
    public String category() {
        return Categories.CONTROLS;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Node build() {
        ControlsUi.ChecksHolder checks = new ControlsUi.ChecksHolder();
        List<Check> early = checks.early;
        Label status = new Label("Last action : none");
        status.getStyleClass().add("status-label");

        // Inline MenuBar
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);
        Menu file = new Menu("File");
        MenuItem newItem = item("New", "Shortcut+N", ControlsUi.HOME, status);
        MenuItem open = item("Open...", "Shortcut+O", ControlsUi.FOLDER, status);
        Menu recent = new Menu("Open Recent", null, new MenuItem("showcase.fxml"), new MenuItem("styles.css"),
                new SeparatorMenuItem(), new MenuItem("Clear list"));
        MenuItem save = item("Save", "Shortcut+S", ControlsUi.SAVE, status);
        MenuItem saveAs = item("Save As...", "Shortcut+Shift+S", null, status);
        MenuItem print = item("Print", "Shortcut+P", ControlsUi.PRINT, status);
        print.setDisable(true);
        file.getItems().addAll(newItem, open, recent, new SeparatorMenuItem(), save, saveAs, new SeparatorMenuItem(),
                print);
        Menu edit = new Menu("Edit", glyph(ControlsUi.CUT, 11));
        edit.getItems().addAll(item("Undo", "Shortcut+Z", ControlsUi.UNDO, status),
                item("Redo", "Shortcut+Shift+Z", ControlsUi.REDO, status), new SeparatorMenuItem(),
                item("Cut", "Shortcut+X", ControlsUi.CUT, status), item("Copy", "Shortcut+C", ControlsUi.COPY, status),
                item("Paste", "Shortcut+V", ControlsUi.PASTE, status));
        Menu view = viewMenu(status);
        Menu window = new Menu("Window");
        window.setDisable(true);
        Menu help = new Menu("Help", null, item("About", "F1", ControlsUi.INFO, status));
        menuBar.getMenus().addAll(file, edit, view, window, help);
        menuBar.setMinWidth(0);

        // ToolBars
        ToolBar toolBar = new ToolBar(
                toolButton(ControlsUi.HOME, "Home"), toolButton(ControlsUi.FOLDER, null), toolButton(ControlsUi.SAVE, null),
                new Separator(),
                toolButton(ControlsUi.CUT, null), toolButton(ControlsUi.COPY, null), toolButton(ControlsUi.PASTE, null),
                new Separator(),
                toolToggle(ControlsUi.BOLD, true), toolToggle(ControlsUi.ITALIC, false),
                toolToggle(ControlsUi.UNDERLINE, false),
                new Separator(),
                sizeCombo(), searchField());
        ToolBar overflow = new ToolBar();
        for (String code : List.of(ControlsUi.HOME, ControlsUi.FOLDER, ControlsUi.SAVE, ControlsUi.CUT, ControlsUi.COPY,
                ControlsUi.PASTE, ControlsUi.UNDO, ControlsUi.REDO, ControlsUi.PRINT, ControlsUi.STAR, ControlsUi.USER,
                ControlsUi.BELL, ControlsUi.HEART, ControlsUi.COG, ControlsUi.TRASH)) {
            overflow.getItems().add(toolButton(code, null));
        }
        overflow.setPrefWidth(300);
        overflow.setMaxWidth(300);
        overflow.setId("overflow-toolbar");
        ToolBar vertical = new ToolBar(toolButton(ControlsUi.HOME, null), toolButton(ControlsUi.SEARCH, null),
                new Separator(), toolButton(ControlsUi.STAR, null), toolButton(ControlsUi.COG, null));
        vertical.setOrientation(Orientation.VERTICAL);
        early.add(Checks.expect("ToolBar item count (main / overflow)", "14 / 15",
                () -> toolBar.getItems().size() + " / " + overflow.getItems().size()));

        // Context menu target and tooltip owner
        Label target = new Label("ContextMenu target : the menu is shown programmatically and captured as an extra "
                + "snapshot (right-click here when running interactively)");
        target.setWrapText(true);
        target.getStyleClass().add("context-target");
        target.setId("context-target");
        target.setMaxWidth(Double.MAX_VALUE);
        ContextMenu contextMenu = contextMenu(status);
        target.setContextMenu(contextMenu);
        Button tooltipOwner = new Button("Tooltip owner", glyph(ControlsUi.INFO, 12, Color.web("#1565c0")));
        tooltipOwner.setId("tooltip-owner");
        Tooltip tooltip = new Tooltip("Tooltip shown programmatically with show(owner, x, y),\n"
                + "with a graphic and two lines of text.");
        tooltip.setGraphic(new javafx.scene.image.ImageView(
                new Image(Fx.resourceUrl("/showcase/images/icon-16.png"))));
        Tooltip.install(tooltipOwner, tooltip);
        Button wrapTooltipOwner = new Button("Styled tooltip");
        Tooltip styledTooltip = new Tooltip("Styled with -fx-background-color and -fx-font-size");
        styledTooltip.setStyle("-fx-background-color: #263238; -fx-text-fill: #ffecb3; -fx-font-size: 13px;");
        wrapTooltipOwner.setTooltip(styledTooltip);

        // Menu model, as rendered by the menus
        GridPane model = new GridPane(10, 2);
        model.addRow(0, caption("Menu"), caption("Item"), caption("Kind"), caption("Accelerator display text"));
        int row = 1;
        for (Menu menu : menuBar.getMenus()) {
            for (MenuItem menuItem : menu.getItems()) {
                if (menuItem instanceof SeparatorMenuItem) {
                    continue;
                }
                model.addRow(row++, new Label(menu.getText()), new Label(menuItem.getText() == null ? "(custom)"
                        : menuItem.getText()), new Label(kind(menuItem)),
                        new Label(menuItem.getAccelerator() == null ? "" : menuItem.getAccelerator().getDisplayText()));
            }
        }

        // Checks
        early.add(Checks.run("display Shortcut+N", () -> KeyCombination.keyCombination("Shortcut+N").getDisplayText()));
        early.add(Checks.run("display Shortcut+Shift+S",
                () -> KeyCombination.keyCombination("Shortcut+Shift+S").getDisplayText()));
        early.add(Checks.run("display Ctrl+Alt+Delete",
                () -> KeyCombination.keyCombination("Ctrl+Alt+Delete").getDisplayText()));
        early.add(Checks.run("display Alt+F4", () -> KeyCombination.keyCombination("Alt+F4").getDisplayText()));
        early.add(Checks.run("display Meta+Enter", () -> new KeyCodeCombination(KeyCode.ENTER,
                KeyCombination.META_DOWN).getDisplayText()));
        early.add(Checks.run("display KeyCharacterCombination '+'",
                () -> new KeyCharacterCombination("+", KeyCombination.SHORTCUT_DOWN).getDisplayText()));
        early.add(Checks.expect("KeyCombination.valueOf(\"shift+shortcut+z\").getName()", "Shift+Shortcut+Z",
                () -> KeyCombination.valueOf("shift+shortcut+z").getName()));
        early.add(Checks.run("Shortcut+N matches Meta/Ctrl+N events", () -> {
            KeyCombination combination = KeyCombination.keyCombination("Shortcut+N");
            KeyEvent meta = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.N, false, false, false, true);
            KeyEvent ctrl = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.N, false, true, false, false);
            return "meta=" + combination.match(meta) + ", ctrl=" + combination.match(ctrl);
        }));
        early.add(Checks.expect("CheckMenuItem / RadioMenuItem state", "Toolbar:true Status bar:false / 100%",
                () -> {
                    List<String> states = new ArrayList<>();
                    RadioMenuItem selectedZoom = null;
                    for (MenuItem mi : view.getItems()) {
                        if (mi instanceof CheckMenuItem check) {
                            states.add(check.getText() + ":" + check.isSelected());
                        } else if (mi instanceof RadioMenuItem radio && radio.isSelected()) {
                            selectedZoom = radio;
                        }
                    }
                    return String.join(" ", states) + " / " + (selectedZoom == null ? "none" : selectedZoom.getText());
                }));
        early.add(Checks.expect("MenuItem.fire runs onAction", "Last action : Save", () -> {
            save.fire();
            String text = status.getText();
            status.setText("Last action : none");
            return text;
        }));

        VBox menus = demo("MenuBar (inline, useSystemMenuBar=false) : menu graphic, disabled menu", menuBar);
        VBox bars = demo("ToolBar : buttons, separators, toggles, combo, text field", toolBar,
                caption("ToolBar with 15 buttons in 300px : overflow button"), overflow);
        HBox verticalRow = new HBox(8, demo("Vertical", vertical),
                grow(demo("ContextMenu and Tooltips (see the extra snapshots)", target,
                        new HBox(8, tooltipOwner, wrapTooltipOwner), status)));
        VBox styledBars = demo("Styled with CSS : dark ToolBar, colored MenuBar, ToolBar with a MenuButton",
                styledToolBar(), styledMenuBar(status), menuButtonToolBar(status));
        VBox left = new VBox(8, menus, bars, verticalRow, styledBars);
        left.setPrefWidth(560);
        left.setMinWidth(560);
        left.setMaxWidth(560);
        VBox right = new VBox(8, demo("Menu model : kind and KeyCombination.getDisplayText()", model), checks);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox body = new HBox(10, left, right);

        Pane hidden = ControlsUi.hiddenChecks();
        VBox root = ControlsUi.page(8, body, hidden);
        root.setPrefWidth(1028);
        root.getProperties().put(CONTEXT_MENU, contextMenu);
        root.getProperties().put(TOOLTIP, tooltip);
        root.getProperties().put(VIEW_MENU, view);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Fx.pulses(2).thenRunAsync(() -> {
            ControlsUi.ChecksHolder holder = ControlsUi.find(content, ControlsUi.ChecksHolder.class);
            List<Check> late = new ArrayList<>();
            late.add(Checks.expect("ToolBar overflow button visible", true, () -> {
                Node button = content.lookup("#overflow-toolbar").lookup(".tool-bar-overflow-button");
                return button != null && button.isVisible();
            }));
            // MenuBar and Control context menu accelerators are installed into the scene
            late.add(Checks.expect("scene accelerators (MenuBar / ContextMenu)",
                    "true / true", () -> {
                        var accelerators = content.getScene().getAccelerators();
                        return accelerators.containsKey(KeyCombination.keyCombination("Shortcut+N")) + " / "
                                + accelerators.containsKey(KeyCombination.keyCombination("Shortcut+BACK_SPACE"));
                    }));
            late.add(Checks.expect("run the Shortcut+Shift+S accelerator", "Last action : Save As...", () -> {
                Runnable action = content.getScene().getAccelerators()
                        .get(KeyCombination.keyCombination("Shortcut+Shift+S"));
                action.run();
                return ((Label) content.lookup(".status-label")).getText();
            }));
            holder.show("Checks", late);
        }, Fx.FX_THREAD);
    }

    @Override
    public CompletionStage<Map<String, Image>> extraSnapshots(Node content) {
        ContextMenu contextMenu = (ContextMenu) content.getProperties().get(CONTEXT_MENU);
        Tooltip tooltip = (Tooltip) content.getProperties().get(TOOLTIP);
        Menu viewMenu = (Menu) content.getProperties().get(VIEW_MENU);
        Node target = content.lookup("#context-target");
        Node tooltipOwner = content.lookup("#tooltip-owner");
        Scene scene = content.getScene();
        Node previousFocus = scene.getFocusOwner();
        Map<String, Image> images = new LinkedHashMap<>();
        List<Check> popupChecks = new ArrayList<>();

        // Popups ignore the mouse (no hover effect whatever the pointer position), and their first item is not
        // focused : a popup is focused only while its owner window is, so a focused item would depend on the
        // active application
        ListChangeListener<Window> noHover = change -> {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) {
                    if (window instanceof PopupWindow && window.getScene() != null) {
                        window.getScene().getRoot().setMouseTransparent(true);
                        window.getScene().getRoot().requestFocus();
                    }
                }
            }
        };
        Window.getWindows().addListener(noHover);

        CompletionStage<Map<String, Image>> result = CompletableFuture.completedFuture((Void) null)
                .thenRunAsync(() -> contextMenu.show(target, Side.BOTTOM, 0, 4), Fx.FX_THREAD)
                .thenCompose(v -> Fx.pulses(3))
                .thenRunAsync(() -> {
                    Parent root = contextMenu.getScene().getRoot();
                    images.put("context-menu", root.snapshot(null, null));
                    popupChecks.add(Checks.run("ContextMenu rendered items", () -> root.lookupAll(".menu-item").size()));
                    popupChecks.add(Checks.run("ContextMenu accelerator texts", () -> texts(root, ".accelerator-text")));
                    contextMenu.hide();
                    Point2D anchor = tooltipOwner.localToScreen(0, tooltipOwner.getLayoutBounds().getHeight() + 6);
                    tooltip.show(tooltipOwner, anchor.getX(), anchor.getY());
                }, Fx.FX_THREAD)
                .thenCompose(v -> Fx.pulses(3))
                .thenRunAsync(() -> {
                    Parent root = tooltip.getScene().getRoot();
                    images.put("tooltip", root.snapshot(null, null));
                    popupChecks.add(Checks.run("Tooltip rendered text", () -> texts(root, ".tooltip")));
                    tooltip.hide();
                    viewMenu.show();
                }, Fx.FX_THREAD)
                .thenCompose(v -> Fx.pulses(3))
                .thenApplyAsync(v -> {
                    ContextMenu popup = Window.getWindows().stream()
                            .filter(w -> w instanceof ContextMenu && w.isShowing())
                            .map(ContextMenu.class::cast)
                            // the popup of the menu bar button holds the items of the View menu
                            .filter(cm -> cm.getItems().contains(viewMenu.getItems().getFirst()))
                            .findFirst().orElse(null);
                    popupChecks.add(Checks.expect("MenuBar menu popup shown", true, () -> popup != null));
                    if (popup != null) {
                        Parent root = popup.getScene().getRoot();
                        images.put("menubar-view-menu", root.snapshot(null, null));
                        popupChecks.add(Checks.run("View menu rendered labels", () -> texts(root, ".menu-item .label")));
                    }
                    return images;
                }, Fx.FX_THREAD);

        return result.whenCompleteAsync((images2, error) -> {
            viewMenu.hide();
            contextMenu.hide();
            tooltip.hide();
            Window.getWindows().removeListener(noHover);
            if (previousFocus != null && previousFocus.getScene() == scene) {
                previousFocus.requestFocus();
            }
            Pane hidden = ControlsUi.find(content, Pane.class, "hidden-checks");
            if (hidden != null) {
                Checks.attach(hidden, popupChecks);
            }
        }, Fx.FX_THREAD);
    }

    @Override
    public void dispose(Node content) {
        ((ContextMenu) content.getProperties().get(CONTEXT_MENU)).hide();
        ((Tooltip) content.getProperties().get(TOOLTIP)).hide();
        ((Menu) content.getProperties().get(VIEW_MENU)).hide();
    }

    private static ToolBar styledToolBar() {
        ToolBar bar = new ToolBar();
        bar.setStyle("-fx-background-color: linear-gradient(to bottom, #37474f, #263238); -fx-padding: 6;");
        for (String code : List.of(ControlsUi.HOME, ControlsUi.SEARCH, ControlsUi.STAR, ControlsUi.BELL,
                ControlsUi.USER)) {
            Button button = new Button(null, glyph(code, 14, Color.web("#eceff1")));
            button.setStyle("-fx-background-color: transparent; -fx-padding: 4 8 4 8;");
            bar.getItems().add(button);
        }
        Separator separator = new Separator(Orientation.VERTICAL);
        Label title = new Label("Dark toolbar");
        title.setStyle("-fx-text-fill: #ffcc80; -fx-font-weight: bold;");
        bar.getItems().addAll(separator, title);
        return bar;
    }

    private static MenuBar styledMenuBar(Label status) {
        MenuBar bar = new MenuBar();
        bar.setUseSystemMenuBar(false);
        bar.setStyle("-fx-base: #1565c0; -fx-background-color: #1976d2;");
        Menu project = new Menu("Project", glyph(ControlsUi.FOLDER, 11, Color.WHITE),
                item("Build", "F9", null, status), item("Run", "Shortcut+R", null, status));
        Menu tools = new Menu("Tools", null, item("Options", null, ControlsUi.COG, status));
        Menu account = new Menu("Account", glyph(ControlsUi.USER, 11, Color.WHITE),
                item("Sign out", null, null, status));
        bar.getMenus().addAll(project, tools, account);
        return bar;
    }

    private static ToolBar menuButtonToolBar(Label status) {
        javafx.scene.control.MenuButton insert = new javafx.scene.control.MenuButton("Insert",
                glyph(ControlsUi.STAR, 11), item("Image", null, null, status), item("Table", null, null, status));
        javafx.scene.control.SplitMenuButton export = new javafx.scene.control.SplitMenuButton(
                item("Export as PDF", null, null, status), item("Export as PNG", null, null, status));
        export.setText("Export");
        ToggleButton pin = new ToggleButton("Pinned", glyph(ControlsUi.HEART, 11, Color.web("#d81b60")));
        pin.setSelected(true);
        Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMaxWidth(Double.MAX_VALUE);
        return new ToolBar(insert, export, new Separator(), pin, spacer, new Label("ToolBar with MenuButtons"));
    }

    private static String texts(Parent root, String selector) {
        return root.lookupAll(selector).stream()
                .filter(n -> n instanceof Labeled || n instanceof javafx.scene.text.Text)
                .map(n -> n instanceof Labeled l ? l.getText() : ((javafx.scene.text.Text) n).getText())
                .filter(t -> t != null && !t.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private static Menu viewMenu(Label status) {
        CheckMenuItem showToolbar = new CheckMenuItem("Toolbar");
        showToolbar.setSelected(true);
        CheckMenuItem showStatus = new CheckMenuItem("Status bar");
        ToggleGroup zoom = new ToggleGroup();
        RadioMenuItem zoom100 = radio("100%", zoom, "Shortcut+0");
        RadioMenuItem zoom150 = radio("150%", zoom, null);
        RadioMenuItem zoom200 = radio("200%", zoom, null);
        zoom100.setSelected(true);
        Slider slider = new Slider(0, 100, 65);
        slider.setPrefWidth(110);
        Label opacity = new Label("Opacity");
        // the popup of a MenuBar menu is styled as a descendant of the showing menu button (white label text)
        opacity.setStyle("-fx-text-fill: #37474f;");
        HBox custom = new HBox(8, opacity, slider);
        custom.setAlignment(Pos.CENTER_LEFT);
        CustomMenuItem customItem = new CustomMenuItem(custom, false);
        Menu appearance = new Menu("Appearance", null, new MenuItem("Light"), new MenuItem("Dark"));
        // the full screen convention of each platform : Cmd+Ctrl+F on macOS (elsewhere Shortcut+Ctrl+F is only Ctrl+F,
        // the usual Find shortcut), F11 on Windows and Linux
        MenuItem fullScreen = item("Full screen", Platforms.pick("Shortcut+Ctrl+F", "F11", "F11"), null, status);
        return new Menu("View", null, showToolbar, showStatus, new SeparatorMenuItem(), zoom100, zoom150, zoom200,
                new SeparatorMenuItem(), customItem, appearance, fullScreen);
    }

    private static ContextMenu contextMenu(Label status) {
        MenuItem cut = item("Cut", "Shortcut+X", ControlsUi.CUT, status);
        MenuItem copy = item("Copy", "Shortcut+C", ControlsUi.COPY, status);
        MenuItem paste = item("Paste", "Shortcut+V", ControlsUi.PASTE, status);
        paste.setDisable(true);
        CheckMenuItem wrap = new CheckMenuItem("Word wrap");
        wrap.setSelected(true);
        CheckMenuItem spell = new CheckMenuItem("Spell check");
        ToggleGroup align = new ToggleGroup();
        RadioMenuItem left = radio("Align left", align, null);
        RadioMenuItem center = radio("Align center", align, null);
        center.setSelected(true);
        Menu more = new Menu("More", glyph(ControlsUi.COG, 11), new MenuItem("Properties"), new MenuItem("Inspect"));
        Slider size = new Slider(8, 32, 14);
        size.setPrefWidth(110);
        HBox custom = new HBox(8, new Label("Size"), size);
        custom.setAlignment(Pos.CENTER_LEFT);
        CustomMenuItem customItem = new CustomMenuItem(custom, false);
        MenuItem delete = item("Delete", "Shortcut+BACK_SPACE", ControlsUi.TRASH, status);
        delete.setStyle("-fx-text-fill: #c62828;");
        return new ContextMenu(cut, copy, paste, new SeparatorMenuItem(), wrap, spell, new SeparatorMenuItem(), left,
                center, new SeparatorMenuItem(), customItem, more, new SeparatorMenuItem(), delete);
    }

    private static MenuItem item(String text, String accelerator, String glyphCode, Label status) {
        MenuItem item = new MenuItem(text, glyphCode == null ? null : glyph(glyphCode, 11));
        if (accelerator != null) {
            item.setAccelerator(KeyCombination.keyCombination(accelerator));
        }
        item.setOnAction(e -> status.setText("Last action : " + text));
        return item;
    }

    private static RadioMenuItem radio(String text, ToggleGroup group, String accelerator) {
        RadioMenuItem item = new RadioMenuItem(text);
        item.setToggleGroup(group);
        if (accelerator != null) {
            item.setAccelerator(KeyCombination.keyCombination(accelerator));
        }
        return item;
    }

    private static String kind(MenuItem item) {
        return item.getClass().getSimpleName();
    }

    private static Button toolButton(String code, String text) {
        Button button = new Button(text, glyph(code, 13));
        return button;
    }

    private static ToggleButton toolToggle(String code, boolean selected) {
        ToggleButton toggle = new ToggleButton(null, glyph(code, 12));
        toggle.setSelected(selected);
        return toggle;
    }

    private static ComboBox<String> sizeCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("10", "12", "14", "18", "24");
        combo.setValue("14");
        combo.setPrefWidth(70);
        return combo;
    }

    private static TextField searchField() {
        TextField field = new TextField();
        field.setPromptText("Search");
        field.setPrefColumnCount(7);
        return field;
    }

}
