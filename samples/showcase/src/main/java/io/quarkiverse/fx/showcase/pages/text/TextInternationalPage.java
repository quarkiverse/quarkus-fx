package io.quarkiverse.fx.showcase.pages.text;

import java.text.Bidi;
import java.text.BreakIterator;
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
import io.quarkiverse.fx.showcase.core.Platforms;
import io.quarkiverse.fx.showcase.core.Platforms.Families;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * International text : complex scripts (Arabic, Devanagari, Thai), right to left and bidirectional layout, CJK, color
 * emoji, a font loaded from the application resources and system font fallback.
 */
@Singleton
public class TextInternationalPage implements FeaturePage {

    private static final String READY = "text-international.ready";

    static final String ARABIC = "مرحبا بالعالم";
    static final String ARABIC_2 = "القرآن الكريم ١٢٣";
    static final String HEBREW = "שלום עולם";
    static final String CHINESE = "你好，世界！中文";
    static final String JAPANESE = "こんにちは世界";
    static final String KOREAN = "안녕하세요 세계";
    static final String HINDI = "नमस्ते दुनिया";
    static final String THAI = "สวัสดีชาวโลก";
    static final String EMOJI = "😀🎉🚀❤️👍🌍🍕";
    /** Skin tone modifier, flag and ZWJ family sequences : JavaFX draws their components side by side. */
    static final String EMOJI_SEQUENCES = "👍🏽 🇯🇵 👨‍👩‍👧";
    static final String MIXED = "النص العربي يحتوي على JavaFX 25 و Quarkus مع الرقم 2024.";

    @Override
    public String id() {
        return "text-international";
    }

    @Override
    public String title() {
        return "International Text";
    }

    @Override
    public String category() {
        return Categories.TEXT;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Node build() {
        List<Check> checks = new ArrayList<>();
        Font kufi = LoadedFonts.kufi();
        String kufiFamily = LoadedFonts.family(kufi, "Droid Arabic Kufi");
        checks.add(Check.of("DroidKufi loadFont(InputStream)", kufi != null, LoadedFonts.describeKufi()));

        // system fonts : on macOS Geeza Pro, Arial Hebrew, Hiragino Sans GB, PingFang SC, Hiragino Sans,
        // Apple SD Gothic Neo, Kohinoor Devanagari, Thonburi and Apple Color Emoji, elsewhere installed equivalents
        String arabicFamily = Families.arabic();
        String hebrewFamily = Families.hebrew();
        String chineseFamily = Families.chinese();
        String chinese2Family = PageFonts.chinese2();
        String japaneseFamily = Families.japanese();
        String koreanFamily = Families.korean();
        String hindiFamily = Families.devanagari();
        String thaiFamily = Families.thai();
        String emojiFamily = Families.emoji();

        // Row 1 : Arabic
        HBox arabic = new HBox(8,
                Ui.grow(scriptTile("Arabic, " + kufiFamily + " (application font)", kufiFamily, 24, ARABIC, ARABIC_2)),
                Ui.grow(scriptTile("Arabic, " + arabicFamily + " (system font)", arabicFamily, 24, ARABIC, ARABIC_2)),
                Ui.grow(scriptTile("Arabic, System font (fallback)", "System", 24, ARABIC, ARABIC_2)));

        // Row 2 : Hebrew and CJK
        // PingFang SC is a macOS system font that JavaFX draws with wrong glyphs; elsewhere another Chinese font
        String chinese2Caption = Platforms.isMac() ? "Font.font(\"PingFang SC\"): wrong glyphs"
                : "Chinese, " + chinese2Family;
        HBox cjk = new HBox(8,
                Ui.grow(scriptTile("Hebrew, " + hebrewFamily, hebrewFamily, 20, HEBREW)),
                Ui.grow(scriptTile("Chinese, " + chineseFamily, chineseFamily, 20, CHINESE)),
                Ui.grow(scriptTile(chinese2Caption, chinese2Family, 20, CHINESE)),
                Ui.grow(scriptTile("Japanese, " + japaneseFamily, japaneseFamily, 20, JAPANESE)),
                Ui.grow(scriptTile("Korean, " + koreanFamily, koreanFamily, 20, KOREAN)));

        // Row 3 : Devanagari, Thai, emoji
        HBox complex = new HBox(8,
                Ui.grow(scriptTile("Hindi, " + hindiFamily, hindiFamily, 20, HINDI)),
                Ui.grow(scriptTile("Thai, " + thaiFamily, thaiFamily, 20, THAI)),
                Ui.grow(scriptTile(emojiFamily + "; sequences", emojiFamily, 20, EMOJI, EMOJI_SEQUENCES)),
                Ui.grow(scriptTile("Emoji, System font (fallback)", "System", 20, EMOJI, EMOJI_SEQUENCES)),
                Ui.grow(scriptTile("CJK, System font (fallback)", "System", 20, "中文 日本語 한국어")));

        // Row 4 : bidirectional text flows
        String mono = Families.mono();
        TextFlow rtl = bidiFlow(kufiFamily, arabicFamily, mono);
        rtl.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        TextFlow ltr = bidiFlow(kufiFamily, arabicFamily, mono);
        ltr.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        HBox bidi = new HBox(8,
                Ui.grow(Ui.tile("Mixed bidirectional TextFlow, NodeOrientation.RIGHT_TO_LEFT", rtl)),
                Ui.grow(Ui.tile("The same TextFlow, NodeOrientation.LEFT_TO_RIGHT", ltr)));

        // Row 5 : controls
        Label label = new Label("تسمية باللغة العربية");
        label.setStyle("-fx-font-size: 15px;");
        TextField field = new TextField("حقل نص: مرحبا JavaFX");
        field.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        field.setPrefColumnCount(18);
        field.setFocusTraversable(false);
        TextField kufiField = new TextField("نص بخط الكوفي");
        kufiField.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        kufiField.setFont(Font.font(kufiFamily, 14));
        kufiField.setPrefColumnCount(14);
        kufiField.setFocusTraversable(false);
        Button button = new Button("زر عربي");
        button.setFocusTraversable(false);
        CheckBox hebrewBox = new CheckBox("תיבת סימון");
        hebrewBox.setSelected(true);
        hebrewBox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        hebrewBox.setFocusTraversable(false);
        HBox controls = new HBox(12, label, field, kufiField, button, hebrewBox);
        controls.setAlignment(Pos.CENTER_LEFT);
        VBox controlsTile = Ui.tile("Label, TextField (RIGHT_TO_LEFT, System and Kufi fonts), Button and CheckBox", controls);

        // Checks computed on Text nodes (independent of the layout)
        List<String> families = new ArrayList<>();
        for (String family : List.of(arabicFamily, hebrewFamily, chineseFamily, chinese2Family, japaneseFamily,
                koreanFamily, hindiFamily, thaiFamily, emojiFamily)) {
            String resolved = Font.font(family, 20).getFamily();
            families.add(resolved.equals(family) ? family + " =" : family + "→" + resolved);
        }
        checks.add(Check.info("Font.font(family).getFamily()", String.join(", ", families)));
        checks.add(Checks.run("text widths (ar kufi/ar geeza/he/zh/zh pf/ja/ko)",
                () -> String.join(" / ", width(kufiFamily, ARABIC), width(arabicFamily, ARABIC),
                        width(hebrewFamily, HEBREW), width(chineseFamily, CHINESE), width(chinese2Family, CHINESE),
                        width(japaneseFamily, JAPANESE),
                        width(koreanFamily, KOREAN))));
        checks.add(Checks.run("text widths (hi/th/emoji/fallback/sequences)",
                () -> String.join(" / ", width(hindiFamily, HINDI), width(thaiFamily, THAI),
                        width(emojiFamily, EMOJI), width("System", EMOJI), width("System", EMOJI_SEQUENCES))));
        checks.add(Checks.run("Arabic Text hitTest(5, 10) / caretShape(0)", () -> {
            Text t = new Text(ARABIC);
            t.setFont(Font.font(arabicFamily, 24));
            var hit = t.hitTest(new Point2D(5, 10));
            return hit.getCharIndex() + (hit.isLeading() ? " leading" : " trailing") + " / " + Ui.path(t.caretShape(0,
                    true));
        }));
        checks.add(Checks.run("java.text.Bidi runs of the mixed text", () -> {
            Bidi b = new Bidi(MIXED, Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
            StringBuilder sb = new StringBuilder(b.getRunCount() + " runs, base level " + b.getBaseLevel() + ":");
            for (int i = 0; i < b.getRunCount(); i++) {
                sb.append(' ').append(b.getRunStart(i)).append('-').append(b.getRunLimit(i)).append("@").append(b
                        .getRunLevel(i));
            }
            return sb.toString();
        }));
        checks.add(Check.info("TextField effective orientation", field.getEffectiveNodeOrientation()));
        // JavaFX wraps text with BreakIterator.getLineInstance and cuts WORD_ELLIPSIS at word boundaries : the rule
        // data are JDK resources that a native image must include
        checks.add(Checks.run("BreakIterator word / line boundaries", () -> boundaries(BreakIterator.getWordInstance(
                Locale.ROOT), BREAK_SAMPLE) + " / " + boundaries(BreakIterator.getLineInstance(Locale.ROOT),
                        BREAK_SAMPLE)));

        VBox checksHolder = new VBox(Checks.view("International text", checks));
        VBox root = new VBox(8, arabic, cjk, complex, bidi, controlsTile, checksHolder);
        root.setPrefWidth(1028);
        root.setMaxWidth(1028);

        CompletionStage<?> ready = Fx.pulses(2).thenRun(() -> {
            List<Check> all = new ArrayList<>(checks);
            all.add(Checks.run("RTL flow lines / hitTest(10, 10)", () -> rtl.getLayoutInfo().getTextLineCount() + " lines, "
                    + Ui.num(rtl.getLayoutInfo().getLogicalBounds(false).getWidth()) + " wide / char "
                    + rtl.hitTest(new Point2D(10, 10)).getCharIndex()));
            all.add(Checks.run("LTR flow lines / hitTest(10, 10)", () -> ltr.getLayoutInfo().getTextLineCount() + " lines, "
                    + Ui.num(ltr.getLayoutInfo().getLogicalBounds(false).getWidth()) + " wide / char "
                    + ltr.hitTest(new Point2D(10, 10)).getCharIndex()));
            checksHolder.getChildren().setAll(Checks.view("International text", all));
        });
        root.getProperties().put(READY, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return (CompletionStage<?>) content.getProperties().get(READY);
    }

    private static VBox scriptTile(String caption, String family, double size, String... lines) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            Text t = new Text(lines[i]);
            t.setFont(Font.font(family, i == 0 ? size : size * 0.75));
            nodes.add(t);
        }
        return Ui.tile(caption, nodes.toArray(Node[]::new));
    }

    private static TextFlow bidiFlow(String kufiFamily, String arabicFamily, String mono) {
        Text a = new Text("النص العربي يحتوي على ");
        a.setFont(Font.font(arabicFamily, 16));
        Text b = new Text("JavaFX 25");
        b.setFont(Font.font("System", FontWeight.BOLD, 15));
        b.setFill(Color.web("#1565c0"));
        Text c = new Text(" و ");
        c.setFont(Font.font(arabicFamily, 16));
        Text d = new Text("Quarkus");
        d.setFont(Font.font(mono, 14));
        d.setFill(Color.web("#c62828"));
        Text e = new Text(" مع الرقم 2024، ");
        e.setFont(Font.font(arabicFamily, 16));
        Text f = new Text("وخط الكوفي من الموارد.");
        f.setFont(Font.font(kufiFamily, 15));
        f.setFill(Color.web("#2e7d32"));
        TextFlow flow = new TextFlow(a, b, c, d, e, f);
        flow.setPrefWidth(480);
        flow.setMaxWidth(480);
        flow.setStyle("-fx-background-color: #fff8e1;");
        return flow;
    }

    static final String BREAK_SAMPLE = "JavaFX 25: مرحبا بالعالم, 你好。 Hello-world";

    private static String boundaries(BreakIterator iterator, String text) {
        iterator.setText(text);
        List<String> result = new ArrayList<>();
        for (int b = iterator.first(); b != BreakIterator.DONE; b = iterator.next()) {
            result.add(String.valueOf(b));
        }
        return String.join(",", result);
    }

    private static String width(String family, String text) {
        Text t = new Text(text);
        t.setFont(Font.font(family, 20));
        return Ui.num(t.getLayoutBounds().getWidth());
    }
}
