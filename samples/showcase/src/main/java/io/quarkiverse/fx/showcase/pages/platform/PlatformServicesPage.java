package io.quarkiverse.fx.showcase.pages.platform;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;

/**
 * Platform services : system clipboard round trips, screens, platform preferences, key combinations display texts,
 * fonts and threading predicates.
 */
@Singleton
public class PlatformServicesPage implements FeaturePage {

    private static final Logger LOG = Logger.getLogger(PlatformServicesPage.class);

    private static final String CUSTOM_TEXT = "application/x-quarkus-fx-showcase-text";
    private static final String CUSTOM_BYTES = "application/x-quarkus-fx-showcase-bytes";
    private static final String RESTORE_CLIPBOARD = PlatformServicesPage.class.getName() + ".restoreClipboard";

    /** The families JavaFX always lists, installed fonts or not. */
    private static final Set<String> LOGICAL_FAMILIES = Set.of("System", "Serif", "SansSerif", "Monospaced");

    @Override
    public String id() {
        return "platform-services";
    }

    @Override
    public String title() {
        return "Clipboard, Screen, Preferences & Keys";
    }

    @Override
    public String category() {
        return Categories.PLATFORM;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        double half = PlatformUi.HALF_WIDTH;
        VBox clipboard = clipboard(half);
        VBox left = new VBox(10, clipboard, keys(half));
        VBox right = new VBox(10, screenAndPreferences(half), fontsAndThreads(half));
        Node page = PlatformUi.page(10, new HBox(12, left, right), preferenceColors());
        page.getProperties().put(RESTORE_CLIPBOARD, clipboard.getProperties().remove(RESTORE_CLIPBOARD));
        return page;
    }

    @Override
    public void dispose(Node content) {
        if (content.getProperties().remove(RESTORE_CLIPBOARD) instanceof Runnable restore) {
            restore.run();
        }
    }

    /**
     * Every Color valued entry of the platform preferences map (macOS.NSColor.*, Windows.SysColor.* / Windows.UIColor.*
     * or GTK.* colors, depending on the operating system).
     */
    private VBox preferenceColors() {
        Platform.Preferences preferences = Platform.getPreferences();
        List<String> keys = new TreeSet<>(preferences.keySet()).stream()
                .filter(key -> preferences.get(key) instanceof Color).toList();
        GridPane grid = new GridPane(10, 3);
        int columns = 5;
        int rows = (keys.size() + columns - 1) / columns;
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Rectangle chip = new Rectangle(12, 12, (Color) preferences.get(key));
            chip.setStroke(Color.web("#90a4ae"));
            Label name = new Label(key.substring(key.lastIndexOf('.') + 1));
            name.getStyleClass().add("swatch-label");
            name.setMaxWidth(178);
            HBox cell = new HBox(4, chip, name);
            cell.setAlignment(Pos.CENTER_LEFT);
            grid.add(cell, i / rows, i % rows);
        }
        List<Check> checks = List.of(Check.info("Color valued preferences", keys.size() + " of " + preferences.size()),
                Check.info("Other preferences", new TreeSet<>(preferences.keySet()).stream()
                        // the network state (reduced data) is already shown, and may change between runs
                        .filter(key -> !(preferences.get(key) instanceof Color) && !isNetworkState(key))
                        .map(key -> key + "=" + preferenceValue(preferences.get(key))).collect(Collectors.joining(", "))));
        VBox box = PlatformUi.demo("Platform.getPreferences() : " + keys.size() + " Color entries of " + preferences.size()
                + " (sorted keys)", grid);
        Checks.attach(box, checks);
        return PlatformUi.width(box, PlatformUi.CONTENT_WIDTH);
    }

    /**
     * The preferences behind {@code reducedData} : macOS NWPathMonitor, Windows NetworkInformation, GTK network_metered.
     */
    private static boolean isNetworkState(String key) {
        return key.contains("NWPathMonitor") || key.contains("NetworkInformation") || key.contains("network_metered");
    }

    // ------------------------------------------------------------------ clipboard

    private static DataFormat format(String mimeType) {
        DataFormat format = DataFormat.lookupMimeType(mimeType);
        return format != null ? format : new DataFormat(mimeType);
    }

    private VBox clipboard(double width) {
        List<Check> checks = new ArrayList<>();
        Image original = new Image(Fx.resourceUrl("/showcase/images/icon.png"));
        ImageView readBack = new ImageView();
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String saved = savedText(clipboard, checks);
        DataFormat customText = format(CUSTOM_TEXT);
        DataFormat customBytes = format(CUSTOM_BYTES);
        ClipboardContent content = new ClipboardContent();
        content.putString("Quarkus FX clipboard");
        content.putHtml("<b>bold</b> and <i>italic</i>");
        content.putUrl("https://quarkus.io/");
        content.putImage(original);
        content.put(customText, "custom payload ✓");
        content.put(customBytes, ByteBuffer.wrap("raw bytes".getBytes(StandardCharsets.UTF_8)));
        // the system clipboard is native (and shared with other processes) : the round trips are only failures on
        // macOS, where they were verified (see PlatformUi.expectOnMac)
        checks.add(PlatformUi.expectOnMac("setContent (6 formats)", true, () -> clipboard.setContent(content)));
        checks.add(PlatformUi.expectOnMac("getString()", "Quarkus FX clipboard", clipboard::getString));
        checks.add(PlatformUi.expectOnMac("getHtml()", "<b>bold</b> and <i>italic</i>", clipboard::getHtml));
        checks.add(PlatformUi.expectOnMac("getUrl()", "https://quarkus.io/", clipboard::getUrl));
        checks.add(PlatformUi.runOnMac("getImage() round trip", () -> {
            Image image = clipboard.getImage();
            if (image == null) {
                throw new IllegalStateException("no image read back");
            }
            readBack.setImage(image);
            return (int) image.getWidth() + "x" + (int) image.getHeight() + ", identical pixels "
                    + identicalPixels(original, image) + "/" + (int) (original.getWidth() * original.getHeight());
        }));
        checks.add(PlatformUi.expectOnMac("custom DataFormat (serialized String)", "custom payload ✓",
                () -> clipboard.getContent(customText)));
        checks.add(PlatformUi.expectOnMac("custom DataFormat (ByteBuffer)", "raw bytes", () -> {
            Object value = clipboard.getContent(customBytes);
            if (value instanceof ByteBuffer buffer) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return String.valueOf(value);
        }));
        checks.add(PlatformUi.expectOnMac("hasContent / content types", "string html url image custom-text custom-bytes", () -> {
            Set<DataFormat> types = clipboard.getContentTypes();
            List<String> present = new ArrayList<>();
            Map<String, DataFormat> expected = new java.util.LinkedHashMap<>();
            expected.put("string", DataFormat.PLAIN_TEXT);
            expected.put("html", DataFormat.HTML);
            expected.put("url", DataFormat.URL);
            expected.put("image", DataFormat.IMAGE);
            expected.put("custom-text", customText);
            expected.put("custom-bytes", customBytes);
            expected.forEach((name, format) -> {
                if (clipboard.hasContent(format) && types.contains(format)) {
                    present.add(name);
                }
            });
            return String.join(" ", present);
        }));

        ImageView originalView = new ImageView(original);
        HBox images = new HBox(8, labelled("put", originalView), new Label("→"), labelled("read back", readBack));
        images.setAlignment(Pos.CENTER_LEFT);
        VBox box = PlatformUi.demo("Clipboard.getSystemClipboard() : put 6 formats, read them back (text restored)",
                images, PlatformUi.checks(null, checks, 190, width - 18));
        box.getProperties().put(RESTORE_CLIPBOARD, (Runnable) () -> restoreClipboard(clipboard, saved));
        return PlatformUi.width(box, width);
    }

    /**
     * Gives the user back the text that was on the clipboard, once the page is left ({@link #dispose(Node)}) rather
     * than right after the 6 formats were read back. Other processes read new clipboard content as soon as it is
     * published (e.g. the Windows clipboard history, which renders every format) : on Windows, when such a request
     * reaches the content after it was replaced, JavaFX gets no data for the format and crashes the process in
     * GlassClipboard.cpp (GetArrayLength of a null array : OLE_CHECK_NOTNULL does not stop in release builds), in JVM
     * and native mode alike. Keeping the content for the lifetime of the page lets those readers complete first.
     */
    private static void restoreClipboard(Clipboard clipboard, String saved) {
        try {
            if (saved != null) {
                ClipboardContent restore = new ClipboardContent();
                restore.putString(saved);
                clipboard.setContent(restore);
            } else {
                clipboard.clear();
            }
        } catch (Throwable t) {
            LOG.warnf(t, "Unable to restore the clipboard text");
        }
    }

    /**
     * The text on the clipboard before this page uses it (null if none, or if it cannot be read).
     */
    private static String savedText(Clipboard clipboard, List<Check> checks) {
        try {
            return clipboard.hasString() ? clipboard.getString() : null;
        } catch (Throwable t) {
            checks.add(Check.info("read the clipboard text", Checks.describe(t)));
            return null;
        }
    }

    private static VBox labelled(String caption, Node node) {
        Label label = new Label(caption);
        label.getStyleClass().add("swatch-label");
        VBox box = new VBox(2, node, label);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static int identicalPixels(Image a, Image b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return 0;
        }
        PixelReader ra = a.getPixelReader();
        PixelReader rb = b.getPixelReader();
        int same = 0;
        for (int y = 0; y < (int) a.getHeight(); y++) {
            for (int x = 0; x < (int) a.getWidth(); x++) {
                if (ra.getArgb(x, y) == rb.getArgb(x, y)) {
                    same++;
                }
            }
        }
        return same;
    }

    // ------------------------------------------------------------------ key combinations

    private VBox keys(double width) {
        List<Check> checks = new ArrayList<>();
        Map<String, KeyCombination> combinations = new java.util.LinkedHashMap<>();
        combinations.put("Shortcut+S", KeyCombination.keyCombination("Shortcut+S"));
        combinations.put("Shortcut+Shift+Z", KeyCombination.keyCombination("Shortcut+Shift+Z"));
        combinations.put("Ctrl+Alt+Delete", KeyCombination.keyCombination("Ctrl+Alt+Delete"));
        combinations.put("Alt+F4", KeyCombination.keyCombination("Alt+F4"));
        combinations.put("Meta+Space", KeyCombination.keyCombination("Meta+Space"));
        combinations.put("Shift+F10", KeyCombination.keyCombination("Shift+F10"));
        combinations.put("Shortcut+'+' (char)", new KeyCharacterCombination("+", KeyCombination.SHORTCUT_DOWN));
        combinations.put("Shortcut+DIGIT1 (code)", new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN));
        combinations.put("Shift+Alt+Left", KeyCombination.keyCombination("Shift+Alt+Left"));
        combinations.put("Shortcut+Shift+Enter", KeyCombination.keyCombination("Shortcut+Shift+Enter"));

        GridPane grid = new GridPane(10, 4);
        int index = 0;
        for (var entry : combinations.entrySet()) {
            Label name = new Label(entry.getKey());
            name.getStyleClass().add("kv-key");
            Label display = new Label(entry.getValue().getDisplayText());
            display.getStyleClass().add("key-cap");
            grid.add(name, (index % 2) * 2, index / 2);
            grid.add(display, (index % 2) * 2 + 1, index / 2);
            index++;
        }
        checks.add(Checks.run("getDisplayText()", () -> combinations.values().stream()
                .map(KeyCombination::getDisplayText).collect(Collectors.joining("  "))));
        checks.add(Checks.expect("KeyCombination.valueOf(...).getName()", "Shift+Shortcut+A",
                () -> KeyCombination.valueOf("shortcut+shift+a").getName()));
        // the Shortcut modifier is Meta (command) on macOS, Ctrl on Windows and Linux
        checks.add(Checks.expect("Shortcut+C matches Meta+C / Ctrl+C", Platforms.pick("true / false", "false / true",
                "false / true"), () -> {
            KeyCombination copy = KeyCombination.keyCombination("Shortcut+C");
            KeyEvent meta = new KeyEvent(KeyEvent.KEY_PRESSED, "c", "c", KeyCode.C, false, false, false, true);
            KeyEvent ctrl = new KeyEvent(KeyEvent.KEY_PRESSED, "c", "c", KeyCode.C, false, true, false, false);
            return copy.match(meta) + " / " + copy.match(ctrl);
        }));
        checks.add(Checks.expect("KeyCode lookups", "F5 / Enter / A / 10",
                () -> KeyCode.getKeyCode("F5") + " / " + KeyCode.ENTER.getName() + " / " + KeyCode.A.getChar()
                        + " / " + KeyCode.ENTER.getCode()));
        checks.add(Checks.run("Platform.isKeyLocked(CAPS) supported",
                () -> Platform.isKeyLocked(KeyCode.CAPS).isPresent()));

        VBox box = PlatformUi.demo("KeyCombination display texts (platform specific)", grid,
                PlatformUi.checks(null, checks, 190, width - 18));
        return PlatformUi.width(box, width);
    }

    // ------------------------------------------------------------------ screen and preferences

    private VBox screenAndPreferences(double width) {
        List<Check> checks = new ArrayList<>();
        Screen primary = Screen.getPrimary();
        Rectangle2D bounds = primary.getBounds();
        Rectangle2D visual = primary.getVisualBounds();
        // the visual bounds follow the Dock and the menu bar (the taskbar, the desktop panels) : only their consistency is
        // checked. With a fractional scale (Windows, Linux) the logical bounds may be rounded differently : 1 px margin
        checks.add(Check.info("Screen bounds", rect(bounds)));
        Rectangle2D container = Platforms.isMac() ? bounds
                : new Rectangle2D(bounds.getMinX() - 1, bounds.getMinY() - 1, bounds.getWidth() + 2, bounds.getHeight() + 2);
        checks.add(Checks.expect("visual bounds inside bounds, not empty", true,
                () -> container.contains(visual) && visual.getWidth() > 0 && visual.getHeight() > 0));
        checks.add(Check.info("dpi, output scale, screens", primary.getDpi() + " dpi, " + primary.getOutputScaleX()
                + "x" + primary.getOutputScaleY() + ", " + Screen.getScreens().size() + " screen(s)"));
        if (Platforms.isMac()) {
            checks.add(Checks.expect("getScreensForRectangle(visual bounds)", 1,
                    () -> Screen.getScreensForRectangle(visual).size()));
        } else {
            // the logical layout of screens with different scales is platform specific : at least the primary screen
            checks.add(Checks.run("getScreensForRectangle(visual bounds)", () -> {
                int count = Screen.getScreensForRectangle(visual).size();
                if (count < 1) {
                    throw new IllegalStateException("no screen for the visual bounds of the primary screen");
                }
                return count;
            }));
        }

        // a scaled drawing of the primary screen with its size
        double scale = 150 / bounds.getWidth();
        Rectangle whole = new Rectangle(Math.round(bounds.getWidth() * scale), Math.round(bounds.getHeight() * scale),
                Color.web("#37474f"));
        Label size = new Label((int) bounds.getWidth() + " x " + (int) bounds.getHeight() + "\n@" + primary.getOutputScaleX()
                + "x");
        size.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        StackPane screen = new StackPane(whole, size);

        Platform.Preferences preferences = Platform.getPreferences();
        checks.add(Check.info("color scheme / accent", preferences.getColorScheme() + " / " + preferences.getAccentColor()));
        checks.add(Check.info("background / foreground", preferences.getBackgroundColor() + " / "
                + preferences.getForegroundColor()));
        checks.add(Check.info("reduced motion / transparency / data", preferences.isReducedMotion() + " / "
                + preferences.isReducedTransparency() + " / " + preferences.isReducedData()));
        checks.add(Check.info("persistent scroll bars", preferences.isPersistentScrollBars()));
        checks.add(Check.info("preference keys", preferences.size() + ": " + new TreeSet<>(preferences.keySet()).stream()
                .map(key -> key.substring(key.lastIndexOf('.') + 1)).limit(6).collect(Collectors.joining(", "))
                + (preferences.size() > 6 ? ", ..." : "")));
        checks.add(Check.info("Platform.isAccessibilityActive()", Platform.isAccessibilityActive()));

        HBox swatches = new HBox(6,
                swatch("accent", preferences.getAccentColor()),
                swatch("background", preferences.getBackgroundColor()),
                swatch("foreground", preferences.getForegroundColor()));
        Label scheme = new Label(String.valueOf(preferences.getColorScheme()));
        scheme.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        VBox prefs = new VBox(4, scheme, swatches);
        HBox visuals = new HBox(14, labelled("Screen.getPrimary()", screen), prefs);
        visuals.setAlignment(Pos.CENTER_LEFT);
        VBox box = PlatformUi.demo("Screen.getPrimary() and Platform.getPreferences()", visuals,
                PlatformUi.checks(null, checks, 190, width - 18));
        return PlatformUi.width(box, width);
    }

    private static VBox swatch(String name, Color color) {
        Rectangle rectangle = new Rectangle(46, 26, color == null ? Color.TRANSPARENT : color);
        rectangle.setStroke(Color.web("#90a4ae"));
        rectangle.setArcWidth(6);
        rectangle.setArcHeight(6);
        return labelled(name, rectangle);
    }

    private static String rect(Rectangle2D r) {
        return (int) r.getMinX() + "," + (int) r.getMinY() + " " + (int) r.getWidth() + "x" + (int) r.getHeight();
    }

    // ------------------------------------------------------------------ fonts and threads

    private VBox fontsAndThreads(double width) {
        List<Check> checks = new ArrayList<>();
        Font font = Font.getDefault();
        checks.add(Check.info("Font.getDefault()", font.getName() + " / " + font.getFamily() + " / " + font.getSize()));
        checks.add(Checks.expect("Font.font(System, BOLD, ITALIC, 14)", "System Bold Italic 14.0",
                () -> {
                    Font bold = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 14);
                    return bold.getName() + " " + bold.getSize();
                }));
        // Font.loadFont / @font-face fonts of other pages join these lists (depending on when they are first listed) :
        // only the installed fonts are counted, so that this page does not depend on the pages shown before it
        checks.add(Checks.run("Font.getFamilies() / getFontNames()", () -> {
            List<String> families = Font.getFamilies();
            // families installed with every macOS / Windows ; no family is common to all Linux distributions
            List<String> required = Platforms.pick(List.of("Helvetica", "Menlo", "Times New Roman"),
                    List.of("Arial", "Consolas", "Times New Roman"), List.of());
            if (!families.containsAll(required)) {
                throw new IllegalStateException(String.join(", ", required.subList(0, required.size() - 1)) + " or "
                        + required.getLast() + " missing in " + families.size() + " families");
            }
            if (families.stream().filter(PlatformServicesPage::isSystemFont).allMatch(LOGICAL_FAMILIES::contains)) {
                throw new IllegalStateException("no installed font family, only the logical ones");
            }
            return families.stream().filter(PlatformServicesPage::isSystemFont).count() + " system families, "
                    + Font.getFontNames().stream().filter(PlatformServicesPage::isSystemFont).count()
                    + " fonts";
        }));
        checks.add(Checks.run("Text layout width (default font)",
                () -> PlatformUi.round(new Text("Quarkus FX native").getLayoutBounds().getWidth())));

        checks.add(Checks.expect("isFxApplicationThread (page / worker)", "true / false", () -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                boolean worker = executor.submit(Platform::isFxApplicationThread).get(5, TimeUnit.SECONDS);
                return Platform.isFxApplicationThread() + " / " + worker;
            } finally {
                executor.shutdownNow();
            }
        }));
        checks.add(Checks.expect("implicitExit / nested loop running / can start", "true / false / true",
                () -> Platform.isImplicitExit() + " / " + Platform.isNestedLoopRunning() + " / "
                        + Platform.canStartNestedEventLoop()));
        checks.add(Checks.expect("Application.getUserAgentStylesheet()", "null (Modena)",
                () -> Application.getUserAgentStylesheet() == null ? "null (Modena)" : "custom"));

        Label sample = new Label("Default font: " + font.getName() + " " + PlatformUi.round(font.getSize()));
        Label bold = new Label("System Bold Italic 14");
        bold.setFont(Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 14));
        Label light = new Label("System Light 16");
        light.setFont(Font.font("System", FontWeight.LIGHT, 16));
        HBox samples = new HBox(14, sample, bold, light);
        samples.setAlignment(Pos.BASELINE_LEFT);
        StackPane holder = new StackPane(samples);
        holder.setAlignment(Pos.CENTER_LEFT);
        VBox box = PlatformUi.demo("Fonts and threading predicates", holder,
                PlatformUi.checks(null, checks, 190, width - 18));
        return PlatformUi.width(box, width);
    }

    /**
     * A preference value without identity hash codes (some values are arrays).
     */
    private static String preferenceValue(Object value) {
        return value instanceof Object[] array ? java.util.Arrays.toString(array) : String.valueOf(value);
    }

    /**
     * Not one of the fonts bundled with the showcase (/showcase/fonts), which other pages may have loaded.
     */
    private static boolean isSystemFont(String name) {
        return !(name.startsWith("Roboto") || name.startsWith("Droid") || name.startsWith("Font Awesome"));
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return PlatformUi.ready(content);
    }
}
