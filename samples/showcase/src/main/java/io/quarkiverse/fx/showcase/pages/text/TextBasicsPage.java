package io.quarkiverse.fx.showcase.pages.text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms.Families;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;

/**
 * Text node basics : fonts, weights, postures, decorations, wrapping, alignment, line spacing, bounds types, smoothing,
 * stroke and paint, CSS styling, and Label overrun styles.
 */
@Singleton
public class TextBasicsPage implements FeaturePage {

    static final String[] WEIGHT_NAMES = { "Thin", "XLight", "Light", "Normal", "Medium", "SemiBold", "Bold", "XBold",
            "Black" };
    static final String PARAGRAPH = "JavaFX lays out this paragraph inside a wrapping width of 228 pixels, "
            + "then aligns every line.";
    static final String OVERRUN_TEXT = "The quick brown fox jumps over the lazy dog";

    @Override
    public String id() {
        return "text-basics";
    }

    @Override
    public String title() {
        return "Text Basics";
    }

    @Override
    public String category() {
        return Categories.TEXT;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        List<Check> checks = new ArrayList<>();
        // "Helvetica Neue", "Avenir Next", "Georgia", "Times New Roman" and "Menlo" on macOS
        String sans = Families.sans();
        String georgia = PageFonts.georgia();
        String serif = Families.serif();
        String mono = Families.mono();
        String[] families = { "System", sans, PageFonts.avenir() };

        // Row 1 : weights and sizes
        GridPane weights = new GridPane();
        weights.setHgap(10);
        weights.setVgap(2);
        for (int row = 0; row < families.length; row++) {
            String family = families[row];
            Text name = new Text(family);
            name.setFont(Font.font("System", FontWeight.BOLD, 11));
            name.setFill(Color.web("#37474f"));
            weights.add(name, 0, row);
            List<String> styles = new ArrayList<>();
            FontWeight[] values = FontWeight.values();
            for (int col = 0; col < values.length; col++) {
                Font font = Font.font(family, values[col], 14);
                Text sample = new Text(WEIGHT_NAMES[col]);
                sample.setFont(font);
                weights.add(sample, col + 1, row);
                styles.add(font.getStyle());
            }
            checks.add(Check.info(family + " THIN..BLACK styles", String.join(", ", styles)));
        }
        VBox weightsTile = Ui.tile("FontWeight THIN .. BLACK, Font.font(family, weight, 14)", weights);

        HBox sizes = new HBox(10);
        sizes.setAlignment(Pos.BASELINE_LEFT);
        for (int size : new int[] { 9, 12, 16, 24, 36 }) {
            Text t = new Text("Ag" + size);
            t.setFont(Font.font("System", size));
            sizes.getChildren().add(t);
        }
        VBox sizesTile = Ui.grow(Ui.tile("System font, 9 .. 36 px", sizes));
        HBox row1 = new HBox(8, weightsTile, sizesTile);

        // Row 2 : posture & decorations, stroke & paint, smoothing, CSS
        Text regular = text("Regular", Font.font("System", FontPosture.REGULAR, 14));
        Text italic = text("Italic", Font.font("System", FontPosture.ITALIC, 14));
        Text boldItalic = text("Bold Italic", Font.font(sans, FontWeight.BOLD, FontPosture.ITALIC, 14));
        Text underline = text("Underline", Font.font("System", 14));
        underline.setUnderline(true);
        Text strike = text("Strikethrough", Font.font("System", 14));
        strike.setStrikethrough(true);
        Text both = text("Both, in red", Font.font(georgia, FontPosture.ITALIC, 14));
        both.setUnderline(true);
        both.setStrikethrough(true);
        both.setFill(Color.web("#c62828"));
        VBox decorTile = Ui.tile("FontPosture, underline, strikethrough",
                new HBox(8, regular, italic, boldItalic), new HBox(8, underline, strike), both);

        Font big = Font.font(sans, FontWeight.BOLD, 30);
        Text stroked = text("Stroke", big);
        stroked.setFill(Color.WHITE);
        stroked.setStroke(Color.web("#1565c0"));
        stroked.setStrokeWidth(1.5);
        Text outline = text("Outline", big);
        outline.setFill(Color.TRANSPARENT);
        outline.setStroke(Color.web("#2e7d32"));
        outline.setStrokeType(StrokeType.OUTSIDE);
        outline.setStrokeWidth(1);
        outline.getStrokeDashArray().addAll(3.0, 2.0);
        Text linear = text("Linear", big);
        linear.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#ff6f00")),
                new Stop(0.5, Color.web("#d81b60")), new Stop(1, Color.web("#5e35b1"))));
        Text radial = text("Radial", big);
        radial.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.6, true, CycleMethod.REFLECT, new Stop(0, Color.web("#fdd835")),
                new Stop(1, Color.web("#00897b"))));
        radial.setStroke(Color.web("#004d40"));
        radial.setStrokeWidth(0.5);
        Text pattern = text("Pattern", big);
        pattern.setFill(new ImagePattern(new javafx.scene.image.Image(Fx.resourceUrl("/showcase/images/texture.png")), 0, 0,
                16, 16, false));
        GridPane paints = new GridPane();
        paints.setHgap(12);
        paints.addRow(0, stroked, outline, pattern);
        paints.addRow(1, linear, radial);
        VBox paintTile = Ui.tile("Stroke (INSIDE/OUTSIDE, dashed), gradient and image pattern fills", paints);

        Text lcd = text("LCD smoothing", Font.font("System", 14));
        lcd.setFontSmoothingType(FontSmoothingType.LCD);
        Text gray = text("GRAY smoothing", Font.font("System", 14));
        gray.setFontSmoothingType(FontSmoothingType.GRAY);
        Text lcdSmall = text("LCD 10px text", Font.font("System", 10));
        lcdSmall.setFontSmoothingType(FontSmoothingType.LCD);
        Text graySmall = text("GRAY 10px text", Font.font("System", 10));
        graySmall.setFontSmoothingType(FontSmoothingType.GRAY);
        VBox smoothingTile = Ui.tile("FontSmoothingType", lcd, gray, lcdSmall, graySmall);

        String cssStyle1 = css1(serif);
        Text css1 = new Text("CSS: -fx-font");
        css1.setStyle(cssStyle1);
        Text css2 = new Text("CSS: " + mono + ", stroke");
        css2.setStyle(css2(mono));
        VBox cssTile = Ui.grow(Ui.tile("Text styled with inline CSS", css1, css2));
        HBox row2 = new HBox(8, decorTile, paintTile, smoothingTile, cssTile);

        // Row 3 : wrapping width and alignment
        HBox alignments = new HBox(10);
        List<String> lineCounts = new ArrayList<>();
        for (TextAlignment alignment : TextAlignment.values()) {
            Text paragraph = new Text(PARAGRAPH + " (" + alignment + ")");
            paragraph.setFont(Font.font("System", 12));
            paragraph.setWrappingWidth(228);
            paragraph.setTextAlignment(alignment);
            Rectangle guide = new Rectangle(228, 1, Color.web("#90caf9"));
            VBox box = new VBox(2, guide, paragraph);
            alignments.getChildren().add(box);
            lineCounts.add(alignment + "=" + paragraph.getLayoutInfo().getTextLineCount());
        }
        checks.add(Check.info("wrapped line count (LayoutInfo)", String.join(", ", lineCounts)));
        VBox alignTile = Ui.tile("wrappingWidth = 228 (blue guide) with TextAlignment LEFT, CENTER, RIGHT, JUSTIFY",
                alignments);

        // Row 4 : line spacing and bounds types
        HBox spacings = new HBox(10);
        for (double spacing : new double[] { 0, 5, 12 }) {
            Text t = new Text("lineSpacing " + (int) spacing + "\nsecond line\nthird line");
            t.setFont(Font.font("System", 12));
            t.setLineSpacing(spacing);
            StackPane box = new StackPane(t);
            box.setAlignment(Pos.TOP_LEFT);
            box.setStyle("-fx-background-color: #f1f8e9;");
            spacings.getChildren().add(box);
        }
        VBox spacingTile = Ui.tile("Text.lineSpacing 0, 5, 12", spacings);

        HBox boundsTypes = new HBox(14);
        List<String> boundsValues = new ArrayList<>();
        for (TextBoundsType type : TextBoundsType.values()) {
            Text t = new Text("Égypte");
            t.setFont(Font.font(serif, 30));
            t.setBoundsType(type);
            t.setTextOrigin(VPos.TOP);
            Rectangle r = new Rectangle();
            r.setFill(Color.web("#ffecb3"));
            r.setStroke(Color.web("#ff8f00"));
            r.setStrokeWidth(0.5);
            r.setX(t.getLayoutBounds().getMinX());
            r.setY(t.getLayoutBounds().getMinY());
            r.setWidth(t.getLayoutBounds().getWidth());
            r.setHeight(t.getLayoutBounds().getHeight());
            Group g = new Group(r, t);
            VBox cell = new VBox(2, Ui.caption(type.name()), g);
            boundsTypes.getChildren().add(cell);
            boundsValues.add(type + " " + Ui.size(t.getLayoutBounds()));
        }
        checks.add(Check.info("layoutBounds by TextBoundsType", String.join(", ", boundsValues)));
        VBox boundsTile = Ui.grow(Ui.tile("TextBoundsType : layout bounds drawn behind the text", boundsTypes));
        HBox row4 = new HBox(8, spacingTile, boundsTile);

        Text metrics = new Text("Hello JavaFX");
        metrics.setFont(Font.font("System", 20));
        checks.add(Check.info("'Hello JavaFX' System 20 bounds / baseline",
                Ui.size(metrics.getLayoutBounds()) + " / " + Ui.num(metrics.getBaselineOffset())));
        checks.add(cssCheck(cssStyle1));

        // Row 5 : label overrun styles
        HBox overruns = new HBox(6);
        List<Label> overrunLabels = new ArrayList<>();
        OverrunStyle[] styles = OverrunStyle.values();
        for (int i = 0; i <= styles.length; i++) {
            Label label = new Label(OVERRUN_TEXT);
            String name;
            if (i < styles.length) {
                label.setTextOverrun(styles[i]);
                name = styles[i].name();
            } else {
                label.setTextOverrun(OverrunStyle.ELLIPSIS);
                label.setEllipsisString(" [...]");
                name = "ellipsisString";
            }
            label.setPrefWidth(114);
            label.setMaxWidth(114);
            label.setStyle("-fx-background-color: #eceff1; -fx-padding: 1 3 1 3;");
            label.setUserData(name);
            overrunLabels.add(label);
            Label caption = Ui.caption(name);
            caption.setMinWidth(Label.USE_PREF_SIZE);
            overruns.getChildren().add(new VBox(1, caption, label));
        }
        VBox overrunTile = Ui.tile("Label.textOverrun (OverrunStyle) with maxWidth 114, and a custom ellipsisString",
                overruns);

        VBox checksHolder = new VBox(Checks.view("Font resolution and text metrics", checks));
        VBox root = new VBox(8, row1, row2, alignTile, row4, overrunTile, checksHolder);
        root.setPrefWidth(1028);
        root.setMaxWidth(1028);
        // the text displayed by the label skins (computed by the overrun logic) is known once laid out
        CompletionStage<?> ready = Fx.pulses(2).thenRun(() -> {
            List<Check> all = new ArrayList<>(checks);
            List<String> shown = overrunLabels.stream()
                    .map(l -> l.getUserData() + "=" + displayed(l))
                    .toList();
            int half = (shown.size() + 1) / 2;
            all.add(Check.info("Label displayed text (1)", String.join(" | ", shown.subList(0, half))));
            all.add(Check.info("Label displayed text (2)", String.join(" | ", shown.subList(half, shown.size()))));
            checksHolder.getChildren().setAll(Checks.view("Font resolution, text metrics and label overrun", all));
        });
        root.getProperties().put(READY, ready);
        return root;
    }

    private static final String READY = "text-basics.ready";

    /** The -fx-font shorthand takes a single family ("Times New Roman" on macOS). */
    private static String css1(String serif) {
        return "-fx-font: italic bold 18px '" + serif + "'; -fx-fill: #6a1b9a; -fx-underline: true;";
    }

    /** "Menlo" on macOS. */
    private static String css2(String mono) {
        return PageFonts.cssFamily(mono) + " -fx-font-size: 15px; -fx-fill: #fff59d; "
                + "-fx-stroke: #e65100; -fx-stroke-width: 0.6;";
    }

    private static Check cssCheck(String style) {
        return Checks.run("inline CSS -fx-font resolved", () -> {
            Text t = new Text("css");
            t.setStyle(style);
            new Scene(new Group(t));
            t.applyCss();
            return t.getFont().getName() + " " + Ui.num(t.getFont().getSize()) + ", fill " + t.getFill() + ", underline "
                    + t.isUnderline();
        });
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return (CompletionStage<?>) content.getProperties().get(READY);
    }

    private static String displayed(Label label) {
        return label.lookupAll(".text").stream()
                .filter(n -> n instanceof Text)
                .map(n -> ((Text) n).getText())
                .collect(Collectors.joining());
    }

    private static Text text(String value, Font font) {
        Text t = new Text(value);
        t.setFont(font);
        return t;
    }
}
