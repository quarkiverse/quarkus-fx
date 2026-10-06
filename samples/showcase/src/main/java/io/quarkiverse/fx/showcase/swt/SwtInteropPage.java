package io.quarkiverse.fx.showcase.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.MainView;
import io.quarkiverse.fx.showcase.pages.fxml.FxThreadProbe;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.embed.swt.FXCanvas;
import javafx.embed.swt.SWTFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

/**
 * JavaFX embedded in SWT (the SWT variant, -Dswt) : FXCanvas started JavaFX on the SWT user interface thread, Quarkus FX
 * keeps JavaFX once an FXCanvas is disposed and runs the {@code @RunOnFxThread} methods there, SWTFXUtils converts the
 * images, and a second FXCanvas, in its own shell, is painted by SWT (Control.print). The checks are the same in JVM mode
 * and in a native executable, where FXCanvas reads SWT internals by reflection (the scale of the display, the GDK event
 * handler of SWT on Linux) : with the values of the platform, never an address.
 */
@Singleton
public class SwtInteropPage implements FeaturePage {

    private static final String READY = "swt.ready";
    private static final String SHELL = "swt.shell";

    // The colors of the scene of the second FXCanvas, its left and right halves
    private static final Color LEFT = Color.rgb(0x46, 0x95, 0xeb);
    private static final Color RIGHT = Color.rgb(0xff, 0x00, 0x4a);
    private static final int CANVAS_WIDTH = 240;
    private static final int CANVAS_HEIGHT = 120;
    // Per channel : the color conversions of the platforms when SWT paints
    private static final int TOLERANCE = 8;

    @Inject
    FxThreadProbe probe;

    @Override
    public String id() {
        return "swt-fxcanvas";
    }

    @Override
    public String title() {
        return "FXCanvas : JavaFX in SWT";
    }

    @Override
    public String category() {
        return Categories.SWT;
    }

    @Override
    public Node build() throws Exception {
        List<Check> checks = new ArrayList<>();
        checks.add(Check.info("SWT", SWT.getPlatform() + " " + SWT.getVersion()));
        // FXCanvas looked up by name (PlatformImpl.checkForClass)
        checks.add(Checks.expect("Platform.isSupported(SWT)", true, () -> Platform.isSupported(ConditionalFeature.SWT)));
        checks.add(Checks.expect("JavaFX Application Thread = SWT user interface thread", true,
                () -> Platform.isFxApplicationThread() && Display.getCurrent() != null
                        && Display.getCurrent().getThread() == Thread.currentThread()));
        // Quarkus FX turns it off : closing the last FXCanvas would exit JavaFX for good
        checks.add(Checks.expect("Platform.isImplicitExit()", false, Platform::isImplicitExit));
        checks.add(platformProperties());

        // SWTFXUtils.fromFXImage : a JavaFX image converted to SWT image data, and back (pixel by pixel)
        Image source = sample().snapshot(new SnapshotParameters(), null);
        ImageData converted = SWTFXUtils.fromFXImage(source, null);
        checks.add(Checks.expect("SWTFXUtils.fromFXImage (pixels that differ)", 0, () -> differences(source, converted)));
        // Looks up SWT members by name that SWT 3.121 and later no longer has (JavaFX, also in JVM mode)
        checks.add(Check.info("SWTFXUtils.toFXImage", String.valueOf(SWTFXUtils.toFXImage(converted, null))));

        VBox page = new VBox(12);
        page.setPadding(new Insets(16));
        page.setMaxWidth(MainView.PAGE_WIDTH - 32);
        VBox images = new VBox(8,
                caption("A JavaFX node"), new ImageView(source),
                caption("SWTFXUtils.fromFXImage, back to JavaFX"), new ImageView(toFxImage(converted)));
        HBox columns = new HBox(24, Checks.view("Checks", checks), images);
        page.getChildren().add(columns);

        CompletableFuture<Void> ready = Fx.pulses(2)
                .thenCompose(v -> secondCanvas(page))
                .handleAsync((v, error) -> {
                    if (error != null) {
                        page.getChildren().add(Checks.view("Later checks",
                                List.of(Check.fail("second FXCanvas", Checks.describe(error)))));
                    }
                    return (Void) null;
                }, Fx.FX_THREAD).toCompletableFuture();
        page.getProperties().put(READY, ready);
        return page;
    }

    /**
     * The checks once the page is shown : its window, the scale of the display, a second FXCanvas painted by SWT then
     * disposed while JavaFX runs on, a {@code @RunOnFxThread} method called from another thread.
     */
    private CompletionStage<Void> secondCanvas(VBox page) {
        List<Check> checks = new ArrayList<>();
        Window window = page.getScene().getWindow();
        checks.add(Checks.expect("window of the scene", "EmbeddedWindow", () -> window.getClass().getSimpleName()));

        Display display = Display.getCurrent();
        Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
        page.getProperties().put(SHELL, shell);
        shell.setLayout(new FillLayout());
        FXCanvas canvas = new FXCanvas(shell, SWT.NONE);
        Rectangle left = new Rectangle(CANVAS_WIDTH / 2.0, CANVAS_HEIGHT, LEFT);
        Rectangle right = new Rectangle(CANVAS_WIDTH / 2.0, CANVAS_HEIGHT, RIGHT);
        right.setX(CANVAS_WIDTH / 2.0);
        canvas.setScene(new Scene(new Group(left, right), CANVAS_WIDTH, CANVAS_HEIGHT));
        shell.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        shell.setLocation(80, 80);
        shell.open();

        CompletableFuture<String> probed = new CompletableFuture<>();
        return Fx.pulses(5)
                // FXCanvas paints the last frame of its scene : a few pulses, then the paint of SWT
                .thenCompose(v -> Fx.delay(200))
                .thenApply(v -> {
                    // The FXCanvas scale : the backing scale factor of the screen (macOS) or the zoom of SWT (Windows),
                    // read by reflection, 1 on Linux
                    double scale = canvas.getScene().getWindow().getRenderScaleX();
                    int zoom = shell.getMonitor().getZoom();
                    double expected = SWT.getPlatform().equals("gtk") ? 1 : zoom / 100.0;
                    checks.add(Check.of("FXCanvas render scale", scale == expected,
                            "render scale " + scale + ", monitor zoom " + zoom + " %"));

                    ImageData printed = print(canvas);
                    checks.add(color("second FXCanvas painted by SWT (left)", printed, CANVAS_WIDTH / 4, LEFT));
                    checks.add(color("second FXCanvas painted by SWT (right)", printed, CANVAS_WIDTH * 3 / 4, RIGHT));
                    page.getChildren().add(new VBox(8, caption("The second FXCanvas, painted by SWT (Control.print)"),
                            new ImageView(toFxImage(printed))));

                    // JavaFX runs on once the FXCanvas is disposed
                    shell.dispose();
                    page.getProperties().remove(SHELL);
                    Thread caller = new Thread(() -> probe.record("worker", probed), "swt-showcase-worker");
                    caller.setDaemon(true);
                    caller.start();
                    return null;
                })
                .thenCompose(v -> Fx.timeout(probed, 10_000, "@RunOnFxThread"))
                .thenAcceptAsync(result -> {
                    checks.add(Checks.expect("@RunOnFxThread from another thread, after a disposed FXCanvas",
                            "worker -> ran on FX thread: true", () -> result));
                    page.getChildren().add(Checks.view("Once shown", checks));
                }, Fx.FX_THREAD);
    }

    /**
     * The system properties FXCanvas sets for JavaFX : on Linux, the GDK event handler of SWT (Display.eventProc, read by
     * reflection), which Glass calls for the events of the other windows ; on Windows, the zoom of SWT.
     */
    private static Check platformProperties() {
        return switch (SWT.getPlatform()) {
            case "gtk" -> Checks.expect("GDK event handler of SWT (javafx.embed.eventProc)", "set",
                    () -> Long.getLong("javafx.embed.eventProc", 0) != 0 ? "set" : "missing");
            case "win32" -> Check.info("glass.win.uiScale", System.getProperty("glass.win.uiScale"));
            default -> Check.info("javafx.embed.eventProc", System.getProperty("javafx.embed.eventProc")
                    + " (no event handler of SWT to call on " + SWT.getPlatform() + ")");
        };
    }

    private static Node sample() {
        Rectangle background = new Rectangle(160, 80, Color.web("#f0f4ff"));
        Rectangle bar = new Rectangle(20, 20, 120, 16);
        bar.setFill(Color.web("#4695eb"));
        Circle dot = new Circle(130, 56, 14, Color.web("#ff004a"));
        return new Group(background, bar, dot);
    }

    private static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    /**
     * What the control paints, captured by SWT, at the size of the control.
     */
    private static ImageData print(FXCanvas canvas) {
        Point size = canvas.getSize();
        org.eclipse.swt.graphics.Image image = new org.eclipse.swt.graphics.Image(canvas.getDisplay(), size.x, size.y);
        try {
            GC gc = new GC(image);
            try {
                if (!canvas.print(gc)) {
                    throw new IllegalStateException("Control.print is not supported");
                }
            } finally {
                gc.dispose();
            }
            return image.getImageData();
        } finally {
            image.dispose();
        }
    }

    private static Check color(String name, ImageData image, int x, Color expected) {
        // The image may have more pixels than the control has points (the zoom of the display)
        double scale = (double) image.width / CANVAS_WIDTH;
        RGB actual = image.palette.getRGB(image.getPixel((int) (x * scale), (int) (CANVAS_HEIGHT / 2 * scale)));
        boolean close = Math.abs(actual.red - (int) Math.round(expected.getRed() * 255)) <= TOLERANCE
                && Math.abs(actual.green - (int) Math.round(expected.getGreen() * 255)) <= TOLERANCE
                && Math.abs(actual.blue - (int) Math.round(expected.getBlue() * 255)) <= TOLERANCE;
        return Check.of(name, close, close ? "the color of the scene" : String.format("#%02x%02x%02x, expected %s",
                actual.red, actual.green, actual.blue, expected));
    }

    /**
     * The pixels whose color differs between a JavaFX image and SWT image data (the alpha aside).
     */
    private static int differences(Image image, ImageData data) {
        PixelReader reader = image.getPixelReader();
        int differences = 0;
        for (int y = 0; y < data.height; y++) {
            for (int x = 0; x < data.width; x++) {
                RGB rgb = data.palette.getRGB(data.getPixel(x, y));
                if ((reader.getArgb(x, y) & 0xffffff) != (rgb.red << 16 | rgb.green << 8 | rgb.blue)) {
                    differences++;
                }
            }
        }
        return differences + Math.abs((int) image.getWidth() * (int) image.getHeight() - data.width * data.height);
    }

    /**
     * SWT image data as a JavaFX image (SWTFXUtils.toFXImage no longer converts it, see the checks).
     */
    private static Image toFxImage(ImageData data) {
        WritableImage image = new WritableImage(data.width, data.height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < data.height; y++) {
            for (int x = 0; x < data.width; x++) {
                RGB rgb = data.palette.getRGB(data.getPixel(x, y));
                int alpha = data.alphaData == null && data.alpha == -1 ? 255 : data.getAlpha(x, y);
                writer.setArgb(x, y, alpha << 24 | rgb.red << 16 | rgb.green << 8 | rgb.blue);
            }
        }
        return image;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        Object ready = content.getProperties().get(READY);
        return ready instanceof CompletionStage<?> stage ? stage : CompletableFuture.completedFuture(null);
    }

    @Override
    public void dispose(Node content) {
        if (content.getProperties().remove(SHELL) instanceof Shell shell && !shell.isDisposed()) {
            shell.dispose();
        }
    }
}
