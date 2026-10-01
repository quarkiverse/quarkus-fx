package io.quarkiverse.fx.showcase.pages.text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms.Families;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.CaretInfo;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.HitInfo;
import javafx.scene.text.LayoutInfo;
import javafx.scene.text.TabStop;
import javafx.scene.text.TabStopPolicy;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.text.TextLineInfo;

/**
 * TextFlow : rich text with embedded nodes, tabs, alignment, line spacing, and the text geometry APIs (hit test, caret
 * and range shapes, layout info).
 */
@Singleton
public class TextFlowPage implements FeaturePage {

    private static final String READY = "text-flow.ready";
    private static final String SELECTED = "Selection is drawn with rangeShape, glyphs use selectionFill.";

    @Override
    public String id() {
        return "text-flow";
    }

    @Override
    public String title() {
        return "TextFlow & Text Geometry";
    }

    @Override
    public String category() {
        return Categories.TEXT;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        List<Check> checks = new ArrayList<>();
        // "Georgia" and "Menlo" on macOS
        String georgia = PageFonts.georgia();
        String mono = Families.mono();

        // Row 1 : rich text flow | tabs
        TextFlow rich = richFlow(georgia, mono);
        rich.setPrefWidth(560);
        rich.setMaxWidth(560);
        rich.setStyle("-fx-background-color: #fafafa;");
        VBox richTile = Ui.tile("TextFlow : mixed fonts, sizes, colors, decorations and embedded Button, ImageView, "
                + "Hyperlink, shape", rich);

        TextFlow tabs4 = tabFlow(4, mono);
        TextFlow tabs8 = tabFlow(8, mono);
        TextFlow stops = tabFlow(8, mono);
        TabStopPolicy policy = new TabStopPolicy();
        policy.tabStops().addAll(new TabStop(90), new TabStop(170));
        policy.setDefaultInterval(60);
        stops.setTabStopPolicy(policy);
        VBox tabsTile = Ui.grow(Ui.tile("tabSize 4, tabSize 8, TabStopPolicy (stops 90, 170, interval 60)",
                labelled("tabSize 4", tabs4), labelled("tabSize 8", tabs8), labelled("TabStopPolicy", stops)));
        HBox row1 = new HBox(8, richTile, tabsTile);

        // Row 2 : alignment in flows
        HBox alignments = new HBox(10);
        for (TextAlignment alignment : TextAlignment.values()) {
            TextFlow flow = new TextFlow(
                    styled("TextFlow ", Font.font("System", FontWeight.BOLD, 12), Color.web("#1565c0")),
                    styled("aligned " + alignment + ", with enough words to wrap over three lines inside "
                            + "a flow that is 238 pixels wide.", Font.font("System", 12), Color.BLACK));
            flow.setTextAlignment(alignment);
            flow.setPrefWidth(238);
            flow.setMaxWidth(238);
            flow.setStyle("-fx-background-color: #e3f2fd;");
            alignments.getChildren().add(flow);
        }
        VBox alignTile = Ui.tile("TextFlow.textAlignment LEFT, CENTER, RIGHT, JUSTIFY", alignments);

        // Row 3 : line spacing | selection | carets and hit test
        HBox spacings = new HBox(8);
        for (double spacing : new double[] { 0, 8 }) {
            TextFlow flow = new TextFlow(styled("lineSpacing " + (int) spacing + " in a flow that wraps on three lines.",
                    Font.font(georgia, 12), Color.BLACK));
            flow.setLineSpacing(spacing);
            flow.setPrefWidth(118);
            flow.setMaxWidth(118);
            flow.setStyle("-fx-background-color: #f1f8e9;");
            spacings.getChildren().add(flow);
        }
        VBox spacingTile = Ui.tile("TextFlow.lineSpacing 0, 8", spacings);

        Text selectable = new Text(SELECTED);
        selectable.setFont(Font.font("System", 13));
        selectable.setWrappingWidth(250);
        selectable.setTextOrigin(VPos.TOP);
        int selStart = SELECTED.indexOf("drawn");
        int selEnd = SELECTED.indexOf("glyphs") + "glyphs".length();
        selectable.setSelectionStart(selStart);
        selectable.setSelectionEnd(selEnd);
        selectable.setSelectionFill(Color.WHITE);
        Path highlight = new Path(selectable.rangeShape(selStart, selEnd));
        highlight.setFill(Color.web("#1e88e5"));
        highlight.setStroke(null);
        Path underline = new Path(selectable.underlineShape(0, 9));
        underline.setFill(Color.web("#e53935"));
        underline.setStroke(null);
        Group selectionGroup = new Group(highlight, underline, selectable);
        VBox selectionTile = Ui.tile("Text selection : rangeShape highlight, selectionFill, underlineShape (red)",
                selectionGroup);
        checks.add(Checks.run("Text.rangeShape(" + selStart + ", " + selEnd + ")",
                () -> Ui.path(selectable.rangeShape(selStart, selEnd))));
        checks.add(Checks.run("Text.underlineShape(0, 9) bounds",
                () -> Ui.bounds(new Path(selectable.underlineShape(0, 9)).getLayoutBounds())));

        Text caretText = new Text("Carets & hits");
        caretText.setFont(Font.font(Families.serif(), 28));
        caretText.setTextOrigin(VPos.TOP);
        Group caretGroup = new Group(caretText);
        for (int index : new int[] { 0, 3, 6, 9, 13 }) {
            Path caret = new Path(caretText.caretShape(index, true));
            caret.setStroke(Color.web("#d81b60"));
            caret.setStrokeWidth(1);
            caretGroup.getChildren().add(caret);
        }
        List<String> hits = new ArrayList<>();
        for (double x : new double[] { 5, 40, 95, 150 }) {
            Point2D point = new Point2D(x, 16);
            HitInfo hit = caretText.hitTest(point);
            hits.add("x" + (int) x + "=" + hitString(hit));
            Circle dot = new Circle(x, 16, 2.5, Color.web("#43a047"));
            caretGroup.getChildren().add(dot);
        }
        VBox caretTile = Ui.grow(Ui.tile("caretShape at 0,3,6,9,13 (pink), hitTest points (green)", caretGroup));
        HBox row3 = new HBox(8, spacingTile, selectionTile, caretTile);

        checks.add(Checks.run("Text.caretShape(3, leading)", () -> Ui.path(caretText.caretShape(3, true))));
        checks.add(Check.info("Text.hitTest (charIndex/leading/insertion)", String.join(", ", hits)));
        checks.add(Checks.run("Text.getLayoutInfo caretInfoAt(6)", () -> caretInfo(caretText.getLayoutInfo().caretInfoAt(6,
                true))));

        // Row 4 : geometry overlays, drawn once the flows are laid out
        TextFlow rangeFlow = geometryFlow(georgia);
        Group rangeOverlay = new Group();
        Pane rangePane = new Pane(rangeOverlay, rangeFlow);
        VBox rangeTile = Ui.grow(Ui.tile("TextFlow.getRangeShape across children (blue), getUnderlineShape (red), "
                + "getStrikeThroughShape (green)", rangePane));
        TextFlow linesFlow = geometryFlow(georgia);
        linesFlow.setLineSpacing(6);
        Group linesOverlay = new Group();
        Pane linesPane = new Pane(linesOverlay, linesFlow);
        VBox linesTile = Ui.grow(Ui.tile("LayoutInfo.getTextLines (orange) and caretInfoAt 0, 25, 50, 75 (pink), "
                + "lineSpacing 6", linesPane));
        HBox row4 = new HBox(8, rangeTile, linesTile);

        VBox checksHolder = new VBox(Checks.view("Text geometry", checks));
        VBox root = new VBox(8, row1, alignTile, row3, row4, checksHolder);
        root.setPrefWidth(1028);
        root.setMaxWidth(1028);

        // TextFlow geometry needs a laid out flow : computed once the page is rendered
        CompletionStage<?> ready = Fx.pulses(2).thenRun(() -> {
            Path range = new Path(rangeFlow.getRangeShape(8, 58, false));
            range.setFill(Color.web("#bbdefb"));
            range.setStroke(null);
            Path under = new Path(rangeFlow.getUnderlineShape(62, 80));
            under.setFill(Color.web("#e53935"));
            under.setStroke(null);
            Path strike = new Path(rangeFlow.getStrikeThroughShape(84, 100));
            strike.setFill(Color.web("#43a047"));
            strike.setStroke(null);
            rangeOverlay.getChildren().setAll(range, under, strike);

            LayoutInfo info = linesFlow.getLayoutInfo();
            for (TextLineInfo line : info.getTextLines(false)) {
                Rectangle r = new Rectangle(line.bounds().getMinX(), line.bounds().getMinY(), line.bounds().getWidth(),
                        line.bounds().getHeight());
                r.setFill(Color.web("#fff3e0"));
                r.setStroke(Color.web("#fb8c00"));
                r.setStrokeWidth(0.5);
                linesOverlay.getChildren().add(r);
            }
            for (int index : new int[] { 0, 25, 50, 75 }) {
                CaretInfo caret = info.caretInfoAt(index, true);
                for (int i = 0; i < caret.getSegmentCount(); i++) {
                    var segment = caret.getSegmentAt(i);
                    Rectangle r = new Rectangle(segment.getMinX() - 1, segment.getMinY(), 2, segment.getHeight());
                    r.setFill(Color.web("#d81b60"));
                    linesOverlay.getChildren().add(r);
                }
            }

            List<Check> all = new ArrayList<>(checks);
            all.add(Checks.run("TextFlow lines (LayoutInfo)", () -> lines(rich.getLayoutInfo())));
            all.add(Checks.run("TextFlow.hitTest(200, 30)", () -> hitString(rich.hitTest(new Point2D(200, 30)))));
            all.add(Checks.run("TextFlow.getRangeShape(8, 58) bounds", () -> Ui.bounds(range.getLayoutBounds())));
            all.add(Checks.run("TextFlow.caretShape(10) / baseline", () -> Ui.path(rich.caretShape(10, true)) + " / "
                    + Ui.num(rich.getBaselineOffset())));
            all.add(Checks.run("LayoutInfo lines with lineSpacing 6", () -> lines(info)));
            all.add(Checks.run("tab flows width (4 / 8 / stops)",
                    () -> Ui.num(tabs4.getLayoutInfo().getLogicalBounds(false).getWidth()) + " / "
                            + Ui.num(tabs8.getLayoutInfo().getLogicalBounds(false).getWidth()) + " / "
                            + Ui.num(stops.getLayoutInfo().getLogicalBounds(false).getWidth())));
            checksHolder.getChildren().setAll(Checks.view("Text and TextFlow geometry", all));
        });
        root.getProperties().put(READY, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return (CompletionStage<?>) content.getProperties().get(READY);
    }

    private static TextFlow richFlow(String georgia, String mono) {
        Text t1 = styled("TextFlow ", Font.font(Families.sans(), FontWeight.BOLD, 18), Color.web("#0d47a1"));
        Text t2 = styled("mixes ", Font.font("System", FontPosture.ITALIC, 14), Color.web("#37474f"));
        Text t3 = styled("fonts, ", Font.font(georgia, 17), Color.web("#6a1b9a"));
        Text t4 = styled("sizes ", Font.font("System", 24), Color.web("#ef6c00"));
        Text t5 = styled("and colors. ", Font.font(mono, 13), Color.web("#2e7d32"));
        Text t6 = styled("It embeds a ", Font.font("System", 14), Color.BLACK);
        Button button = new Button("Button");
        button.setFocusTraversable(false);
        Text t7 = styled(", an image ", Font.font("System", 14), Color.BLACK);
        ImageView image = new ImageView(new Image(Fx.resourceUrl("/showcase/images/icon.png")));
        image.setFitWidth(22);
        image.setFitHeight(22);
        Text t8 = styled(", a ", Font.font("System", 14), Color.BLACK);
        Hyperlink link = new Hyperlink("Hyperlink");
        link.setFocusTraversable(false);
        Text t9 = styled(" and a shape ", Font.font("System", 14), Color.BLACK);
        Rectangle shape = new Rectangle(28, 12, Color.web("#ffb300"));
        shape.setArcWidth(8);
        shape.setArcHeight(8);
        Text t10 = styled(". Underlined, ", Font.font("System", 14), Color.BLACK);
        t10.setUnderline(true);
        Text t11 = styled("struck out", Font.font("System", 14), Color.web("#c62828"));
        t11.setStrikethrough(true);
        Text t12 = styled(" and gradient text flow together, wrapping inside the 560 px wide flow.",
                Font.font("System", FontWeight.BOLD, 14), Color.BLACK);
        t12.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#00897b")),
                new Stop(1, Color.web("#8e24aa"))));
        TextFlow flow = new TextFlow(t1, t2, t3, t4, t5, t6, button, t7, image, t8, link, t9, shape, t10, t11, t12);
        flow.setLineSpacing(2);
        return flow;
    }

    private static TextFlow geometryFlow(String georgia) {
        TextFlow flow = new TextFlow(
                styled("Geometry ", Font.font("System", FontWeight.BOLD, 13), Color.web("#0d47a1")),
                styled("of a flow made of several Text nodes: the range spans ", Font.font("System", 13), Color.BLACK),
                styled("styled children", Font.font(georgia, FontPosture.ITALIC, 14), Color.web("#6a1b9a")),
                styled(", then an underline and a strike-through computed from the text layout.",
                        Font.font("System", 13), Color.BLACK));
        flow.setPrefWidth(480);
        flow.setMaxWidth(480);
        return flow;
    }

    private static TextFlow tabFlow(int tabSize, String mono) {
        Text text = styled("Item\tQty\tPrice\nApple\t3\t1.20\nWatermelon\t12\t10.50", Font.font(mono, 11),
                Color.BLACK);
        TextFlow flow = new TextFlow(text);
        flow.setTabSize(tabSize);
        flow.setStyle("-fx-background-color: #fff8e1;");
        flow.setPrefWidth(300);
        flow.setMaxWidth(300);
        return flow;
    }

    private static Node labelled(String caption, Node node) {
        javafx.scene.control.Label label = Ui.caption(caption);
        label.setMinWidth(82);
        HBox box = new HBox(6, label, node);
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private static Text styled(String value, Font font, Color color) {
        Text t = new Text(value);
        t.setFont(font);
        t.setFill(color);
        return t;
    }

    private static String hitString(HitInfo hit) {
        return hit.getCharIndex() + "/" + (hit.isLeading() ? "L" : "T") + "/" + hit.getInsertionIndex();
    }

    private static String caretInfo(CaretInfo info) {
        StringBuilder sb = new StringBuilder(info.getSegmentCount() + " segment(s)");
        for (int i = 0; i < info.getSegmentCount(); i++) {
            sb.append(' ').append(Ui.rect(info.getSegmentAt(i)));
        }
        return sb.toString();
    }

    private static String lines(LayoutInfo info) {
        StringBuilder sb = new StringBuilder(info.getTextLineCount() + " lines:");
        for (TextLineInfo line : info.getTextLines(false)) {
            sb.append(' ').append(line.start()).append('-').append(line.end()).append(" h").append(
                    Ui.num(line.bounds().getHeight()));
        }
        return sb.toString();
    }
}
