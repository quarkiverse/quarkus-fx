package io.quarkiverse.fx.showcase.pages.fxml;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;

/**
 * An FXML form loaded with the {@code FXMLLoader} produced by quarkus-fx (CDI controller factory), and isolated
 * probes : one small FXML file per FXML feature, so that a feature failing in native mode is pinpointed.
 */
@Singleton
public class FxmlLoaderPage implements FeaturePage {

    private static final String BUNDLE = "showcase.fxml.LoaderMessages";
    private static final String READY = "fxml-loader.ready";

    /** Page content width (page frame minus its padding). */
    private static final double CONTENT_WIDTH = io.quarkiverse.fx.showcase.core.MainView.PAGE_WIDTH - 32;
    private static final double MAIN_WIDTH = 460;
    private static final double TILE_CONTENT_WIDTH = 112;
    private static final double TILE_CONTENT_HEIGHT = 50;

    @Inject
    Instance<FXMLLoader> loaders;

    private record Probe(String file, String title, Function<Map<String, Object>, Object> describe) {
    }

    private static final List<Probe> PROBES = List.of(
            new Probe("named-color", "Color @NamedArg", ns -> fill(ns, "subject") + " " + fill(ns, "web") + " "
                    + fill(ns, "rgba")),
            new Probe("insets", "Insets", ns -> ((Label) ns.get("subject")).getPadding() + " / uniform "
                    + ((Label) ns.get("uniform")).getPadding().getTop() + ", margin "
                    + VBox.getMargin((Node) ns.get("uniform")).getLeft()),
            new Probe("font", "Font", ns -> fontOf(ns, "subject") + " | " + fontOf(ns, "serif") + " | "
                    + fontOf(ns, "defaultFont")),
            new Probe("linear-gradient", "LinearGradient + Stop", ns -> fill(ns, "subject")),
            new Probe("radial-gradient", "RadialGradient", ns -> fill(ns, "subject")),
            new Probe("rectangle2d", "Image + Rectangle2D", ns -> {
                ImageView view = (ImageView) ns.get("subject");
                ImageView photo = (ImageView) ns.get("photo");
                return view.getViewport() + " / pattern " + size(view) + " error=" + view.getImage().isError()
                        + " / photo " + size(photo) + " error=" + photo.getImage().isError();
            }),
            new Probe("background", "Background + Border", ns -> {
                Pane pane = (Pane) ns.get("subject");
                var fills = pane.getBackground().getFills();
                var stroke = pane.getBorder().getStrokes().getFirst();
                return fills.size() + " fills " + fills.get(0).getFill() + " r=" + fills.get(0).getRadii()
                        .getTopLeftHorizontalRadius() + " inset=" + fills.get(1).getInsets().getLeft() + ", border "
                        + stroke.getTopStroke() + " dashes=" + stroke.getTopStyle().getDashArray() + " w="
                        + stroke.getWidths().getTop();
            }),
            new Probe("constant", "fx:constant", ns -> "maxWidth==Double.MAX_VALUE: "
                    + (((Button) ns.get("subject")).getMaxWidth() == Double.MAX_VALUE) + ", "
                    + ((Label) ns.get("coral")).getTextFill() + ", " + ((Label) ns.get("coral")).getAlignment()),
            new Probe("factory", "fx:factory", ns -> {
                ChoiceBox<?> choice = (ChoiceBox<?>) ns.get("subject");
                return choice.getItems() + " value=" + choice.getValue() + " label='"
                        + ((Label) ns.get("bound")).getText() + "'";
            }),
            new Probe("define-reference", "fx:define / fx:reference", ns -> "same instance: "
                    + (((Shape) ns.get("subject")).getFill() == ns.get("brand")) + ", caption='"
                    + ((Label) ns.get("captionLabel")).getText() + "', escaped='"
                    + ((Label) ns.get("escaped")).getText() + "'"),
            new Probe("copy", "fx:root + fx:copy", ns -> {
                ColorSwatch original = (ColorSwatch) ns.get("subject");
                ColorSwatch copy = (ColorSwatch) ns.get("copy");
                return original.getSwatchName() + " " + original.getSwatchColor() + " -> " + copy.getSwatchName() + " "
                        + copy.getSwatchColor() + ", distinct=" + (original != copy) + ", label='"
                        + copy.nameLabel().getText() + "'";
            }),
            new Probe("include", "fx:include", ns -> {
                BadgeController controller = (BadgeController) ns.get("innerController");
                return ns.get("inner").getClass().getSimpleName() + " + " + controller.getClass().getSimpleName()
                        + " initialized=" + controller.initialized + " text='" + controller.badgeText.getText()
                        + "' cdi='" + controller.greeting() + "'";
            }),
            new Probe("expression", "${expression}", ns -> {
                Label result = (Label) ns.get("result");
                return "progress=" + ((ProgressBar) ns.get("subject")).getProgress() + " text='" + result.getText()
                        + "' visible=" + result.isVisible() + " disable=" + result.isDisable();
            }),
            new Probe("static-props", "Static properties", ns -> {
                Label subject = (Label) ns.get("subject");
                return "row=" + GridPane.getRowIndex(subject) + " span=" + GridPane.getColumnSpan(subject)
                        + " halign=" + GridPane.getHalignment(subject) + " alignment=" + subject.getAlignment()
                        + " hgrow=" + HBox.getHgrow((Node) ns.get("grow"));
            }),
            new Probe("effects", "Effects & transforms", ns -> {
                DropShadow shadow = (DropShadow) ((Node) ns.get("subject")).getEffect();
                Rectangle rotated = (Rectangle) ns.get("rotated");
                Rectangle clipped = (Rectangle) ns.get("clipped");
                return "DropShadow r=" + shadow.getRadius() + " offset=" + shadow.getOffsetX() + "," + shadow.getOffsetY()
                        + " " + shadow.getColor() + ", Rotate " + ((Rotate) rotated.getTransforms().getFirst()).getAngle()
                        + ", clip " + clipped.getClip().getClass().getSimpleName() + " r="
                        + ((Circle) clipped.getClip()).getRadius() + ", "
                        + clipped.getEffect().getClass().getSimpleName();
            }),
            new Probe("resources", "%resources", ns -> ((Label) ns.get("subject")).getText() + " | "
                    + ((Label) ns.get("escaped")).getText() + " | " + ((Label) ns.get("arabic")).getText()),
            new Probe("table", "PropertyValueFactory", ns -> {
                TableView<?> table = (TableView<?>) ns.get("subject");
                return table.getItems().size() + " rows, " + table.getColumns().get(0).getCellData(0) + "/"
                        + table.getColumns().get(1).getCellData(0) + ", " + table.getColumns().get(0).getCellData(1)
                        + "/" + table.getColumns().get(1).getCellData(1);
            }),
            new Probe("values", "fx:value / list coercion", ns -> {
                Polygon polygon = (Polygon) ns.get("subject");
                Polyline polyline = (Polyline) ((HBox) polygon.getParent()).getChildren().get(1);
                return polygon.getPoints() + ", polyline " + polyline.getPoints();
            }));

    @Override
    public String id() {
        return "fxml-loader";
    }

    @Override
    public String title() {
        return "FXMLLoader & CDI controllers";
    }

    @Override
    public String category() {
        return Categories.FXML;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        List<Check> checks = new ArrayList<>();
        ResourceBundle bundle = FxmlUi.bundle(BUNDLE, checks, "ResourceBundle.getBundle");
        checks.add(Checks.expect("ResourceBundle Locale.FRENCH", "Bonjour, bundle / Name",
                () -> {
                    ResourceBundle french = ResourceBundle.getBundle(BUNDLE, Locale.FRENCH);
                    return french.getString("probe.hello") + " / " + french.getString("loader.col.name");
                }));

        VBox mainBox = FxmlUi.demo("loader.fxml loaded by Instance<FXMLLoader>.get() (CDI controller factory)");
        mainBox.setPrefWidth(MAIN_WIDTH);
        mainBox.setMinWidth(MAIN_WIDTH);
        mainBox.setMaxWidth(MAIN_WIDTH);
        LoaderDemoController controller = null;
        Parent mainRoot = null;
        boolean factory = false;
        try {
            FXMLLoader loader = loaders.get();
            factory = loader.getControllerFactory() != null;
            loader.setLocation(Fx.resource("/showcase/fxml/loader.fxml"));
            loader.setResources(bundle);
            mainRoot = loader.load();
            controller = loader.getController();
            mainBox.getChildren().add(mainRoot);
        } catch (Throwable t) {
            checks.add(Check.fail("load loader.fxml", FxmlUi.describe(t)));
            Label error = new Label("loader.fxml failed to load: " + FxmlUi.describe(t));
            error.getStyleClass().add("load-error");
            error.setWrapText(true);
            error.setMaxWidth(MAIN_WIDTH - 20);
            mainBox.getChildren().add(error);
        }
        if (controller != null) {
            mainChecks(controller, mainRoot, factory, checks);
        }

        VBox probes = probes(bundle);

        HBox top = new HBox(12, mainBox, probes);
        double half = (CONTENT_WIDTH - 12) / 2;
        int split = (checks.size() + 1) / 2;
        VBox leftColumn = new VBox(6, FxmlUi.checks("Loader checks", new ArrayList<>(checks.subList(0, split)), 190, half));
        VBox rightColumn = new VBox(6, FxmlUi.checks(" ", new ArrayList<>(checks.subList(split, checks.size())), 190, half));
        HBox checksRow = new HBox(12, leftColumn, rightColumn);
        VBox root = FxmlUi.page(10, top, checksRow);

        LoaderDemoController c = controller;
        CompletableFuture<Void> ready = whenShown(root, () -> {
            if (c != null) {
                rightColumn.getChildren().add(FxmlUi.checks("After CSS & layout", lateChecks(c), 190, half));
            }
        });
        root.getProperties().put(READY, ready);
        return root;
    }

    private static void mainChecks(LoaderDemoController c, Parent root, boolean factory, List<Check> checks) {
        // interactions : the bindings and handlers declared in FXML react
        c.nameField.setText("Quarkus");
        c.incrementButton.fire();
        c.incrementButton.fire();

        checks.add(Checks.expect("controller from the CDI factory", "LoaderDemoController + @Inject GreetingService",
                () -> (factory ? "" : "no controller factory, ") + c.getClass().getSimpleName()
                        + (c.greetings != null ? " + @Inject GreetingService" : ", no CDI injection")));
        checks.add(Checks.expect("initialize() + @FXML fields", "initialized, 18/18 fields", () -> {
            List<Object> fields = new ArrayList<>(List.of(c.nameField, c.greetingLabel, c.serviceLabel,
                    c.incrementButton, c.resetButton, c.wideButton, c.enableBox, c.sizeGroup, c.languageBox,
                    c.levelSlider, c.levelBar, c.clicksLabel, c.peopleTable, c.primarySwatch, c.badge,
                    c.badgeController, c.resources));
            fields.add(c.location);
            return (c.initialized ? "initialized, " : "not initialized, ")
                    + fields.stream().filter(Objects::nonNull).count() + "/" + fields.size() + " fields";
        }));
        checks.add(Checks.expect("resources / location fields", "FXML loaded by the CDI FXMLLoader, loader.fxml",
                () -> c.resources.getString("loader.title") + ", "
                        + c.location.getPath().substring(c.location.getPath().lastIndexOf('/') + 1)));
        checks.add(Checks.expect("fx:include nested controller", "BadgeController owner=LoaderDemoController, Hello, include!",
                () -> c.badgeController.getClass().getSimpleName() + " owner=" + c.badgeController.owner + ", "
                        + c.badgeController.greeting()));
        checks.add(Checks.expect("${'Hello, ' + nameField.text + '!'}", "Hello, Quarkus!",
                () -> c.greetingLabel.getText()));
        checks.add(Checks.expect("${levelSlider.value / 100}", 0.7, () -> c.levelBar.getProgress()));
        checks.add(Checks.expect("#increment x2 + ${controller.clickText}", "Clicked 2 times / 2 calls from incrementButton",
                () -> c.clicksLabel.getText() + " / " + c.handlerCalls + " calls from " + c.lastEventSource));
        checks.add(Checks.expect("${!enableBox.selected}", true, () -> c.resetButton.isDisable()));
        checks.add(Checks.expect("fx:define ToggleGroup via $sizeGroup", "3 toggles, selected medium",
                () -> c.sizeGroup.getToggles().size() + " toggles, selected "
                        + c.sizeGroup.getSelectedToggle().getUserData()));
        checks.add(Checks.expect("fx:factory list via $languages", "[Java, Kotlin, Scala] value=Kotlin",
                () -> c.languageBox.getItems() + " value=" + c.languageBox.getValue()));
        checks.add(Checks.expect("fx:root components + fx:copy", "Primary 0x1e88e5ff | Accent 0x8f24abff | Primary (copy) 0x186db7ff",
                () -> swatches(root).stream()
                        .map(s -> s.nameLabel().getText() + " " + s.chip().getFill())
                        .collect(Collectors.joining(" | "))));
        checks.add(Checks.expect("fx:constant Double / TableView", "MAX_VALUE: true, FLEX_LAST_COLUMN: true",
                () -> "MAX_VALUE: " + (c.wideButton.getMaxWidth() == Double.MAX_VALUE) + ", FLEX_LAST_COLUMN: "
                        + (c.peopleTable.getColumnResizePolicy() == TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN)));
        checks.add(Checks.expect("PropertyValueFactory (FXML Person items)", "3 rows, Grace Hopper, 36, Cryptanalyst",
                () -> {
                    List<TableColumn<Person, ?>> columns = c.peopleTable.getColumns();
                    return c.peopleTable.getItems().size() + " rows, " + columns.get(0).getCellData(2) + ", "
                            + columns.get(1).getCellData(0) + ", " + columns.get(2).getCellData(1);
                }));
        checks.add(Checks.expect("stylesheets=\"@loader.css\"", "1 stylesheet, loader.css",
                () -> root.getStylesheets().size() + " stylesheet, "
                        + root.getStylesheets().getFirst().substring(root.getStylesheets().getFirst().lastIndexOf('/') + 1)));
    }

    private static List<Check> lateChecks(LoaderDemoController c) {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("looked-up color (-loader-accent)", "0x1565c0ff", () -> c.greetingLabel.getTextFill().toString()));
        checks.add(Checks.expect("included badge styled by loader.css", "0xfff8e1ff",
                () -> c.badge.getBackground().getFills().getFirst().getFill().toString()));
        checks.add(Checks.run("MAX_VALUE button width (HBox.hgrow)", () -> c.wideButton.getWidth()));
        return checks;
    }

    private static List<ColorSwatch> swatches(Node node) {
        List<ColorSwatch> result = new ArrayList<>();
        if (node instanceof ColorSwatch swatch) {
            result.add(swatch);
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                result.addAll(swatches(child));
            }
        }
        return result;
    }

    private VBox probes(ResourceBundle bundle) {
        GridPane grid = new GridPane(6, 6);
        int index = 0;
        for (Probe probe : PROBES) {
            grid.add(tile(probe, bundle), index % 4, index / 4);
            index++;
        }
        VBox box = FxmlUi.demo("FXML probes : one file per feature, each loaded by its own CDI FXMLLoader", grid);
        return box;
    }

    private VBox tile(Probe probe, ResourceBundle bundle) {
        Check check;
        Node content;
        try {
            FXMLLoader loader = loaders.get();
            loader.setLocation(Fx.resource("/showcase/fxml/probes/" + probe.file() + ".fxml"));
            loader.setResources(bundle);
            content = loader.load();
            Check described = Checks.run("probe " + probe.file(), () -> probe.describe().apply(loader.getNamespace()));
            check = new Check(described.name(), FxmlUi.sanitize(described.value()), described.ok());
        } catch (Throwable t) {
            check = Check.fail("probe " + probe.file(), FxmlUi.describe(t));
            Label error = new Label(FxmlUi.describe(t));
            error.getStyleClass().add("probe-error");
            error.setWrapText(true);
            error.setMaxWidth(TILE_CONTENT_WIDTH);
            content = error;
        }
        boolean ok = Boolean.TRUE.equals(check.ok());
        Label status = new Label(ok ? "✔" : "✘");
        status.getStyleClass().add(ok ? "probe-status-ok" : "probe-status-failed");
        Label title = new Label(probe.title());
        title.getStyleClass().add("probe-title");
        HBox header = new HBox(4, status, title);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane holder = new StackPane(content);
        holder.setAlignment(Pos.TOP_LEFT);
        holder.setPrefSize(TILE_CONTENT_WIDTH, TILE_CONTENT_HEIGHT);
        holder.setMinSize(TILE_CONTENT_WIDTH, TILE_CONTENT_HEIGHT);
        holder.setMaxSize(TILE_CONTENT_WIDTH, TILE_CONTENT_HEIGHT);
        holder.setClip(new Rectangle(TILE_CONTENT_WIDTH, TILE_CONTENT_HEIGHT));

        VBox tile = new VBox(3, header, holder);
        tile.getStyleClass().add("probe-tile");
        if (!ok) {
            tile.getStyleClass().add("probe-failed");
        }
        Checks.attach(tile, List.of(check));
        return tile;
    }

    private static String fill(Map<String, Object> ns, String id) {
        return String.valueOf(((Shape) ns.get(id)).getFill());
    }

    private static String fontOf(Map<String, Object> ns, String id) {
        var font = ((Label) ns.get(id)).getFont();
        return font.getName() + " " + font.getSize();
    }

    private static String size(ImageView view) {
        return (int) view.getImage().getWidth() + "x" + (int) view.getImage().getHeight();
    }

    /**
     * Runs {@code action} once {@code content} is shown and laid out.
     */
    static CompletableFuture<Void> whenShown(Node content, Runnable action) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Fx.when(content.sceneProperty(), Objects::nonNull)
                .thenCompose(scene -> Fx.pulses(2))
                .whenComplete((v, error) -> {
                    if (error != null) {
                        done.completeExceptionally(error);
                        return;
                    }
                    try {
                        action.run();
                        done.complete(null);
                    } catch (Throwable t) {
                        done.completeExceptionally(t);
                    }
                });
        return done;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        Object ready = content.getProperties().get(READY);
        return ready instanceof CompletionStage<?> stage ? stage : CompletableFuture.completedFuture(null);
    }

}
