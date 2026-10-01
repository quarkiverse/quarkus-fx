package io.quarkiverse.fx.showcase.pages.controls;

import java.text.Bidi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.geometry.HPos;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

@Singleton
public class RtlPage implements FeaturePage {

    /**
     * The texts of one copy of the form.
     */
    record Texts(String title, String name, String nameValue, String email, String country, List<String> countries,
            String options, String newsletter, String remember, String plan, List<String> plans, String volume,
            String progress, String items, List<String> itemValues, String quantity, String notes, String notesValue,
            String mixed, String submit, String cancel) {
    }

    private static final Texts ENGLISH = new Texts(
            "Registration form (LEFT_TO_RIGHT, system font)",
            "Name", "Sara",
            "Email",
            "Country", List.of("Saudi Arabia", "Egypt", "Morocco"),
            "Options", "Newsletter", "Remember me",
            "Plan", List.of("Free", "Pro", "Team"),
            "Volume",
            "Progress",
            "Items", List.of("Apples", "Bananas", "Oranges", "Grapes", "Dates", "Figs", "Lemons", "Olives"),
            "Quantity",
            "Notes", "A wrapped TextArea : the lines start on the left side in a left-to-right orientation and on the "
                    + "right side in a right-to-left orientation.",
            "Mixed text: Quarkus FX 3.28 with JavaFX 25",
            "Submit", "Cancel");

    private static final Texts ARABIC = new Texts(
            "نموذج التسجيل (RIGHT_TO_LEFT, Droid Arabic Kufi)",
            "الاسم", "سارة",
            "البريد الإلكتروني",
            "البلد", List.of("السعودية", "مصر", "المغرب"),
            "الخيارات", "النشرة الإخبارية", "تذكرني",
            "الخطة", List.of("مجانية", "احترافية", "فريق"),
            "مستوى الصوت",
            "التقدم",
            "العناصر", List.of("تفاح", "موز", "برتقال", "عنب", "تمر", "تين", "ليمون", "زيتون"),
            "الكمية",
            "ملاحظات", "منطقة نص ملتفة: تبدأ الأسطر من الجهة اليسرى في الاتجاه من اليسار إلى اليمين، ومن الجهة اليمنى "
                    + "في الاتجاه من اليمين إلى اليسار.",
            "نص مختلط بخط كوفي: Quarkus FX 3.28 مع JavaFX 25",
            "إرسال", "إلغاء");

    private static final String ARABIC_WORD = "مرحبا";

    /**
     * Maximum line length (in chars) of the notes when right-to-left text cannot be wrapped, see {@link #rtlWrapError()}.
     */
    private static final int HARD_WRAP_CHARS = 48;

    @Override
    public String id() {
        return "controls-rtl";
    }

    @Override
    public String title() {
        return "Right-to-left Orientation";
    }

    @Override
    public String category() {
        return Categories.CONTROLS;
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public Node build() {
        ControlsUi.ChecksHolder checks = new ControlsUi.ChecksHolder();
        List<Check> early = checks.early;

        Font kufi = ControlsUi.droidKufi();
        early.add(ControlsUi.droidKufiCheck("Font.loadFont(stream) Droid Kufi"));

        Throwable rtlWrapError = rtlWrapError();
        VBox ltr = form(ENGLISH, NodeOrientation.LEFT_TO_RIGHT, null, true);
        VBox rtl = form(ARABIC, NodeOrientation.RIGHT_TO_LEFT, kufi, rtlWrapError == null);
        ltr.setId("form-ltr");
        rtl.setId("form-rtl");
        HBox.setHgrow(ltr, Priority.ALWAYS);
        HBox.setHgrow(rtl, Priority.ALWAYS);
        ltr.setPrefWidth(500);
        rtl.setPrefWidth(500);
        HBox forms = new HBox(12, ltr, rtl);

        early.add(Checks.expect("effective orientation of a nested control (LTR / RTL)",
                "LEFT_TO_RIGHT / RIGHT_TO_LEFT", () -> ltr.lookup("#name-field").getEffectiveNodeOrientation() + " / "
                        + rtl.lookup("#name-field").getEffectiveNodeOrientation()));
        early.add(Checks.run("java.text.Bidi on the mixed Arabic text", () -> {
            Bidi bidi = new Bidi(ARABIC.mixed(), Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            return "mixed=" + bidi.isMixed() + ", runs=" + bidi.getRunCount() + ", baseLevel=" + bidi.getBaseLevel();
        }));
        early.add(Checks.run("Text hitTest on an Arabic word (left edge / right edge)", () -> {
            Text text = new Text(ARABIC_WORD);
            text.setFont(new Font(kufi.getName(), 20));
            double width = text.getLayoutBounds().getWidth();
            double y = text.getLayoutBounds().getMinY() + text.getLayoutBounds().getHeight() / 2;
            return "char " + text.hitTest(new Point2D(1, y)).getCharIndex() + " / char "
                    + text.hitTest(new Point2D(width - 1, y)).getCharIndex() + ", width "
                    + String.format(Locale.ROOT, "%.1f", width);
        }));
        if (rtlWrapError != null) {
            early.add(Check.info("TextArea wrapping of right-to-left text",
                    "disabled, explicit line breaks instead (JavaFX bug: " + Checks.describe(rtlWrapError) + ")"));
        }

        VBox root = ControlsUi.page(10, forms, checks);
        root.setPrefWidth(1028);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Fx.pulses(2).thenRunAsync(() -> {
            ControlsUi.ChecksHolder holder = ControlsUi.find(content, ControlsUi.ChecksHolder.class);
            List<Check> late = new ArrayList<>();
            late.add(Checks.expect("label left of its field (LTR / RTL)", "true / false",
                    () -> labelLeftOfField(content, "#form-ltr") + " / " + labelLeftOfField(content, "#form-rtl")));
            late.add(Checks.expect("submit button left of cancel (LTR / RTL)", "true / false",
                    () -> leftOf(content, "#form-ltr", "#submit", "#cancel") + " / "
                            + leftOf(content, "#form-rtl", "#submit", "#cancel")));
            late.add(Checks.expect("ListView scroll bar on the right (LTR / RTL)", "true / false",
                    () -> scrollBarOnRight(content, "#form-ltr") + " / " + scrollBarOnRight(content, "#form-rtl")));
            holder.show("Checks", late);
        }, Fx.FX_THREAD);
    }

    /**
     * The error thrown when laying out a wrapped right-to-left text, or {@code null} when it can be wrapped.
     * <p>
     * JavaFX 25 bug on Linux ({@code PangoGlyphLayout}) : when {@code PrismTextLayout} wraps a right-to-left run, it
     * splits the run and shapes its first part again, but {@code PangoGlyphLayout} shapes the UTF-8 copy of the text it
     * cached for that {@code TextRun} before the split (the whole run). The char offsets of the glyphs are then shifted
     * by the length of the part moved to the next line, and {@code PrismTextLayout.computeTrailingSpaceWidth} throws an
     * {@code ArrayIndexOutOfBoundsException} (negative index) when the run starts at the beginning of the text, like
     * any wrapped paragraph of a TextArea (other wrapped complex runs do not throw, but draw the whole run on their
     * first line). The exception is thrown during the layout pass, which leaves the page half laid out. macOS (CoreText)
     * and Windows (DirectWrite) are not affected : they do not cache the text.
     */
    static Throwable rtlWrapError() {
        try {
            Text probe = new Text(ARABIC_WORD + " " + ARABIC_WORD);
            // narrower than the text : the right-to-left run is split
            probe.setWrappingWidth(20);
            probe.getLayoutBounds();
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    /**
     * Breaks {@code text} into lines of at most {@code maxChars} chars, at spaces.
     */
    static String hardWrap(String text, int maxChars) {
        StringBuilder lines = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (!line.isEmpty() && line.length() + 1 + word.length() > maxChars) {
                lines.append(line).append('\n');
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        return lines.append(line).toString();
    }

    private static boolean labelLeftOfField(Node content, String form) {
        return leftOf(content, form, "#name-label", "#name-field");
    }

    private static boolean leftOf(Node content, String form, String first, String second) {
        Node root = content.lookup(form);
        return sceneX(root.lookup(first)) < sceneX(root.lookup(second));
    }

    private static boolean scrollBarOnRight(Node content, String form) {
        Node list = content.lookup(form).lookup("#items-list");
        Node bar = list.lookupAll(".scroll-bar").stream()
                .filter(n -> n instanceof javafx.scene.control.ScrollBar sb
                        && sb.getOrientation() == javafx.geometry.Orientation.VERTICAL && sb.isVisible())
                .findFirst().orElseThrow(() -> new IllegalStateException("no visible vertical scroll bar"));
        return sceneX(bar) > sceneX(list) + list.getLayoutBounds().getWidth() / 2;
    }

    private static double sceneX(Node node) {
        return node.localToScene(node.getLayoutBounds()).getMinX();
    }

    /**
     * @param wrapNotes whether the notes TextArea wraps its text, or shows it with explicit line breaks
     */
    private static VBox form(Texts t, NodeOrientation orientation, Font font, boolean wrapNotes) {
        Label title = new Label(t.title());
        title.getStyleClass().add("form-title");

        GridPane grid = new GridPane(10, 8);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(110);
        labels.setHalignment(HPos.LEFT);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, fields);

        TextField name = new TextField(t.nameValue());
        name.setId("name-field");
        Label nameLabel = new Label(t.name());
        nameLabel.setId("name-label");
        nameLabel.setLabelFor(name);
        TextField email = new TextField("user@example.com");
        ComboBox<String> country = new ComboBox<>();
        country.getItems().setAll(t.countries());
        country.getSelectionModel().select(0);
        country.setMaxWidth(Double.MAX_VALUE);

        CheckBox newsletter = new CheckBox(t.newsletter());
        newsletter.setSelected(true);
        CheckBox remember = new CheckBox(t.remember());
        HBox options = new HBox(14, newsletter, remember);

        ToggleGroup plans = new ToggleGroup();
        HBox planBox = new HBox(14);
        for (String plan : t.plans()) {
            RadioButton radio = new RadioButton(plan);
            radio.setToggleGroup(plans);
            planBox.getChildren().add(radio);
        }
        plans.selectToggle(plans.getToggles().get(1));

        Slider volume = ControlsUi.staticTicks(new Slider(0, 100, 30));
        volume.setShowTickMarks(true);
        volume.setShowTickLabels(true);
        volume.setMajorTickUnit(25);

        ProgressBar progress = new ProgressBar(0.6);
        progress.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progress, Priority.ALWAYS);
        HBox progressBox = new HBox(8, progress, new Label("60%"));
        progressBox.setAlignment(Pos.CENTER_LEFT);

        ListView<String> list = new ListView<>();
        list.setId("items-list");
        list.getItems().setAll(t.itemValues());
        list.getSelectionModel().select(2);
        list.setPrefHeight(112);

        javafx.scene.control.Spinner<Integer> quantity = new javafx.scene.control.Spinner<>(1, 99, 12);
        quantity.setPrefWidth(110);
        javafx.scene.control.TextArea notes = new javafx.scene.control.TextArea(
                wrapNotes ? t.notesValue() : hardWrap(t.notesValue(), HARD_WRAP_CHARS));
        notes.setWrapText(wrapNotes);
        notes.setPrefRowCount(3);

        Label mixed = new Label(t.mixed());

        Button submit = new Button(t.submit());
        submit.setId("submit");
        submit.setDefaultButton(true);
        Button cancel = new Button(t.cancel());
        cancel.setId("cancel");
        HBox buttons = new HBox(8, submit, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        grid.addRow(0, nameLabel, name);
        grid.addRow(1, new Label(t.email()), email);
        grid.addRow(2, new Label(t.country()), country);
        grid.addRow(3, new Label(t.options()), options);
        grid.addRow(4, new Label(t.plan()), planBox);
        grid.addRow(5, new Label(t.volume()), volume);
        grid.addRow(6, new Label(t.progress()), progressBox);
        Label itemsLabel = new Label(t.items());
        grid.addRow(7, itemsLabel, list);
        GridPane.setValignment(itemsLabel, javafx.geometry.VPos.TOP);
        grid.addRow(8, new Label(t.quantity()), quantity);
        Label notesLabel = new Label(t.notes());
        grid.addRow(9, notesLabel, notes);
        GridPane.setValignment(notesLabel, javafx.geometry.VPos.TOP);

        VBox form = new VBox(10, title, grid, mixed, buttons);
        form.getStyleClass().add("form-copy");
        form.setNodeOrientation(orientation);
        if (font != null) {
            // the embedded Arabic font for the title and the mixed text, the system font (and its Arabic fallback)
            // for the controls
            String style = "-fx-font-family: '" + font.getFamily() + "'; -fx-font-size: 13px;";
            title.setStyle(style);
            mixed.setStyle(style);
        }
        return form;
    }
}
