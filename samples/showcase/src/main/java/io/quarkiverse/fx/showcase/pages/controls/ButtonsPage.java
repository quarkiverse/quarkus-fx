package io.quarkiverse.fx.showcase.pages.controls;

import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.demo;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.find;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.glyph;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.grow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

@Singleton
public class ButtonsPage implements FeaturePage {

    private static final String BAR_ID = "platform-button-bar";

    @Override
    public String id() {
        return "controls-buttons";
    }

    @Override
    public String title() {
        return "Buttons & Toggles";
    }

    @Override
    public String category() {
        return Categories.CONTROLS;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        ControlsUi.ChecksHolder checks = new ControlsUi.ChecksHolder();
        List<Check> early = checks.early;

        early.add(ControlsUi.fontAwesomeCheck("Font.loadFont(url) Font Awesome"));
        Image icon16 = new Image(Fx.resourceUrl("/showcase/images/icon-16.png"));
        Image icon = new Image(Fx.resourceUrl("/showcase/images/icon.png"), 24, 24, true, true);
        early.add(Checks.run("Image icon-16.png / icon.png@24", () -> {
            if (icon16.isError() || icon.isError()) {
                throw new IllegalStateException("image error", icon16.isError() ? icon16.getException()
                        : icon.getException());
            }
            return (int) icon16.getWidth() + "x" + (int) icon16.getHeight() + " / " + (int) icon.getWidth() + "x"
                    + (int) icon.getHeight();
        }));

        // Plain buttons
        Button defaultButton = new Button("Default");
        defaultButton.setDefaultButton(true);
        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        Button disabledButton = new Button("Disabled");
        disabledButton.setDisable(true);
        Button imageButton = new Button("Image", new ImageView(icon16));
        Button glyphButton = new Button("Home", glyph(ControlsUi.HOME, 14, Color.web("#1565c0")));
        Button armedButton = new Button("Armed");
        armedButton.arm();
        FlowPane plain = new FlowPane(6, 6, defaultButton, cancelButton, disabledButton, imageButton, glyphButton,
                armedButton);
        plain.setPrefWrapLength(480);

        Button green = new Button("-fx-base");
        green.getStyleClass().add("pill-green");
        Button round = new Button("Rounded");
        round.getStyleClass().add("round-button");
        Button big = new Button("Big bold");
        big.getStyleClass().add("big-button");
        Button shadow = new Button("Drop shadow");
        shadow.getStyleClass().add("shadow-button");
        Button gradient = new Button("Gradient");
        gradient.getStyleClass().add("gradient-button");
        Button heart = new Button(null, glyph(ControlsUi.HEART, 16, Color.web("#d81b60")));
        FlowPane styled = new FlowPane(6, 6, green, round, big, shadow, gradient, heart);
        styled.setAlignment(Pos.CENTER_LEFT);
        styled.setPrefWrapLength(480);

        // ContentDisplay variants
        HBox contentDisplays = new HBox(6);
        contentDisplays.setAlignment(Pos.CENTER_LEFT);
        for (ContentDisplay display : ContentDisplay.values()) {
            Button button = new Button(display.name(), new ImageView(icon));
            button.setContentDisplay(display);
            button.setMnemonicParsing(false);
            contentDisplays.getChildren().add(button);
        }
        early.add(Checks.expect("ContentDisplay variants", 7, () -> ContentDisplay.values().length));

        // Text overrun and wrapping
        HBox overruns = new HBox(6);
        overruns.setAlignment(Pos.CENTER_LEFT);
        for (OverrunStyle overrun : List.of(OverrunStyle.ELLIPSIS, OverrunStyle.CENTER_WORD_ELLIPSIS,
                OverrunStyle.LEADING_ELLIPSIS, OverrunStyle.CLIP)) {
            Button button = new Button(overrun.name().toLowerCase(java.util.Locale.ROOT) + " : a long button text");
            button.setMnemonicParsing(false);
            button.setTextOverrun(overrun);
            button.setPrefWidth(92);
            overruns.getChildren().add(button);
        }
        Button wrapped = new Button("wrapText : two lines of text");
        wrapped.setWrapText(true);
        wrapped.setPrefWidth(92);
        wrapped.setTextAlignment(TextAlignment.CENTER);
        overruns.getChildren().add(wrapped);

        // ToggleButtons
        ToggleGroup alignGroup = new ToggleGroup();
        ToggleButton left = toggle("Left", "left-pill", alignGroup);
        ToggleButton center = toggle("Center", "center-pill", alignGroup);
        ToggleButton right = toggle("Right", "right-pill", alignGroup);
        center.setSelected(true);
        HBox pills = new HBox(left, center, right);
        ToggleButton bold = new ToggleButton(null, glyph(ControlsUi.BOLD, 12));
        bold.setSelected(true);
        ToggleButton italic = new ToggleButton(null, glyph(ControlsUi.ITALIC, 12));
        ToggleButton disabledToggle = new ToggleButton("Disabled on");
        disabledToggle.setSelected(true);
        disabledToggle.setDisable(true);
        HBox formatting = new HBox(4, bold, italic, disabledToggle);
        early.add(Checks.expect("ToggleGroup selected toggle", "Center",
                () -> ((ToggleButton) alignGroup.getSelectedToggle()).getText()));
        early.add(Checks.expect("ToggleGroup exclusive selection", "Left:true Center:false Right:false", () -> {
            ToggleGroup group = new ToggleGroup();
            ToggleButton a = toggle("Left", null, group);
            ToggleButton b = toggle("Center", null, group);
            ToggleButton c = toggle("Right", null, group);
            b.setSelected(true);
            a.fire();
            return List.of(a, b, c).stream().map(t -> t.getText() + ":" + t.isSelected())
                    .collect(Collectors.joining(" "));
        }));

        // RadioButtons
        ToggleGroup sizes = new ToggleGroup();
        RadioButton small = radio("Small", sizes);
        RadioButton medium = radio("Medium", sizes);
        RadioButton large = radio("Large", sizes);
        RadioButton disabledRadio = radio("Disabled", new ToggleGroup());
        disabledRadio.setDisable(true);
        disabledRadio.setSelected(true);
        medium.setSelected(true);
        VBox radios = new VBox(3, small, medium, large, disabledRadio);
        early.add(Checks.expect("RadioButton group selection", "Medium",
                () -> ((RadioButton) sizes.getSelectedToggle()).getText()));

        // CheckBoxes
        CheckBox checked = new CheckBox("Checked");
        checked.setSelected(true);
        CheckBox unchecked = new CheckBox("Unchecked");
        CheckBox indeterminate = new CheckBox("Indeterminate");
        indeterminate.setAllowIndeterminate(true);
        indeterminate.setIndeterminate(true);
        CheckBox disabledCheck = new CheckBox("Disabled");
        disabledCheck.setSelected(true);
        disabledCheck.setDisable(true);
        VBox checkBoxes = new VBox(3, checked, unchecked, indeterminate, disabledCheck);
        early.add(Checks.expect("CheckBox tri-state cycle", "indeterminate > checked > unchecked", () -> {
            CheckBox box = new CheckBox();
            box.setAllowIndeterminate(true);
            List<String> states = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                box.fire();
                states.add(box.isIndeterminate() ? "indeterminate" : box.isSelected() ? "checked" : "unchecked");
            }
            return String.join(" > ", states);
        }));

        // Hyperlinks
        Hyperlink link = new Hyperlink("Not visited");
        Hyperlink visited = new Hyperlink("Visited");
        visited.setVisited(true);
        Hyperlink disabledLink = new Hyperlink("Disabled");
        disabledLink.setDisable(true);
        Hyperlink glyphLink = new Hyperlink("With graphic", glyph(ControlsUi.STAR, 11, Color.web("#f9a825")));
        VBox links = new VBox(2, link, visited, disabledLink, glyphLink);
        early.add(Checks.expect("Hyperlink :visited pseudo-class", "false/true",
                () -> link.getPseudoClassStates().contains(PseudoClass.getPseudoClass("visited")) + "/"
                        + visited.getPseudoClassStates().contains(PseudoClass.getPseudoClass("visited"))));

        // Labels
        TextField nameField = new TextField("Quarkus");
        nameField.setPrefColumnCount(8);
        nameField.setId("mnemonic-target");
        Label mnemonic = new Label("_Name:");
        mnemonic.setId("mnemonic-label");
        mnemonic.setMnemonicParsing(true);
        mnemonic.setLabelFor(nameField);
        HBox mnemonicRow = new HBox(6, mnemonic, nameField);
        mnemonicRow.setAlignment(Pos.CENTER_LEFT);
        Label graphicLabel = new Label("Notifications", glyph(ControlsUi.BELL, 13, Color.web("#ef6c00")));
        Label rightGraphic = new Label("Graphic on the right", new ImageView(icon16));
        rightGraphic.setContentDisplay(ContentDisplay.RIGHT);
        rightGraphic.setGraphicTextGap(8);
        VBox labels = new VBox(6, mnemonicRow, graphicLabel, rightGraphic);
        early.add(Checks.expect("Label mnemonic parsing", "true, labelFor=TextField",
                () -> mnemonic.isMnemonicParsing() + ", labelFor=" + mnemonic.getLabelFor().getClass().getSimpleName()));

        // MenuButton and SplitMenuButton
        Label actionResult = new Label("(no action)");
        MenuButton menuButton = new MenuButton("Actions", glyph(ControlsUi.COG, 12));
        MenuItem rename = new MenuItem("Rename");
        rename.setOnAction(e -> actionResult.setText("Rename"));
        menuButton.getItems().addAll(rename, new MenuItem("Duplicate"), new MenuItem("Delete"));
        SplitMenuButton split = new SplitMenuButton(new MenuItem("Save as..."), new MenuItem("Save all"));
        split.setText("Save");
        split.setGraphic(glyph(ControlsUi.SAVE, 12));
        split.setOnAction(e -> actionResult.setText("Save"));
        MenuButton topMenu = new MenuButton("Opens up");
        topMenu.setPopupSide(javafx.geometry.Side.TOP);
        topMenu.getItems().add(new MenuItem("Item"));
        HBox topRow = new HBox(6, topMenu, actionResult);
        topRow.setAlignment(Pos.CENTER_LEFT);
        VBox menuButtons = new VBox(6, menuButton, split, topRow);
        early.add(Checks.expect("MenuItem.fire / SplitMenuButton.fire", "Rename > Save", () -> {
            rename.fire();
            String first = actionResult.getText();
            split.fire();
            String second = actionResult.getText();
            actionResult.setText("(no action)");
            return first + " > " + second;
        }));
        early.add(Checks.expect("disabled Button.fire ignored", "not fired", () -> {
            String[] fired = { "not fired" };
            disabledButton.setOnAction(e -> fired[0] = "fired");
            disabledButton.fire();
            return fired[0];
        }));
        early.add(Checks.expect("default/cancel pseudo-classes", "true/true",
                () -> defaultButton.getPseudoClassStates().contains(PseudoClass.getPseudoClass("default")) + "/"
                        + cancelButton.getPseudoClassStates().contains(PseudoClass.getPseudoClass("cancel"))));

        // Resources and classes referenced from controls.css (and from an inline style)
        Button cssRelative = new Button("relative url");
        cssRelative.setId("css-graphic-relative");
        cssRelative.getStyleClass().add("css-graphic-relative");
        Button cssRoot = new Button("/classpath url");
        cssRoot.setId("css-graphic-root");
        cssRoot.getStyleClass().add("css-graphic-root");
        Button cssInline = new Button("inline style url");
        cssInline.setId("css-graphic-inline");
        // no stylesheet URL to resolve against : resolved by the context class loader
        cssInline.setStyle("-fx-graphic: url('showcase/images/icon-16.png');");
        Button cssData = new Button("data: url");
        cssData.setId("css-graphic-data");
        cssData.getStyleClass().add("css-graphic-data");
        javafx.scene.image.ImageView cssImage = new javafx.scene.image.ImageView();
        cssImage.setId("css-image-view");
        cssImage.getStyleClass().add("css-image-view");
        javafx.scene.layout.Region cssBackground = new javafx.scene.layout.Region();
        cssBackground.setId("css-background-image");
        cssBackground.getStyleClass().add("css-background-image");
        Button cssShape = new Button("-fx-shape");
        cssShape.setId("css-shape-button");
        cssShape.getStyleClass().add("css-shape-button");
        Label cssFont = new Label("@font-face Roboto Light");
        cssFont.setId("css-font-face");
        cssFont.getStyleClass().add("css-font-face");
        Button cssSkin = new Button("skin");
        cssSkin.setId("css-skin-button");
        cssSkin.getStyleClass().add("css-skin-button");
        HBox cssRow = new HBox(8, cssRelative, cssRoot, cssInline, cssData, cssImage, cssBackground, cssShape,
                cssFont, cssSkin);
        early.add(Checks.run("CssParser.parse(controls.css) rules / @font-face", () -> {
            javafx.css.Stylesheet stylesheet = new javafx.css.CssParser()
                    .parse(Fx.resource("/showcase/controls/controls.css"));
            return stylesheet.getRules().size() + " rules / " + stylesheet.getFontFaces().size() + " font face";
        }));
        cssRow.setAlignment(Pos.CENTER_LEFT);

        // ButtonBar : buttons are added in an arbitrary order, the bar sorts them by ButtonData
        ButtonBar platformBar = buttonBar(null);
        platformBar.setId(BAR_ID);
        ButtonBar windowsBar = buttonBar(ButtonBar.BUTTON_ORDER_WINDOWS);
        early.add(Checks.run("ButtonBar default order (platform)", platformBar::getButtonOrder));
        early.add(Checks.expect("ButtonData OK_DONE type code / default", "O/true",
                () -> ButtonData.OK_DONE.getTypeCode() + "/" + ButtonData.OK_DONE.isDefaultButton()));

        HBox row1 = new HBox(8, grow(demo("Button : default, cancel, disabled, image graphic, Font Awesome glyph, armed",
                plain)), grow(demo("Styled with CSS : -fx-base, radius, font, effect, gradient", styled)));
        HBox row2 = new HBox(8, demo("ContentDisplay variants (graphic : icon.png scaled to 24px)", contentDisplays),
                grow(demo("OverrunStyle : ellipsis, center-word-ellipsis, leading-ellipsis, clip / wrapText",
                        overruns)));
        HBox row3 = new HBox(8,
                demo("ToggleButton + ToggleGroup (pills)", pills, formatting),
                demo("RadioButton group", radios),
                demo("CheckBox (tri-state)", checkBoxes),
                demo("Hyperlink", links),
                demo("MenuButton / SplitMenuButton", menuButtons),
                grow(demo("Label : mnemonic, graphic", labels)));
        VBox bars = demo("ButtonBar : added as Help, Cancel, No, Apply, Next, Yes, Back, OK, Finish, Left, Right, Other"
                + " - platform order (top) and BUTTON_ORDER_WINDOWS (bottom)", platformBar, windowsBar);
        VBox css = demo("From controls.css : -fx-graphic url() relative to the stylesheet, from the classpath root, in an"
                + " inline style and as a data URI, -fx-image, -fx-background-image, -fx-shape, @font-face, -fx-skin"
                + " (custom Skin class instantiated by reflection)", cssRow);
        checks.setPadding(new Insets(0));

        VBox root = ControlsUi.page(6, row1, row2, row3, bars, css, checks);
        root.setPrefWidth(1028);
        ControlsUi.rememberFocus(root);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Fx.pulses(2).thenRunAsync(() -> {
            ControlsUi.ChecksHolder holder = find(content, ControlsUi.ChecksHolder.class);
            ButtonBar bar = (ButtonBar) content.lookup("#" + BAR_ID);
            List<Check> late = new ArrayList<>();
            // ButtonBarSkin requests the focus on the first button whose ButtonData is a default one
            late.add(Checks.run("ButtonBarSkin focused the default ButtonData button", () -> {
                Node owner = content.getScene().getFocusOwner();
                // never Node.toString() : it contains an identity hash code
                return owner instanceof Button b ? b.getText() : ControlsUi.describe(owner);
            }));
            ControlsUi.restoreFocus(content);
            late.add(Checks.run("ButtonBar laid out order (by x)", () -> bar.getButtons().stream()
                    .sorted(Comparator.comparingDouble(b -> b.localToScene(0, 0).getX()))
                    .map(b -> ((Button) b).getText())
                    .collect(Collectors.joining(" "))));
            // LabeledSkinBase registers the mnemonic of the label in the scene, targeting its labelFor node
            late.add(Checks.run("Scene mnemonic of the Label _Name:", () -> {
                Node target = content.lookup("#mnemonic-target");
                String names = content.getScene().getMnemonics().entrySet().stream()
                        .filter(e -> e.getValue().stream().anyMatch(m -> m.getNode() == target))
                        .map(e -> e.getKey().getName())
                        .sorted()
                        .collect(Collectors.joining(", "));
                // none on macOS, where LabeledSkinBase does not register mnemonics, an Alt combination on Windows
                // and Linux (reported, not compared)
                return names.isEmpty() ? "none registered" : names;
            }));
            late.add(Checks.expect("CSS -fx-graphic url() (relative / root / inline / data)",
                    "16x16 / 32x32 / 16x16 / 12x12",
                    () -> graphicSize(content, "#css-graphic-relative") + " / "
                            + graphicSize(content, "#css-graphic-root") + " / "
                            + graphicSize(content, "#css-graphic-inline") + " / "
                            + graphicSize(content, "#css-graphic-data")));
            late.add(Checks.expect("CSS -fx-image on an ImageView", "64x64", () -> {
                Image image = ((javafx.scene.image.ImageView) content.lookup("#css-image-view")).getImage();
                return size(image);
            }));
            late.add(Checks.expect("CSS -fx-background-image", "256x256", () -> {
                javafx.scene.layout.Region region = (javafx.scene.layout.Region) content
                        .lookup("#css-background-image");
                var background = region.getBackground();
                if (background == null || background.getImages().isEmpty()) {
                    return "no background image";
                }
                return size(background.getImages().getFirst().getImage());
            }));
            late.add(Checks.expect("CSS -fx-shape", "SVGPath", () -> {
                javafx.scene.shape.Shape shape = ((javafx.scene.layout.Region) content.lookup("#css-shape-button"))
                        .getShape();
                return shape == null ? "no shape" : shape.getClass().getSimpleName();
            }));
            late.add(Checks.expect("CSS @font-face font (family / name)", "Roboto / Roboto Light", () -> {
                javafx.scene.text.Font font = ((Label) content.lookup("#css-font-face")).getFont();
                return font.getFamily() + " / " + font.getName();
            }));
            late.add(Checks.expect("CSS -fx-skin custom skin", BadgeButtonSkin.class.getSimpleName(),
                    () -> ((Button) content.lookup("#css-skin-button")).getSkin().getClass().getSimpleName()));
            holder.show("Checks", late, 2);
        }, Fx.FX_THREAD);
    }

    private static String graphicSize(Node content, String selector) {
        Node graphic = ((Button) content.lookup(selector)).getGraphic();
        return graphic instanceof javafx.scene.image.ImageView view ? size(view.getImage())
                : ControlsUi.describe(graphic);
    }

    private static String size(Image image) {
        if (image == null) {
            return "no image";
        }
        if (image.isError()) {
            return "error: " + Checks.describe(image.getException());
        }
        return (int) image.getWidth() + "x" + (int) image.getHeight();
    }

    private static ToggleButton toggle(String text, String styleClass, ToggleGroup group) {
        ToggleButton toggle = new ToggleButton(text);
        toggle.setToggleGroup(group);
        if (styleClass != null) {
            toggle.getStyleClass().add(styleClass);
        }
        return toggle;
    }

    private static RadioButton radio(String text, ToggleGroup group) {
        RadioButton radio = new RadioButton(text);
        radio.setToggleGroup(group);
        return radio;
    }

    private static ButtonBar buttonBar(String order) {
        ButtonBar bar = order == null ? new ButtonBar() : new ButtonBar(order);
        bar.setButtonMinWidth(60);
        bar.getButtons().addAll(
                button("Help", ButtonData.HELP),
                button("Cancel", ButtonData.CANCEL_CLOSE),
                button("No", ButtonData.NO),
                button("Apply", ButtonData.APPLY),
                button("Next", ButtonData.NEXT_FORWARD),
                button("Yes", ButtonData.YES),
                button("Back", ButtonData.BACK_PREVIOUS),
                button("OK", ButtonData.OK_DONE),
                button("Finish", ButtonData.FINISH),
                button("Left", ButtonData.LEFT),
                button("Right", ButtonData.RIGHT),
                button("Other", ButtonData.OTHER));
        return bar;
    }

    private static Button button(String text, ButtonData data) {
        Button button = new Button(text);
        ButtonBar.setButtonData(button, data);
        return button;
    }
}
