package io.quarkiverse.fx.showcase.pages.platform;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import javax.imageio.ImageIO;

import jakarta.inject.Singleton;

import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.WebPage;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms;
import io.quarkiverse.fx.showcase.pages.web.WebSupport;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * JavaFX features relying on AWT, besides printing and Swing interop : the ImageIO image loader (JavaFX 24 and later
 * decode the formats they do not support themselves, e.g. TIFF, with ImageIO), and WebKit, which beeps through
 * {@code java.awt.Toolkit} (Ctrl+C or cut without a selection) and encodes {@code canvas.toDataURL} with ImageIO.
 * <p>
 * Like the Swing interop and printing pages, this page needs AWT : in a native executable, Quarkus Desktop.
 */
@Singleton
public class PlatformAwtPage implements FeaturePage {

    /**
     * pattern.bmp as an 8 bit gray TIFF. The ImageIO image loader of JavaFX 25.0.4 fails, in the JVM too, on RGB(A)
     * TIFFs ("Unsupported image type: TYPE_CUSTOM"), on 8 bit palettes (ArrayIndexOutOfBoundsException for the indexes
     * above 127) and when the image is requested at another size (ClassCastException of a ToolkitImage).
     */
    private static final String TIFF = "/showcase/images/pattern.tiff";

    /**
     * 8x8 TIFF, 2 bits per pixel palette : red, green, blue and amber quadrants.
     */
    private static final String TIFF_DATA_URI = "data:image/tiff;base64,TU0AKgAAAAgADQEAAAMAAAABAAgAAAEBAAMAAAABAAgAAAE"
            + "CAAMAAAABAAIAAAEDAAMAAAABAAEAAAEGAAMAAAABAAMAAAERAAQAAAABAAAA1AEVAAMAAAABAAEAAAEWAAMAAAABAAgAAAEXAAQAAAA"
            + "BAAAAEAEaAAUAAAABAAAArAEbAAUAAAABAAAAtAEoAAMAAAABAAEAAAFAAAMAAAAMAAAAvAAAAAAAAAAAAAEAAAABAAAAAQAAAAHl5UN"
            + "DHh7//zk5oKCIiMHBNTVHR+XlBwcAVQBVAFUAVar/qv+q/6r/";

    private static final String HTML = """
            <html><body style="margin: 8px; font-family: Arial, sans-serif; font-size: 13px; background: #fafafa;">
            <div id="editor" contenteditable="true" style="display: inline-block; vertical-align: top; width: 190px;
                height: 64px; padding: 6px; border: 1px solid #90a4ae; background: white;">Editable, nothing selected :
                Ctrl+C and cut beep</div>
            <canvas id="canvas" width="160" height="80" style="border: 1px solid #90a4ae;"></canvas>
            <script>
            var g = document.getElementById('canvas').getContext('2d');
            g.fillStyle = '#1e88e5';
            g.fillRect(0, 0, 80, 80);
            g.fillStyle = '#fdd835';
            g.beginPath();
            g.arc(120, 40, 30, 0, 2 * Math.PI);
            g.fill();
            </script>
            </body></html>
            """;

    private static final String READY = PlatformAwtPage.class.getName();

    @Override
    public String id() {
        return "platform-awt";
    }

    @Override
    public String title() {
        return "JavaFX Features Using AWT";
    }

    @Override
    public String category() {
        return Categories.PLATFORM;
    }

    @Override
    public int order() {
        return 45;
    }

    @Override
    public Node build() {
        double half = PlatformUi.HALF_WIDTH;
        List<Check> imageChecks = new ArrayList<>();

        // ImageIO image loader
        Image tiff = load(imageChecks, "pattern.tiff (ImageIO loader)", () -> new Image(Fx.resourceUrl(TIFF)));
        if (tiff != null) {
            imageChecks.add(Checks.expect("pattern.tiff pixels", "256x256, gray levels of ImageIO.read",
                    () -> size(tiff) + ", " + (sameGrayLevels(tiff, ImageIO.read(Fx.resource(TIFF))) ? "gray levels of"
                            : "different from") + " ImageIO.read"));
        }
        Image data = load(imageChecks, "TIFF data: URI (ImageIO loader)", () -> new Image(TIFF_DATA_URI));
        if (data != null) {
            imageChecks.add(Checks.expect("TIFF data: URI quadrants", "8x8: ffe53935 ff43a047 ff1e88e5 ffffc107",
                    () -> size(data) + ": " + quadrants(data)));
        }
        Image background = new Image(Fx.resourceUrl(TIFF), true);

        HBox images = new HBox(12,
                image("pattern.tiff (8 bit gray)", tiff, 128),
                image("data: URI x8", data, 64),
                image("background loading", background, 64));
        images.setAlignment(Pos.TOP_LEFT);

        // WebKit
        List<Check> webChecks = new ArrayList<>();
        WebView webView = new WebView();
        webView.setPrefSize(half - 20, 110);
        webView.setMinSize(half - 20, 110);
        webView.setMaxSize(half - 20, 110);
        webView.setContextMenuEnabled(false);
        webView.setFocusTraversable(false);
        WebEngine engine = webView.getEngine();
        ImageView dataUrlView = new ImageView();
        engine.loadContent(HTML);

        VBox left = new VBox(10,
                PlatformUi.demo("ImageIO image loader : TIFF, a format JavaFX does not decode itself (JavaFX 24+)",
                        images),
                PlatformUi.demo("WebView : copy and cut without a selection beep (java.awt.Toolkit), toDataURL",
                        webView, new HBox(8, PlatformUi.caption(
                                "canvas.toDataURL('image/png'), decoded :"), dataUrlView)));
        PlatformUi.width(left, half);
        VBox checksBox = new VBox(10, PlatformUi.checks("ImageIO image loader", imageChecks, 230, half));
        PlatformUi.width(checksBox, half);
        VBox root = PlatformUi.page(0, new HBox(12, left, checksBox));

        CompletionStage<?> ready = Fx.timeout(Fx.when(background.progressProperty(), progress -> progress.doubleValue()
                >= 1), 20_000, "background image")
                .handle((v, error) -> {
                    imageChecks.add(error != null ? Check.fail("background loading", Checks.describe(error))
                            : background.isError() ? Check.fail("background loading",
                                    Checks.describe(background.getException()))
                                    : Check.pass("background loading", size(background) + ", progress 1.0"));
                    return null;
                })
                .thenCompose(v -> WebSupport.loaded(engine, 20_000))
                .handle((state, error) -> {
                    webChecks.add(Checks.expect("load", "SUCCEEDED", () -> WebSupport.outcome(engine, state, error)));
                    webChecks.addAll(webKitChecks(webView, dataUrlView));
                    checksBox.getChildren().setAll(PlatformUi.checks("ImageIO image loader", imageChecks, 230, half),
                            PlatformUi.checks("WebKit", webChecks, 230, half));
                    return null;
                })
                .thenCompose(v -> WebSupport.stable(webView, 10_000))
                .thenCompose(v -> Fx.pulses(3));
        root.getProperties().put(READY, ready);
        return root;
    }

    /**
     * WebKit features relying on AWT (FX thread, once the page is loaded).
     */
    private static List<Check> webKitChecks(WebView webView, ImageView dataUrlView) {
        WebEngine engine = webView.getEngine();
        List<Check> checks = new ArrayList<>();
        // A key event, as in an HTMLEditor : Ctrl+C (Cmd+C on macOS) without a selection makes WebKit beep with
        // Toolkit.getDefaultToolkit().beep(), called from native code (the process crashes when the JNI lookups fail)
        checks.add(Checks.expect("Ctrl+C without a selection (beep)", "handled, still running", () -> {
            engine.executeScript("document.getElementById('editor').focus()");
            boolean mac = Platforms.isMac();
            webView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.C, false, !mac, false, mac));
            webView.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.C, false, !mac, false, mac));
            engine.executeScript("document.activeElement.blur()");
            return "handled, still running";
        }));
        // An editor command, as the HTMLEditor toolbar runs them : cut without a selection beeps too. Not copy :
        // WebPage.executeCommand("copy") without a selection crashes JavaFX 25.0.4 in the JVM too
        checks.add(Checks.expect("cut without a selection (beep)", "executed, still running", () -> {
            WebPage page = Accessor.getPageFor(engine);
            return (page.executeCommand("cut", null) ? "executed" : "not executed") + ", still running";
        }));
        checks.add(Checks.expect("selection after the commands", "''",
                () -> "'" + engine.executeScript("String(window.getSelection())") + "'"));
        // canvas.toDataURL : WebKit converts the canvas to a BufferedImage and encodes it with an ImageIO writer
        checks.add(Checks.run("canvas.toDataURL('image/png')", () -> {
            String url = (String) engine.executeScript("document.getElementById('canvas').toDataURL('image/png')");
            if (!url.startsWith("data:image/png;base64,")) {
                throw new IllegalStateException("not a PNG data URL: " + url.substring(0, Math.min(40, url.length())));
            }
            byte[] bytes = Base64.getMimeDecoder().decode(url.substring(url.indexOf(',') + 1));
            Image image = new Image(new ByteArrayInputStream(bytes));
            if (image.isError()) {
                throw new IllegalStateException("not decodable", image.getException());
            }
            dataUrlView.setImage(image);
            PixelReader reader = image.getPixelReader();
            return size(image) + ", (40,40) " + hex(reader.getArgb(40, 40)) + ", (120,40) " + hex(reader.getArgb(120,
                    40)) + ", (150,5) " + hex(reader.getArgb(150, 5));
        }));
        checks.add(Checks.run("canvas.toDataURL('image/jpeg')", () -> {
            String url = (String) engine.executeScript("document.getElementById('canvas').toDataURL('image/jpeg')");
            if (!url.startsWith("data:image/jpeg;base64,")) {
                throw new IllegalStateException("not a JPEG data URL: " + url.substring(0, Math.min(40, url.length())));
            }
            Image image = new Image(new ByteArrayInputStream(Base64.getMimeDecoder().decode(url.substring(
                    url.indexOf(',') + 1))));
            if (image.isError()) {
                throw new IllegalStateException("not decodable", image.getException());
            }
            return "JPEG " + size(image);
        }));
        return checks;
    }

    /**
     * Loads an image, recording a failed check when it fails : null then.
     */
    private static Image load(List<Check> checks, String name, Callable<Image> loader) {
        try {
            Image image = loader.call();
            if (image.isError()) {
                checks.add(Check.fail(name, Checks.describe(image.getException())));
                return null;
            }
            checks.add(Check.pass(name, "loaded " + size(image)));
            return image;
        } catch (Throwable t) {
            checks.add(Check.fail(name, Checks.describe(t)));
            return null;
        }
    }

    private static VBox image(String caption, Image image, double width) {
        Node content;
        if (image == null) {
            Label failed = new Label("failed");
            failed.getStyleClass().add("check-fail");
            content = failed;
        } else {
            // the pixels of a small (loaded) image shown as squares
            int factor = image.getWidth() > 0 ? (int) (width / image.getWidth()) : 1;
            ImageView view = new ImageView(factor > 1 ? enlarge(image, factor) : image);
            view.setFitWidth(width);
            view.setPreserveRatio(true);
            content = view;
        }
        VBox box = new VBox(3, content, PlatformUi.caption(caption));
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    /**
     * Whether the pixels of {@code image} have the gray levels of {@code expected} (JavaFX shows the gray levels as
     * they are, {@code BufferedImage.getRGB} would convert them from the linear gray color space).
     */
    private static boolean sameGrayLevels(Image image, BufferedImage expected) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        if (width != expected.getWidth() || height != expected.getHeight()) {
            return false;
        }
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        int[] levels = expected.getRaster().getSamples(0, 0, width, height, 0, (int[]) null);
        return Arrays.equals(pixels, Arrays.stream(levels).map(g -> 0xFF000000 | g << 16 | g << 8 | g).toArray());
    }

    /**
     * Nearest neighbor enlargement.
     */
    private static WritableImage enlarge(Image image, int factor) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage large = new WritableImage(width * factor, height * factor);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = large.getPixelWriter();
        for (int y = 0; y < height * factor; y++) {
            for (int x = 0; x < width * factor; x++) {
                writer.setArgb(x, y, reader.getArgb(x / factor, y / factor));
            }
        }
        return large;
    }

    private static String quadrants(Image image) {
        PixelReader reader = image.getPixelReader();
        return String.join(" ", hex(reader.getArgb(1, 1)), hex(reader.getArgb(6, 1)), hex(reader.getArgb(1, 6)),
                hex(reader.getArgb(6, 6)));
    }

    private static String size(Image image) {
        return (int) image.getWidth() + "x" + (int) image.getHeight();
    }

    private static String hex(int argb) {
        return String.format(Locale.ROOT, "%08x", argb);
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return (CompletionStage<?>) content.getProperties().get(READY);
    }
}
