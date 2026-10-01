package io.quarkiverse.fx.showcase.pages.data;

import static io.quarkiverse.fx.showcase.pages.data.DataUi.check;
import static io.quarkiverse.fx.showcase.pages.data.DataUi.demo;
import static io.quarkiverse.fx.showcase.pages.data.DataUi.fmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

@Singleton
public class ContainersPage implements FeaturePage {

    @Override
    public String id() {
        return "data-containers";
    }

    @Override
    public String title() {
        return "Containers";
    }

    @Override
    public String category() {
        return Categories.DATA;
    }

    @Override
    public int order() {
        return 40;
    }

    private static Node dot(String color) {
        return new Circle(5, Color.web(color));
    }

    private static Node square(String color) {
        return new Rectangle(10, 10, Color.web(color));
    }

    private static Node arrow(String color) {
        Polygon arrow = new Polygon(0, 0, 9, 5, 0, 10);
        arrow.setFill(Color.web(color));
        return arrow;
    }

    private static Node star(String color) {
        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double radius = i % 2 == 0 ? 6 : 2.6;
            double angle = Math.PI / 5 * i - Math.PI / 2;
            star.getPoints().addAll(radius * Math.cos(angle), radius * Math.sin(angle));
        }
        star.setFill(Color.web(color));
        return star;
    }

    private static StackPane colored(String styleClass, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("pane-label");
        StackPane pane = new StackPane(label);
        pane.getStyleClass().add(styleClass);
        pane.setMinSize(30, 30);
        return pane;
    }

    private static VBox tabBody(String title, Node... nodes) {
        Label label = new Label(title);
        label.getStyleClass().add("pane-label");
        VBox box = new VBox(label);
        box.getChildren().addAll(nodes);
        box.getStyleClass().add("tab-body");
        return box;
    }

    private static Tab tab(String text, Node graphic, Node content) {
        Tab tab = new Tab(text, content);
        tab.setGraphic(graphic);
        return tab;
    }

    private static String dividers(SplitPane split) {
        return Arrays.stream(split.getDividerPositions()).mapToObj(DataUi::fmt).collect(Collectors.joining(", "));
    }

    @Override
    public Node build() {
        boolean animated = !ShowcaseMode.snapshot();

        // --- TabPane, top, closable, graphics
        CheckBox hidden = new CheckBox("Show hidden files");
        hidden.setSelected(true);
        // "Temp" is closed after layout through its close button (TabPaneSkin), without animation thanks to the
        // skin CSS properties -fx-open-tab-animation / -fx-close-tab-animation (see data.css)
        Tab temp = tab("Temp", dot("#43a047"), tabBody("Temp", new Label("Closed through its close button")));
        temp.setId("temp-tab");
        List<String> tabEvents = new ArrayList<>();
        temp.setOnCloseRequest(e -> tabEvents.add("onCloseRequest"));
        temp.setOnClosed(e -> tabEvents.add("onClosed"));
        TabPane topTabs = new TabPane(
                tab("Home", dot("#e53935"), tabBody("Home", new Label("Welcome to the Home tab"))),
                tab("Files", square("#1e88e5"), tabBody("Files tab (selected)", hidden, new CheckBox("Sort by name"),
                        new Button("Refresh"))),
                tab("Settings", star("#fdd835"), tabBody("Settings", new Label("Preferences"))),
                new Tab("Help", tabBody("Help", new Label("Not closable"))),
                temp);
        topTabs.getStyleClass().add("no-tab-animation");
        topTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        topTabs.getTabs().get(3).setClosable(false);
        topTabs.getSelectionModel().select(1);

        // --- TabPane, left side
        TabPane leftTabs = new TabPane(
                tab("Alpha", arrow("#43a047"), colored("pane-a", "Alpha")),
                tab("Beta", arrow("#1e88e5"), colored("pane-b", "Beta")),
                tab("Gamma", arrow("#fb8c00"), colored("pane-c", "Gamma (selected) · rotateGraphic")));
        leftTabs.setSide(Side.LEFT);
        leftTabs.setRotateGraphic(true);
        leftTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        leftTabs.getSelectionModel().select(2);

        // --- TabPane, bottom side, floating style
        TabPane bottomTabs = new TabPane(
                new Tab("One", colored("pane-d", "One")),
                new Tab("Two", colored("pane-b", "Two (selected)")),
                new Tab("Three", colored("pane-a", "Three")));
        bottomTabs.setSide(Side.BOTTOM);
        bottomTabs.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);
        bottomTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        bottomTabs.getSelectionModel().select(1);

        // --- Accordion
        ToggleGroup theme = new ToggleGroup();
        RadioButton light = new RadioButton("Light");
        light.setToggleGroup(theme);
        light.setSelected(true);
        RadioButton dark = new RadioButton("Dark");
        dark.setToggleGroup(theme);
        Slider zoom = new Slider(50, 200, 125);
        zoom.setShowTickMarks(true);
        zoom.setMajorTickUnit(50);
        TitledPane general = new TitledPane("General", new VBox(6, new CheckBox("Auto save"), new CheckBox("Backups")));
        TitledPane appearance = new TitledPane("Appearance (expanded)", new VBox(6, light, dark, zoom));
        TitledPane advanced = new TitledPane("Advanced", new Label("Nothing here"));
        Accordion accordion = new Accordion(general, appearance, advanced);
        accordion.getPanes().forEach(pane -> pane.setAnimated(animated));
        accordion.setExpandedPane(appearance);

        // --- standalone TitledPanes
        TitledPane collapsed = new TitledPane("Details (collapsed)", new Label("Hidden content"));
        collapsed.setExpanded(false);
        collapsed.setAnimated(animated);
        TitledPane fixed = new TitledPane("Summary (not collapsible)",
                new VBox(4, new Label("Rows: 42"), new Label("Columns: 7"), new Label("Status: ready")));
        fixed.setCollapsible(false);
        fixed.setGraphic(dot("#8e24aa"));
        TitledPane graphicTitle = new TitledPane("With graphic (collapsed)", new Label("Content"));
        graphicTitle.setGraphic(star("#fb8c00"));
        graphicTitle.setExpanded(false);
        graphicTitle.setAnimated(animated);
        VBox titledPanes = new VBox(8, collapsed, fixed, graphicTitle);

        // --- SplitPanes
        SplitPane horizontal = new SplitPane(colored("pane-a", "left 0.25"), colored("pane-b", "center 0.25..0.60"),
                colored("pane-c", "right 0.60"));
        horizontal.setDividerPositions(0.25, 0.6);

        SplitPane nested = new SplitPane(colored("pane-a", "A"), colored("pane-b", "B"));
        nested.setDividerPositions(0.5);
        SplitPane vertical = new SplitPane(colored("pane-d", "top 0.35"), nested);
        vertical.setOrientation(Orientation.VERTICAL);
        vertical.setDividerPositions(0.35);

        // --- ScrollPane scrolled to a fixed position
        GridPane grid = new GridPane();
        for (int row = 0; row < 12; row++) {
            for (int col = 0; col < 10; col++) {
                Label cell = new Label(String.format(Locale.ROOT, "C%d·R%d", col, row));
                cell.getStyleClass().add("scroll-cell");
                cell.setPrefSize(64, 40);
                cell.setAlignment(Pos.CENTER);
                cell.setBackground(javafx.scene.layout.Background.fill(Color.hsb(col * 30 + row * 6, 0.25, 1.0)));
                grid.add(cell, col, row);
            }
        }
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setPannable(true);
        scroll.setHvalue(0.5);
        scroll.setVvalue(0.4);

        // --- checks
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("top tabs / selected (built)", "5 / Files",
                () -> topTabs.getTabs().size() + " / " + topTabs.getSelectionModel().getSelectedItem().getText()));
        checks.add(Checks.expect("closable tabs", "[Home, Files, Settings, Temp]", () -> topTabs.getTabs().stream()
                .filter(Tab::isClosable).map(Tab::getText).toList().toString()));
        checks.add(Checks.expect("side tabs", "LEFT Gamma / BOTTOM Two",
                () -> leftTabs.getSide() + " " + leftTabs.getSelectionModel().getSelectedItem().getText() + " / "
                        + bottomTabs.getSide() + " " + bottomTabs.getSelectionModel().getSelectedItem().getText()));
        checks.add(Checks.expect("remove tab (detached)", "2 tabs, B selected, tabPane=null", () -> {
            Tab a = new Tab("A");
            Tab b = new Tab("B");
            Tab c = new Tab("C");
            TabPane pane = new TabPane(a, b, c);
            pane.getSelectionModel().select(a);
            pane.getTabs().remove(a);
            return pane.getTabs().size() + " tabs, " + pane.getSelectionModel().getSelectedItem().getText()
                    + " selected, tabPane=" + a.getTabPane();
        }));
        checks.add(Checks.expect("accordion expanded pane", "Appearance (expanded)",
                () -> accordion.getExpandedPane().getText()));
        // without a skin, collapsing the previously expanded pane is not done (AccordionSkin does it)
        checks.add(Checks.run("setExpandedPane (no skin)", () -> {
            TitledPane p1 = new TitledPane("p1", new Label("1"));
            TitledPane p2 = new TitledPane("p2", new Label("2"));
            Accordion other = new Accordion(p1, p2);
            other.setExpandedPane(p1);
            other.setExpandedPane(p2);
            return other.getExpandedPane().getText() + (p2.isExpanded() ? " expanded" : " collapsed") + ", p1"
                    + (p1.isExpanded() ? " expanded" : " collapsed");
        }));
        checks.add(Checks.expect("titled panes expanded", "[false, true, false]", () -> titledPanes.getChildren().stream()
                .map(n -> ((TitledPane) n).isExpanded()).toList().toString()));

        DataUi.ChecksHolder holder = new DataUi.ChecksHolder("Checks", checks);
        holder.setPrefWidth(492);

        HBox rowA = DataUi.row(
                demo("TabPane · top · closable · graphics · Temp closed", topTabs, 400, 216),
                demo("TabPane · Side.LEFT · graphics", leftTabs, 330, 216),
                demo("TabPane · Side.BOTTOM · floating", bottomTabs, 274, 216));
        VBox left = new VBox(10,
                DataUi.row(demo("Accordion · one expanded", accordion, 256, 220),
                        demo("TitledPane", titledPanes, 256, 220)),
                DataUi.row(demo("SplitPane · vertical 0.35 · nested", vertical, 256, 220),
                        demo("ScrollPane · hvalue 0.5 · vvalue 0.4", scroll, 256, 220)));
        VBox right = new VBox(10, demo("SplitPane · horizontal · dividers 0.25, 0.60", horizontal, 492, 120), holder);
        VBox root = DataUi.page(new VBox(10, rowA, DataUi.row(left, right)));

        CompletionStage<?> ready = Fx.pulses(4).thenRun(() -> holder.complete(List.of(
                Checks.expect("close button (skin)", "[onCloseRequest, onClosed] 4 tabs, 4 headers, Files", () -> {
                    Node closeButton = topTabs.lookup("#temp-tab").lookup(".tab-close-button");
                    Event.fireEvent(closeButton, new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0,
                            MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, false, false, false,
                            null));
                    // without animation, the header is removed at once
                    return tabEvents + " " + topTabs.getTabs().size() + " tabs, "
                            + topTabs.lookupAll(".tab-header-area .tab").size() + " headers, "
                            + topTabs.getSelectionModel().getSelectedItem().getText();
                }),
                check("close button text (bundle)", () -> topTabs.lookup(".tab-close-button").getAccessibleText()),
                Checks.expect("expand General (skin)", "General, Appearance collapsed; restored", () -> {
                    general.setExpanded(true);
                    String result = accordion.getExpandedPane().getText() + ", Appearance "
                            + (appearance.isExpanded() ? "expanded" : "collapsed");
                    appearance.setExpanded(true);
                    return result + (!general.isExpanded() && accordion.getExpandedPane() == appearance ? "; restored"
                            : "; not restored");
                }),
                check("horizontal dividers", () -> dividers(horizontal)),
                check("vertical / nested dividers", () -> dividers(vertical) + " / " + dividers(nested)),
                check("scroll h / v", () -> fmt(scroll.getHvalue()) + " / " + fmt(scroll.getVvalue())),
                check("scroll viewport", () -> {
                    Bounds b = scroll.getViewportBounds();
                    return String.format(Locale.ROOT, "%.0fx%.0f at %.1f,%.1f", b.getWidth(), b.getHeight(),
                            b.getMinX(), b.getMinY());
                }),
                check("collapsed pane height", () -> String.valueOf(Math.round(collapsed.getHeight()))),
                check("skins", () -> List.of(topTabs, accordion, horizontal, scroll).stream()
                        .map(c -> c.getSkin().getClass().getSimpleName().replace("Skin", ""))
                        .collect(Collectors.joining(", "))))));
        DataUi.setReady(root, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return DataUi.ready(content);
    }
}
