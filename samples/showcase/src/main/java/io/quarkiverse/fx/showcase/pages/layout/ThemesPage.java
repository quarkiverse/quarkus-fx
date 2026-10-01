package io.quarkiverse.fx.showcase.pages.layout;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.css.Stylesheet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.chart.Axis;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@Singleton
public class ThemesPage implements FeaturePage {

    /** What PlatformImpl loads for {@link Application#STYLESHEET_MODENA} and {@link Application#STYLESHEET_CASPIAN}. */
    static final String MODENA = "com/sun/javafx/scene/control/skin/modena/modena.css";
    static final String CASPIAN = "com/sun/javafx/scene/control/skin/caspian/caspian.css";

    private static final double COLUMN = 333;
    private static final double SUBSCENE_HEIGHT = 486;

    @Override
    public String id() {
        return "layout-themes";
    }

    @Override
    public String title() {
        return "Themes: Modena, Caspian, high contrast";
    }

    @Override
    public String category() {
        return Categories.LAYOUT_CSS;
    }

    @Override
    public int order() {
        return 50;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Node build() throws Exception {
        int cssMark = Kit.cssErrorMark();
        VBox root = Kit.page("themes-page");

        URL modenaUrl = Control.class.getResource("/" + MODENA);
        URL highContrastUrl = modenaUrl == null ? null : new URL(modenaUrl, "blackOnWhite.css");

        Theme modena = theme(MODENA, null);
        Theme caspian = theme(CASPIAN, null);
        Theme highContrast = theme(MODENA, highContrastUrl == null ? null : highContrastUrl.toExternalForm());

        HBox themes = new HBox(14,
                Kit.captioned("STYLESHEET_MODENA · SubScene user agent modena.css", modena.subScene),
                Kit.captioned("STYLESHEET_CASPIAN · SubScene user agent caspian.css", caspian.subScene),
                Kit.captioned("Modena + blackOnWhite.css (high contrast)", highContrast.subScene));

        HBox checks = Kit.checksRow(1028);
        root.getChildren().addAll(themes, checks);

        Kit.whenShown(root, 3, () -> {
            List<Check> left = new ArrayList<>();
            left.add(Check.info("Application.STYLESHEET_* constants", Application.STYLESHEET_MODENA + ", "
                    + Application.STYLESHEET_CASPIAN));
            left.add(Checks.expect("theme resources (css, bss)", "true true true true true true", () -> exists(MODENA) + " "
                    + exists(MODENA.replace(".css", ".bss")) + " " + exists(CASPIAN) + " " + exists(CASPIAN.replace(".css",
                            ".bss"))
                    + " " + (highContrastUrl != null) + " " + exists("com/sun/javafx/scene/control/skin/modena/blackOnWhite.bss")));
            left.add(Checks.expect("blackOnWhite.css relative to modena.css", "blackOnWhite.css", () -> {
                String path = highContrastUrl.getPath();
                try (var in = highContrastUrl.openStream()) {
                    in.read();
                }
                return path.substring(path.lastIndexOf('/') + 1);
            }));
            left.add(Checks.run("loadBinary rules (modena, caspian, bow .bss)", () -> rules(MODENA) + ", "
                    + rules(CASPIAN) + ", " + rules("com/sun/javafx/scene/control/skin/modena/blackOnWhite.css")));
            left.add(Check.info("Application.getUserAgentStylesheet()", Application.getUserAgentStylesheet()));
            List<Check> right = new ArrayList<>();
            right.add(Check.info("Modena button / root fills", describe(modena)));
            right.add(Check.info("Caspian button / root fills", describe(caspian)));
            right.add(Check.info("High contrast button / root fills", describe(highContrast)));
            right.add(Kit.cssErrorsCheck("CSS errors on this page", cssMark));
            Kit.fillChecks(checks, "Theme stylesheets", left, "Resolved theme colors", right);
        });
        return root;
    }

    /**
     * Rule count of the binary version (.bss) of a theme stylesheet, as StyleManager loads it.
     */
    private static int rules(String cssPath) throws java.io.IOException {
        URL bss = Control.class.getResource("/" + cssPath.replace(".css", ".bss"));
        if (bss == null) {
            throw new java.io.FileNotFoundException(cssPath.replace(".css", ".bss"));
        }
        return Stylesheet.loadBinary(bss).getRules().size();
    }

    private static boolean exists(String path) {
        return Control.class.getResource("/" + path) != null;
    }

    private static String describe(Theme theme) {
        return theme.button.getBackground().getFills().size() + " fills, " + Kit.fill(theme.button, 0) + " · root "
                + Kit.fill(theme.root) + " · text " + Kit.paint(theme.label.getTextFill());
    }

    private record Theme(SubScene subScene, VBox root, Button button, Label label) {
    }

    private static Theme theme(String userAgentStylesheet, String authorStylesheet) {
        Label label = new Label("Label");
        Button button = new Button("Button");
        Button defaultButton = new Button("Default");
        defaultButton.setDefaultButton(true);
        Button disabled = new Button("Disabled");
        disabled.setDisable(true);
        HBox buttons = new HBox(6, button, defaultButton, disabled);

        ToggleButton toggleOn = new ToggleButton("Toggle on");
        toggleOn.setSelected(true);
        ToggleButton toggleOff = new ToggleButton("Toggle off");
        Hyperlink link = new Hyperlink("Hyperlink");
        HBox toggles = new HBox(6, toggleOn, toggleOff, link);
        toggles.setAlignment(Pos.CENTER_LEFT);

        CheckBox checked = new CheckBox("Checked");
        checked.setSelected(true);
        CheckBox indeterminate = new CheckBox("Indeterminate");
        indeterminate.setAllowIndeterminate(true);
        indeterminate.setIndeterminate(true);
        CheckBox unchecked = new CheckBox("Off");
        HBox checks = new HBox(8, checked, indeterminate, unchecked);

        ToggleGroup group = new ToggleGroup();
        RadioButton radioA = new RadioButton("Radio A");
        radioA.setToggleGroup(group);
        radioA.setSelected(true);
        RadioButton radioB = new RadioButton("Radio B");
        radioB.setToggleGroup(group);
        HBox radios = new HBox(10, radioA, radioB);

        TextField field = new TextField("Text field");
        field.setPrefColumnCount(10);
        TextField prompt = new TextField();
        prompt.setPromptText("Prompt");
        prompt.setPrefColumnCount(6);
        HBox fields = new HBox(6, field, prompt);

        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList("Combo box", "Second", "Third"));
        combo.getSelectionModel().select(0);
        ChoiceBox<String> choice = new ChoiceBox<>(FXCollections.observableArrayList("Choice", "Other"));
        choice.getSelectionModel().select(0);
        HBox combos = new HBox(6, combo, choice);

        Slider slider = new Slider(0, 100, 40);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(25);
        slider.setPrefWidth(290);
        // SliderSkin draws the ticks with a NumberAxis, animated by default : its tick labels fade in over 750ms once
        // the slider is shown, and a snapshot would catch that fade at a timing dependent opacity. The skin is created
        // here to switch the axis animation off.
        slider.setSkin(new SliderSkin(slider));
        if (slider.lookup(".axis") instanceof Axis<?> axis) {
            axis.setAnimated(false);
        }
        ProgressBar progress = new ProgressBar(0.6);
        progress.setPrefWidth(200);
        Spinner<Integer> spinner = new Spinner<>(0, 10, 3);
        spinner.setPrefWidth(80);
        HBox progressRow = new HBox(8, progress, spinner);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        TabPane tabs = new TabPane(new Tab("First", new Label("  Tab content")), new Tab("Second"), new Tab("Third"));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefSize(300, 64);
        tabs.setMinHeight(64);

        ListView<String> list = new ListView<>(FXCollections.observableArrayList("List item 1", "List item 2 (selected)",
                "List item 3", "List item 4"));
        list.getSelectionModel().select(1);
        list.setFixedCellSize(22);
        list.setPrefSize(300, 92);
        list.setMinHeight(92);

        VBox content = new VBox(9, label, buttons, toggles, checks, radios, fields, combos, slider, progressRow, tabs, list);
        content.setPadding(new Insets(10));
        for (Node node : content.lookupAll("*")) {
            node.setFocusTraversable(false);
        }

        SubScene subScene = new SubScene(content, COLUMN, SUBSCENE_HEIGHT);
        subScene.setUserAgentStylesheet(userAgentStylesheet);
        if (authorStylesheet != null) {
            content.getStylesheets().add(authorStylesheet);
        }
        return new Theme(subScene, content, button, label);
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Kit.ready(content);
    }
}
