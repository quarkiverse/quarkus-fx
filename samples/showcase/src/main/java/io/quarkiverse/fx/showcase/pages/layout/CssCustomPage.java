package io.quarkiverse.fx.showcase.pages.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.application.ColorScheme;
import javafx.css.PseudoClass;
import javafx.css.TransitionEvent;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Control;

@Singleton
public class CssCustomPage implements FeaturePage {

    private static final PseudoClass ON = PseudoClass.getPseudoClass("on");
    private static final String SNAPSHOT_ERROR = "layout.snapshot-error";
    private static final double COLUMN = 333;
    private static final double AREA_HEIGHT = 440;
    private static final int SCENE_WIDTH = 313;
    private static final int SCENE_HEIGHT = 184;

    @Override
    public String id() {
        return "layout-css-custom";
    }

    @Override
    public String title() {
        return "Custom CSS properties, transitions, media queries";
    }

    @Override
    public String category() {
        return Categories.LAYOUT_CSS;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Node build() {
        int cssMark = Kit.cssErrorMark();
        VBox root = Kit.page("custom-page", "custom.css");

        // ---- custom control ----
        Badge ua = new Badge("UA");
        Badge primary = badge("1", "primary");
        Badge success = badge("2", "success");
        Badge warning = badge("3", "warning");
        Badge outlined = badge("4", "outlined");
        Badge large = badge("5", "primary", "large");
        Badge lookup = badge("6", "lookup");
        Badge alert = new Badge("!");
        alert.setAlert(true);
        Badge inline = new Badge("7");
        inline.setStyle("-badge-color: #8e24aa; -badge-shape: diamond; -badge-size: 52; -badge-ring-width: 3;");
        GridPane badges = new GridPane(6, 4);
        badges.setAlignment(Pos.CENTER);
        Badge ringSkin = badge("8", "ring-skin");
        Badge[] all = { ua, primary, success, warning, outlined, large, lookup, alert, inline, ringSkin };
        String[] notes = { "UA stylesheet", ".primary", ".success", ".warning", ".outlined", ".primary.large", ".lookup (derive)",
                ":alert", "inline style", "-fx-skin (CSS)" };
        for (int i = 0; i < all.length; i++) {
            Label note = new Label(notes[i]);
            note.getStyleClass().add("note");
            VBox cell = new VBox(2, all[i], note);
            cell.setAlignment(Pos.BOTTOM_CENTER);
            cell.setMinSize(96, 82);
            cell.setPrefSize(96, 82);
            // the last one, alone on its row, is centered
            badges.add(cell, i == 9 ? 1 : i % 3, i / 3);
        }
        VBox legend = new VBox(1);
        for (String line : List.of("StyleablePropertyFactory: -badge-color <color>, -badge-size <number>,",
                "-badge-outlined <boolean>, -badge-shape <enum BadgeShape>", "CssMetaData: -badge-text-fill <paint>, -badge-ring-width <size>",
                "Region.getUserAgentStylesheet() = badge.css")) {
            Label label = new Label(line);
            label.getStyleClass().add("note");
            legend.getChildren().add(label);
        }
        VBox badgeBox = new VBox(10, badges, legend);
        badgeBox.setPadding(new Insets(8));
        badgeBox.setAlignment(Pos.TOP_CENTER);
        Node badgeDemo = Kit.demo("Custom control · styleable properties from CSS", COLUMN, AREA_HEIGHT, badgeBox);

        // ---- transitions ----
        String[][] transitions = { { "tr-color", "-fx-background-color 300ms linear" }, { "tr-opacity", "-fx-opacity 300ms ease" },
                { "tr-translate", "-fx-translate-x 300ms ease-in-out" }, { "tr-rotate", "-fx-rotate 300ms steps(3, jump-end)" },
                { "tr-scale", "-fx-scale-x/y 300ms cubic-bezier()" }, { "tr-delayed", "border 200ms delay 100ms, text step-end" },
                { "tr-all", "all 300ms ease-in" } };
        VBox transitionRows = new VBox(5);
        List<Label> animated = new ArrayList<>();
        // property -> RUN, START, END, CANCEL counts
        Map<String, int[]> events = new TreeMap<>();
        for (String[] transition : transitions) {
            Label before = new Label("from");
            before.getStyleClass().addAll("tr-box", transition[0]);
            Label after = new Label("to");
            after.getStyleClass().addAll("tr-box", transition[0]);
            after.addEventHandler(TransitionEvent.ANY, e -> {
                int[] counts = events.computeIfAbsent(e.getPropertyName().replace("-fx-", ""), k -> new int[4]);
                EventType<? extends Event> type = e.getEventType();
                counts[type == TransitionEvent.RUN ? 0 : type == TransitionEvent.START ? 1 : type == TransitionEvent.END ? 2 : 3]++;
            });
            animated.add(after);
            Label spec = new Label(transition[1]);
            spec.getStyleClass().add("note");
            spec.setWrapText(true);
            spec.setMinWidth(95);
            spec.setPrefWidth(95);
            spec.setMaxWidth(95);
            // the end state may be translated, rotated or scaled : room around the box
            StackPane slot = new StackPane(after);
            slot.setAlignment(Pos.CENTER_LEFT);
            slot.setMinSize(136, 48);
            slot.setPrefSize(136, 48);
            slot.setMaxSize(136, 48);
            HBox row = new HBox(10, before, slot, spec);
            row.setAlignment(Pos.CENTER_LEFT);
            transitionRows.getChildren().add(row);
        }
        ToggleButton replay = new ToggleButton("toggle :on");
        replay.setFocusTraversable(false);
        replay.setOnAction(e -> animated.forEach(label -> label.pseudoClassStateChanged(ON, replay.isSelected())));
        VBox transitionBox = new VBox(6, transitionRows, replay);
        transitionBox.setPadding(new Insets(6, 10, 6, 10));
        Node transitionDemo = Kit.demo("CSS transitions · left: initial state · right: :on, end state", COLUMN, AREA_HEIGHT,
                transitionBox);

        // ---- color scheme preferences ----
        List<VBox> schemeRoots = new ArrayList<>();
        List<Scene> scenes = new ArrayList<>();
        ImageView light = schemeImage(ColorScheme.LIGHT, false, schemeRoots, scenes);
        ImageView dark = schemeImage(ColorScheme.DARK, true, schemeRoots, scenes);
        Label lightCaption = new Label("Scene preferences: colorScheme LIGHT, reducedMotion false");
        lightCaption.getStyleClass().add("note");
        Label darkCaption = new Label("Scene preferences: colorScheme DARK, reducedMotion true");
        darkCaption.getStyleClass().add("note");
        VBox schemes = new VBox(4, lightCaption, light, darkCaption, dark);
        schemes.setPadding(new Insets(6, 9, 6, 9));
        Node schemeDemo = Kit.demo("@media (prefers-color-scheme) · off-screen Scene snapshots", COLUMN, AREA_HEIGHT, schemes);

        HBox columns = new HBox(14, badgeDemo, transitionDemo, schemeDemo);
        HBox checks = Kit.checksRow(1028);
        root.getChildren().addAll(columns, checks);

        Kit.whenShown(root, 3, () -> {
            // the transitions only run when a state changes after the first CSS pass
            replay.setSelected(true);
            animated.forEach(label -> label.pseudoClassStateChanged(ON, true));
            // all transitions last at most 350ms (delay included) : wait for their END events, whatever the load
            return Fx.delay(450).thenCompose(v -> Kit.until(() -> ended(events), 600)).thenRun(() -> {
                List<Check> left = new ArrayList<>();
                left.add(Checks.expect("CssMetaData count (Control + 6)", Control.getClassCssMetaData().size() + 6,
                        () -> Badge.getClassCssMetaData().size()));
                left.add(Check.info("UA defaults", describe(ua)));
                left.add(Check.info(".warning", describe(warning)));
                left.add(Check.info(".outlined / .large", describe(outlined) + " / " + describe(large)));
                left.add(Check.info(".lookup / :alert", describe(lookup) + " / " + describe(alert)));
                left.add(Check.info("inline", describe(inline)));
                left.add(Checks.expect("skins (default · -fx-skin from CSS)", "BadgeSkin · BadgeRingSkin",
                        () -> skinName(ua) + " · " + skinName(ringSkin)));
                left.add(Check.info("-badge-color origins", ua.colorOrigin() + ", " + primary.colorOrigin() + ", "
                        + inline.colorOrigin()));
                List<Check> right = new ArrayList<>();
                right.add(Check.info("transitioned properties (count)", events.entrySet().stream()
                        .map(e -> e.getKey() + " " + e.getValue()[0]).collect(Collectors.joining(", "))));
                int[] totals = new int[4];
                events.values().forEach(counts -> {
                    for (int i = 0; i < 4; i++) {
                        totals[i] += counts[i];
                    }
                });
                right.add(Check.of("events RUN/START/END/CANCEL", totals[0] > 0 && totals[0] == totals[2] && totals[3] == 0,
                        totals[0] + "/" + totals[1] + "/" + totals[2] + "/" + totals[3]));
                right.add(Check.info("end state (opacity, tx, rotate, scale)",
                        Kit.num(animated.get(1).getOpacity()) + ", " + Kit.num(animated.get(2).getTranslateX()) + ", "
                                + Kit.num(animated.get(3).getRotate()) + ", " + Kit.num(animated.get(4).getScaleX())));
                right.add(Check.info("end state colors", Kit.fill(animated.get(0)) + ", " + Kit.paint(animated.get(5)
                        .getTextFill()) + ", " + Kit.fill(animated.get(6))));
                right.add(Check.info("scheme root fills (light / dark)",
                        Kit.fill(schemeRoots.get(0)) + " / " + Kit.fill(schemeRoots.get(1))));
                right.add(Check.info("indicators light", indicators(schemeRoots.get(0))));
                right.add(Check.info("indicators dark", indicators(schemeRoots.get(1))));
                right.add(Checks.expect("scene snapshots (light, dark)", "313x184, 313x184",
                        () -> snapshotSize(light) + ", " + snapshotSize(dark)));
                right.add(Check.info("scene preferences", scenes.stream().map(s -> s.getPreferences().getColorScheme() + "/"
                        + s.getPreferences().isReducedMotion()).collect(Collectors.joining(", "))));
                right.add(Kit.cssErrorsCheck("CSS errors on this page", cssMark));
                Kit.fillChecks(checks, "Styleable properties", left, "Transitions and media queries", right);
            });
        });
        return root;
    }

    /**
     * {@code true} once transitions ran and every one of them ended or was canceled.
     */
    private static boolean ended(Map<String, int[]> events) {
        int run = 0;
        int finished = 0;
        for (int[] counts : events.values()) {
            run += counts[0];
            finished += counts[2] + counts[3];
        }
        return run > 0 && finished >= run;
    }

    private static Badge badge(String text, String... styleClasses) {
        Badge badge = new Badge(text);
        badge.getStyleClass().addAll(styleClasses);
        return badge;
    }

    private static String skinName(Badge badge) {
        return badge.getSkin() == null ? "no skin" : badge.getSkin().getClass().getSimpleName();
    }

    private static String describe(Badge badge) {
        return Kit.paint(badge.getColor()) + " " + badge.getBadgeShape().name().toLowerCase(java.util.Locale.ROOT) + " "
                + (int) badge.getSize() + (badge.isOutlined() ? " outlined" : "") + (badge.getRingWidth() > 0 ? " ring "
                        + (int) badge.getRingWidth() : "") + " " + Kit.paint(badge.getTextFill());
    }

    private static String indicators(VBox schemeRoot) {
        return schemeRoot.lookupAll(".indicator").stream().map(node -> Kit.fill((Label) node))
                .collect(Collectors.joining(" "));
    }

    private static ImageView schemeImage(ColorScheme colorScheme, boolean reducedMotion, List<VBox> roots, List<Scene> scenes) {
        Label title = new Label(colorScheme == ColorScheme.DARK ? "Dark scheme" : "Light scheme");
        title.getStyleClass().add("scheme-title");
        Label muted = new Label("Styled by @media rules of scheme.css");
        muted.getStyleClass().add("scheme-muted");
        Label cardLabel = new Label("Card text");
        Button button = new Button("Button");
        button.setFocusTraversable(false);
        CheckBox checkBox = new CheckBox("Check");
        checkBox.setSelected(true);
        checkBox.setFocusTraversable(false);
        TextField field = new TextField("Text");
        field.setPrefColumnCount(5);
        field.setFocusTraversable(false);
        HBox controls = new HBox(8, button, checkBox, field);
        controls.setAlignment(Pos.CENTER_LEFT);
        ProgressBar progress = new ProgressBar(0.65);
        progress.setPrefWidth(270);
        VBox card = new VBox(cardLabel, controls, progress);
        card.getStyleClass().add("scheme-card");
        HBox accent = new HBox();
        accent.getStyleClass().add("scheme-accent");
        Label motion = indicator("reduced motion", "motion");
        Label combined = indicator("dark and reduced", "combined");
        Label lightOnly = indicator("not dark", "light-only");
        HBox indicators = new HBox(6, motion, combined, lightOnly);
        VBox schemeRoot = new VBox(title, muted, card, accent, indicators);
        schemeRoot.getStyleClass().add("scheme-root");

        Scene scene = new Scene(schemeRoot, SCENE_WIDTH, SCENE_HEIGHT);
        scene.getPreferences().setColorScheme(colorScheme);
        scene.getPreferences().setReducedMotion(reducedMotion);
        scene.getStylesheets().addAll(Kit.css("common.css"), Kit.css("scheme.css"));
        roots.add(schemeRoot);
        scenes.add(scene);
        ImageView view = new ImageView();
        view.setFitWidth(SCENE_WIDTH);
        view.setFitHeight(SCENE_HEIGHT);
        // a rendering failure of the off-screen scene is reported by the "scene snapshots" check, not as a build error
        try {
            WritableImage image = scene.snapshot(null);
            view.setImage(image);
        } catch (Throwable t) {
            view.getProperties().put(SNAPSHOT_ERROR, Checks.describe(t));
        }
        return view;
    }

    private static String snapshotSize(ImageView view) {
        Object error = view.getProperties().get(SNAPSHOT_ERROR);
        if (error != null) {
            return String.valueOf(error);
        }
        return view.getImage() == null ? "no image"
                : (int) view.getImage().getWidth() + "x" + (int) view.getImage().getHeight();
    }

    private static Label indicator(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("indicator", styleClass);
        return label;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Kit.ready(content);
    }
}
