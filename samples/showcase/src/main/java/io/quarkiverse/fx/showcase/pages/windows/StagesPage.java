package io.quarkiverse.fx.showcase.pages.windows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Real stages of every {@link StageStyle}, an owned window-modal stage, opacity and icons : opened side by side next to
 * the main window, captured, then closed. The page shows mock-ups of each window built from the same content.
 */
@Singleton
@SuppressWarnings("deprecation") // StageStyle.EXTENDED, HeaderBar and ConditionalFeature.EXTENDED_WINDOW are previews
public class StagesPage implements FeaturePage {

    static final double CONTENT_WIDTH = 240;
    static final double CONTENT_HEIGHT = 132;
    private static final String LIVE_TITLE = "Stages (shown, measured, captured, closed)";

    /**
     * A stage to open : its key (file-name safe), style and content factory.
     */
    record Spec(String key, String caption, StageStyle style, Modality modality, Supplier<Parent> content) {
    }

    private static List<Spec> specs() {
        return List.of(
                new Spec("decorated", "StageStyle.DECORATED · icons", StageStyle.DECORATED, Modality.NONE,
                        StagesPage::decoratedContent),
                new Spec("undecorated", "StageStyle.UNDECORATED", StageStyle.UNDECORATED, Modality.NONE,
                        StagesPage::undecoratedContent),
                new Spec("transparent", "StageStyle.TRANSPARENT · translucent", StageStyle.TRANSPARENT, Modality.NONE,
                        StagesPage::transparentContent),
                new Spec("utility", "StageStyle.UTILITY · opacity 0.9", StageStyle.UTILITY, Modality.NONE,
                        StagesPage::utilityContent),
                new Spec("unified", "StageStyle.UNIFIED", StageStyle.UNIFIED, Modality.NONE, StagesPage::unifiedContent),
                new Spec("extended", "StageStyle.EXTENDED (preview) · HeaderBar", StageStyle.EXTENDED, Modality.NONE,
                        () -> extendedContent(true)),
                new Spec("window-modal", "Owned · Modality.WINDOW_MODAL", StageStyle.DECORATED, Modality.WINDOW_MODAL,
                        StagesPage::modalContent));
    }

    @Override
    public String id() {
        return "windows-stages";
    }

    @Override
    public String title() {
        return "Stages & Stage Styles";
    }

    @Override
    public String category() {
        return Categories.WINDOWS;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        VBox root = WindowSupport.page(10);

        GridPane cards = new GridPane();
        cards.setHgap(14);
        cards.setVgap(8);
        int index = 0;
        for (Spec spec : specs()) {
            cards.add(WindowSupport.demo(spec.caption(), mock(spec)), index % 4, index / 4);
            index++;
        }
        cards.add(WindowSupport.demo("Stage icons · WindowEvent order", propertiesCard()), 3, 1);

        List<Check> checks = new ArrayList<>();
        checks.add(Check.info("ConditionalFeature",
                "TRANSPARENT_WINDOW=" + Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW)
                        + ", UNIFIED_WINDOW=" + Platform.isSupported(ConditionalFeature.UNIFIED_WINDOW)
                        + ", EXTENDED_WINDOW=" + Platform.isSupported(ConditionalFeature.EXTENDED_WINDOW)));
        checks.add(headerBarCheck());
        checks.add(WindowSupport.iconFontCheck());
        VBox live = WindowSupport.liveSection(root, LIVE_TITLE, checks,
                "Opens the 7 stages at fixed offsets from the main window, captures their scenes, closes them.",
                this::runLive);

        root.getChildren().addAll(cards, live);
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
    }

    /**
     * HeaderBar (preview) : created when preview features are enabled, refused otherwise. The flag is read by a static
     * initializer (com.sun.javafx.PreviewFeature), so the outcome also tells whether it was frozen at build time.
     */
    private static Check headerBarCheck() {
        try {
            HeaderBar bar = new HeaderBar();
            return Check.info("new HeaderBar() (preview)", "created, " + bar.getChildrenUnmodifiable().size()
                    + " children");
        } catch (RuntimeException e) {
            return Check.info("new HeaderBar() (preview)", "refused (" + e.getClass().getSimpleName() + "): "
                    + String.valueOf(e.getMessage()).lines().findFirst().orElse(""));
        }
    }

    private static List<Image> icons() {
        return List.of(new Image(Fx.resourceUrl("/showcase/images/icon.png")),
                new Image(Fx.resourceUrl("/showcase/images/icon-16.png")));
    }

    // ------------------------------------------------------------------------------------------------ stage contents

    private static Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("stage-title");
        return label;
    }

    private static Label text(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("stage-text");
        label.setWrapText(true);
        return label;
    }

    private static <T extends Region> T sized(T region) {
        region.setPrefSize(CONTENT_WIDTH, CONTENT_HEIGHT);
        region.setMinSize(CONTENT_WIDTH, CONTENT_HEIGHT);
        region.setMaxSize(CONTENT_WIDTH, CONTENT_HEIGHT);
        return region;
    }

    static Parent decoratedContent() {
        CheckBox remember = new CheckBox("Remember");
        remember.setSelected(true);
        HBox actions = new HBox(10, new Button("OK"), remember);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(title("Decorated stage"), text("Native title bar and border, resizable, with icons."),
                actions);
        box.getStyleClass().addAll("stage-content", "decorated-content");
        return sized(box);
    }

    static Parent undecoratedContent() {
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(new Label("Undecorated"), spacer, new Label("✕"));
        bar.getStyleClass().add("custom-title-bar");
        ProgressBar progress = new ProgressBar(0.6);
        progress.setMaxWidth(Double.MAX_VALUE);
        VBox body = new VBox(text("No OS decoration: custom title bar and border."),
                progress, text("Determinate progress: 60 %"));
        body.getStyleClass().add("body");
        VBox box = new VBox(bar, body);
        box.getStyleClass().addAll("stage-content", "undecorated-content");
        return sized(box);
    }

    static Parent transparentContent() {
        HBox dots = new HBox(6, new Circle(6, Color.web("#ff6b6b")), new Circle(6, Color.web("#ffd93d")),
                new Circle(6, Color.web("#6bcB77")), new Circle(6, Color.web("#4d96ff")));
        Label heading = title("Transparent stage");
        VBox glass = new VBox(heading, text("Scene fill TRANSPARENT, rounded translucent content."), dots);
        glass.getStyleClass().add("glass");
        StackPane box = new StackPane(glass);
        box.getStyleClass().addAll("transparent-content");
        return sized(box);
    }

    static Parent utilityContent() {
        TilePane tools = new TilePane(6, 6);
        tools.setPrefColumns(5);
        char[] glyphs = { '\uf015', '\uf004', '\uf013', '\uf002', '\uf0c7', '\uf1f8', '\uf03e', '\uf0e0', '\uf007',
                '\uf005' };
        for (int i = 0; i < glyphs.length; i++) {
            Label tool = WindowSupport.icon(glyphs[i], 15, "#e5e9f0");
            tool.getStyleClass().add("tool");
            if (i == 2) {
                tool.getStyleClass().add("selected");
            }
            tools.getChildren().add(tool);
        }
        Label heading = new Label("Tool palette");
        heading.setStyle("-fx-font-weight: bold;");
        VBox box = new VBox(6, heading, tools);
        box.getStyleClass().addAll("utility-content");
        return sized(box);
    }

    static Parent unifiedContent() {
        ToolBar toolBar = new ToolBar(new Button("Back"), new Button("Forward"), new Separator(), new Button("Share"));
        VBox center = new VBox(title("Unified stage"), text("On macOS the title bar and the toolbar share one "
                + "background (the scene fill is transparent)."));
        center.getStyleClass().add("stage-content");
        BorderPane box = new BorderPane(center);
        box.setTop(toolBar);
        box.getStyleClass().add("unified-content");
        return sized(box);
    }

    /**
     * With {@code real}, the header is a {@link HeaderBar} (preview API, only when preview features are enabled);
     * otherwise a plain box of the same look (mock-up).
     */
    static Parent extendedContent(boolean real) {
        Label label = new Label("Extended · HeaderBar");
        Node header;
        if (real) {
            HeaderBar bar = new HeaderBar();
            bar.setCenter(label);
            bar.getStyleClass().add("header-bar-area");
            header = bar;
        } else {
            HBox bar = new HBox(label);
            bar.setAlignment(Pos.CENTER);
            bar.getStyleClass().add("header-area");
            header = bar;
        }
        VBox center = new VBox(text("The scene extends into the title bar: the application provides the header, "
                + "the OS draws the window buttons over it."));
        center.getStyleClass().add("stage-content");
        BorderPane box = new BorderPane(center);
        box.setTop(header);
        box.getStyleClass().add("extended-content");
        return sized(box);
    }

    static Parent modalContent() {
        VBox box = new VBox(title("Window-modal stage"), text("Owned by the main window: blocks input to its owner "
                + "only, stays on top of it."), new Button("Close"));
        box.getStyleClass().addAll("stage-content", "modal-content");
        return sized(box);
    }

    // ---------------------------------------------------------------------------------------------------- mock-ups

    private static HBox titleBar(String title, boolean utility) {
        double radius = utility ? 4 : 5.5;
        HBox lights = new HBox(utility ? 4 : 6, new Circle(radius, Color.web("#ff5f57")),
                new Circle(radius, Color.web("#febc2e")), new Circle(radius, Color.web("#28c840")));
        lights.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(title);
        StackPane centered = new StackPane(label);
        HBox.setHgrow(centered, Priority.ALWAYS);
        Pane balance = new Pane();
        balance.setPrefWidth(utility ? 36 : 50);
        HBox bar = new HBox(lights, centered, balance);
        bar.getStyleClass().add("mock-title-bar");
        if (utility) {
            bar.getStyleClass().add("utility");
        }
        return bar;
    }

    private static Node framed(String title, boolean utility, Parent content) {
        VBox window = new VBox(titleBar(title, utility), content);
        window.getStyleClass().add("mock-window");
        window.setPadding(new Insets(0, 1, 1, 1));
        window.setMaxWidth(Region.USE_PREF_SIZE);
        return window;
    }

    private static Node mock(Spec spec) {
        return switch (spec.key()) {
            case "decorated" -> framed("Decorated", false, decoratedContent());
            case "undecorated" -> undecoratedContent();
            case "transparent" -> {
                // a checkerboard behind the translucent content, as a desktop would be
                Rectangle board = new Rectangle(CONTENT_WIDTH, CONTENT_HEIGHT, new ImagePattern(checker(), 0, 0, 16, 16,
                        false));
                board.setArcWidth(12);
                board.setArcHeight(12);
                StackPane stack = new StackPane(board, transparentContent());
                stack.setMaxWidth(Region.USE_PREF_SIZE);
                yield stack;
            }
            case "utility" -> {
                Node window = framed("Tools", true, utilityContent());
                window.setOpacity(0.9);
                yield window;
            }
            case "unified" -> {
                Parent content = unifiedContent();
                HBox bar = titleBar("Unified", false);
                bar.setStyle("-fx-background-color: linear-gradient(#e9e9ed, #e3e3e8);");
                VBox window = new VBox(bar, content);
                window.getStyleClass().add("mock-window");
                window.setPadding(new Insets(0, 1, 1, 1));
                window.setMaxWidth(Region.USE_PREF_SIZE);
                yield window;
            }
            case "extended" -> {
                Parent content = extendedContent(false);
                HBox lights = new HBox(6, new Circle(5.5, Color.web("#ff5f57")), new Circle(5.5, Color.web("#febc2e")),
                        new Circle(5.5, Color.web("#28c840")));
                lights.setPadding(new Insets(9, 0, 0, 10));
                lights.setMouseTransparent(true);
                StackPane stack = new StackPane(content, lights);
                StackPane.setAlignment(lights, Pos.TOP_LEFT);
                lights.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                VBox window = new VBox(stack);
                window.getStyleClass().add("mock-window");
                window.setPadding(new Insets(1));
                window.setMaxWidth(Region.USE_PREF_SIZE);
                yield window;
            }
            case "window-modal" -> {
                Rectangle owner = new Rectangle(CONTENT_WIDTH + 2, CONTENT_HEIGHT + 24, Color.web("#d9dde3"));
                owner.setArcWidth(12);
                owner.setArcHeight(12);
                Label ownerLabel = new Label("main window (blocked)");
                ownerLabel.getStyleClass().add("demo-note");
                StackPane.setAlignment(ownerLabel, Pos.TOP_LEFT);
                StackPane.setMargin(ownerLabel, new Insets(4, 0, 0, 8));
                Node modal = framed("Window-modal", false, modalContent());
                modal.setScaleX(0.8);
                modal.setScaleY(0.8);
                StackPane.setAlignment(modal, Pos.BOTTOM_RIGHT);
                StackPane stack = new StackPane(owner, ownerLabel, new Group(modal));
                StackPane.setAlignment(stack.getChildren().get(2), Pos.BOTTOM_RIGHT);
                stack.setMaxWidth(Region.USE_PREF_SIZE);
                yield stack;
            }
            default -> throw new IllegalArgumentException(spec.key());
        };
    }

    private static Image checker() {
        WritableImage image = new WritableImage(16, 16);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                writer.setArgb(x, y, ((x / 8) + (y / 8)) % 2 == 0 ? 0xFFE8EBF0 : 0xFFC4CAD3);
            }
        }
        return image;
    }

    private static Node propertiesCard() {
        HBox icons = new HBox(12);
        icons.setAlignment(Pos.CENTER_LEFT);
        for (Image icon : icons()) {
            ImageView view = new ImageView(icon);
            Label size = new Label(WindowSupport.size(icon.getWidth(), icon.getHeight()));
            size.getStyleClass().add("demo-note");
            VBox box = new VBox(2, view, size);
            box.setAlignment(Pos.BOTTOM_CENTER);
            box.setMinWidth(Region.USE_PREF_SIZE);
            icons.getChildren().add(box);
        }
        Label iconsNote = WindowSupport.note("stage.getIcons() of the decorated stage (macOS does not draw them)");
        HBox.setHgrow(iconsNote, Priority.ALWAYS);
        icons.getChildren().add(iconsNote);
        VBox card = new VBox(6, icons,
                WindowSupport.note("WindowEvents: SHOWING, SHOWN, CLOSE_REQUEST (vetoed), CLOSE_REQUEST, HIDING, "
                        + "HIDDEN"));
        card.getStyleClass().add("launcher-box");
        card.setPadding(new Insets(8));
        card.setPrefWidth(CONTENT_WIDTH + 2);
        card.setMaxWidth(CONTENT_WIDTH + 2);
        return card;
    }

    // -------------------------------------------------------------------------------------------------- live checks

    private CompletionStage<Void> runLive(Node content) {
        Stage main = WindowSupport.mainStage(content);
        WindowSupport.Live live = WindowSupport.startLive(content);
        if (main == null) {
            live.checks.add(Check.fail("main window", "page not showing"));
            WindowSupport.publish(content, live);
            return CompletableFuture.completedFuture(null);
        }
        // ready() is invoked right after build(): let the page get its skins and layout first
        return Fx.pulses(3).thenCompose(v -> openStages(content, main, live));
    }

    private CompletionStage<Void> openStages(Node content, Stage main, WindowSupport.Live live) {
        boolean mainFocused = main.isFocused();
        Set<Window> before = WindowSupport.showingWindows();
        Map<Spec, Stage> stages = new LinkedHashMap<>();
        Map<Spec, double[]> offsets = new LinkedHashMap<>();
        List<String> events = new ArrayList<>();
        int[] closeRequests = { 0 };

        int index = 0;
        for (Spec spec : specs()) {
            double x = main.getX() + 300 + (index % 4) * 270;
            double y = main.getY() + 110 + (index / 4) * 230;
            index++;
            if (spec.style() == StageStyle.EXTENDED) {
                if (!Platform.isSupported(ConditionalFeature.EXTENDED_WINDOW)) {
                    live.checks.add(Check.info(spec.caption(), "EXTENDED_WINDOW not supported"));
                    continue;
                }
                try {
                    Stage probe = new Stage(StageStyle.EXTENDED);
                    probe.hide();
                } catch (RuntimeException e) {
                    String message = String.valueOf(e.getMessage()).lines().findFirst().orElse("");
                    live.checks.add(Check.info(spec.caption(), "refused (preview features disabled): " + message));
                    continue;
                }
            }
            try {
                Stage stage = new Stage(spec.style());
                stage.setTitle(spec.caption());
                Scene scene = new Scene(WindowSupport.styled(spec.content().get()));
                switch (spec.key()) {
                    case "decorated" -> {
                        stage.getIcons().setAll(icons());
                        stage.addEventHandler(WindowEvent.ANY, e -> events.add(e.getEventType().getName()));
                        // the first close request is vetoed, the second one closes the stage
                        stage.setOnCloseRequest(e -> {
                            if (closeRequests[0]++ == 0) {
                                e.consume();
                            }
                        });
                    }
                    case "transparent", "unified" -> scene.setFill(Color.TRANSPARENT);
                    case "extended" -> scene.setFill(Color.WHITE);
                    case "utility" -> {
                        stage.setOpacity(0.9);
                        stage.setResizable(false);
                        stage.setAlwaysOnTop(true);
                    }
                    case "window-modal" -> {
                        stage.initOwner(main);
                        stage.initModality(spec.modality());
                    }
                    default -> {
                    }
                }
                stage.setScene(scene);
                stage.sizeToScene();
                stage.setX(x);
                stage.setY(y);
                stage.show();
                WindowSupport.shield(stage);
                live.opened.add(stage);
                stages.put(spec, stage);
                offsets.put(spec, new double[] { x - main.getX(), y - main.getY() });
            } catch (Throwable t) {
                live.checks.add(Check.fail(spec.caption(), Checks.describe(t)));
            }
        }

        return Fx.pulses(4).thenCompose(v -> {
            stages.values().forEach(stage -> WindowSupport.stabilize(stage.getScene()));
            return Fx.pulses(3);
        }).thenCompose(v -> Fx.delay(150)).thenAccept(v -> {
            for (Map.Entry<Spec, Stage> entry : stages.entrySet()) {
                Spec spec = entry.getKey();
                Stage stage = entry.getValue();
                try {
                    live.checks.add(stageCheck(spec, stage, main, offsets.get(spec)));
                    live.images.put(spec.key(), WindowSupport.capture(stage.getScene()));
                } catch (Throwable t) {
                    live.checks.add(Check.fail(spec.caption(), Checks.describe(t)));
                }
            }
            long listed = stages.values().stream().filter(s -> Window.getWindows().contains(s)).count();
            long newWindows = Window.getWindows().stream().filter(w -> !before.contains(w) && w instanceof Stage).count();
            live.checks.add(Check.of("Window.getWindows() while open", listed == stages.size() && newWindows == listed,
                    listed + " of " + stages.size() + " opened stages listed, " + newWindows + " new stages"));

            stages.entrySet().stream().filter(e -> e.getKey().key().equals("decorated")).map(Map.Entry::getValue)
                    .findFirst().ifPresent(stage -> live.checks.add(lifecycleCheck(stage)));
            stages.values().forEach(Stage::close);
            long stillShowing = stages.values().stream().filter(Window::isShowing).count();
            long stillListed = stages.values().stream().filter(s -> Window.getWindows().contains(s)).count();
            live.checks.add(Check.of("After close()", stillShowing == 0 && stillListed == 0,
                    stillShowing + " showing, " + stillListed + " listed"));
            live.checks.add(Checks.expect("WindowEvents (decorated)",
                    "SHOWING SHOWN CLOSE_REQUEST CLOSE_REQUEST HIDING HIDDEN",
                    () -> String.join(" ", events).replace("WINDOW_", "")));
        }).thenRun(() -> {
            live.closeAll();
            if (mainFocused && !main.isFocused()) {
                main.requestFocus();
            }
            WindowSupport.publish(content, live);
        });
    }

    /**
     * On a showing stage : initStyle() is refused, a consumed close request keeps it open, an unconsumed one (handled
     * by the default WindowCloseRequestHandler) closes it.
     */
    private static Check lifecycleCheck(Stage stage) {
        List<String> steps = new ArrayList<>();
        try {
            stage.initStyle(StageStyle.UTILITY);
            steps.add("initStyle accepted");
        } catch (IllegalStateException e) {
            steps.add("initStyle refused");
        }
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        boolean vetoed = stage.isShowing();
        steps.add("close request " + (vetoed ? "vetoed" : "NOT vetoed"));
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        boolean closed = !stage.isShowing();
        steps.add("close request " + (closed ? "closed it" : "did NOT close it"));
        return Check.of("Showing stage lifecycle (decorated)", steps.get(0).equals("initStyle refused") && vetoed
                && closed, String.join(", ", steps));
    }

    /**
     * The stage must be showing with its style, at the requested offset from the main window and with the size of its
     * scene content. Position and size are compared give or take one device pixel (fractional scales of Windows) and
     * are only informational on Linux, where the window manager may place windows itself (see
     * {@link WindowSupport#placement(boolean)}).
     */
    private static Check stageCheck(Spec spec, Stage stage, Stage main, double[] offset) {
        Scene scene = stage.getScene();
        boolean sizeOk = WindowSupport.near(scene.getWidth(), CONTENT_WIDTH, stage)
                && WindowSupport.near(scene.getHeight(), CONTENT_HEIGHT, stage);
        double dx = stage.getX() - main.getX();
        double dy = stage.getY() - main.getY();
        boolean positionOk = WindowSupport.near(dx, offset[0], stage) && WindowSupport.near(dy, offset[1], stage);
        StringBuilder value = new StringBuilder();
        value.append(stage.isShowing() ? "showing" : "NOT showing").append(", ").append(stage.getStyle())
                .append(", at +").append(WindowSupport.fmt(dx)).append(",+").append(WindowSupport.fmt(dy))
                .append(positionOk ? "" : " (requested +" + WindowSupport.fmt(offset[0]) + ",+"
                        + WindowSupport.fmt(offset[1]) + ")")
                .append(", scene ").append(WindowSupport.size(scene.getWidth(), scene.getHeight()))
                .append(sizeOk ? "" : " (requested " + WindowSupport.size(CONTENT_WIDTH, CONTENT_HEIGHT) + ")")
                .append(", frame +").append(WindowSupport.fmt(stage.getWidth() - scene.getWidth())).append("x+")
                .append(WindowSupport.fmt(stage.getHeight() - scene.getHeight()));
        boolean ok = stage.isShowing() && stage.getStyle() == spec.style();
        switch (spec.key()) {
            case "decorated" -> {
                value.append(", icons ").append(stage.getIcons().stream()
                        .map(i -> WindowSupport.size(i.getWidth(), i.getHeight()) + (i.isError() ? " ERROR" : ""))
                        .toList());
                ok &= stage.getIcons().size() == 2 && stage.getIcons().stream().noneMatch(Image::isError);
            }
            case "transparent" -> {
                value.append(", fill ").append(scene.getFill());
                if (!Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW)) {
                    // Linux without a compositing window manager : the window is shown opaque (not a failure)
                    value.append(" (TRANSPARENT_WINDOW not supported: opaque window)");
                }
            }
            case "utility" -> {
                value.append(", opacity ").append(stage.getOpacity()).append(", alwaysOnTop ")
                        .append(stage.isAlwaysOnTop()).append(", resizable ").append(stage.isResizable());
                ok &= stage.getOpacity() == 0.9 && stage.isAlwaysOnTop() && !stage.isResizable();
            }
            case "unified" -> value.append(", supported ").append(Platform.isSupported(ConditionalFeature.UNIFIED_WINDOW));
            case "window-modal" -> {
                value.append(", owner ").append(stage.getOwner() == main ? "main window" : String.valueOf(stage.getOwner()))
                        .append(", ").append(stage.getModality());
                ok &= stage.getOwner() == main && stage.getModality() == Modality.WINDOW_MODAL;
            }
            default -> {
            }
        }
        return new Check(spec.caption(), value.toString(),
                ok ? WindowSupport.placement(sizeOk && positionOk) : Boolean.FALSE);
    }
}
