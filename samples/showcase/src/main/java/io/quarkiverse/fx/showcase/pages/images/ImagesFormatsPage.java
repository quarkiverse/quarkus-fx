package io.quarkiverse.fx.showcase.pages.images;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import io.quarkiverse.fx.showcase.pages.text.Ui;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

/**
 * Image loading : PNG, JPEG, GIF, BMP, data URI and stream sources, resource lookups (URL without scheme, hi-DPI
 * {@code @2x} and {@code @1x} variants), ImageView options (fit, ratio, smooth, viewport, rotation), requested sizes,
 * background loading and error reporting.
 */
@Singleton
public class ImagesFormatsPage implements FeaturePage {

    private static final String READY = "images-formats.ready";

    static final String PNG = "/showcase/images/pattern.png";
    static final String JPG = "/showcase/images/photo.jpg";
    static final String GIF = "/showcase/images/pattern.gif";
    static final String BMP = "/showcase/images/pattern.bmp";
    static final String TEXTURE = "/showcase/images/texture.png";
    static final String ICON = "/showcase/images/icon.png";
    static final String ANIMATED = "/showcase/images/animated.gif";
    /** 48x48 blue "1x", with a 96x96 orange "2x" scaled@2x.png variant. */
    static final String SCALED = "/showcase/images-pages/scaled.png";

    /** 8x8 RGBA PNG : red, green, blue and half transparent amber quadrants. */
    static final String DATA_URI = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAJUlEQVR42mN4amn6"
            + "Hxk7L3BHwQx0UCDX8fQ/Mv5/kL0BGdNBAQB0PZTBuob4+gAAAABJRU5ErkJggg==";
    static final String INVALID_DATA_URI = "data:image/png;base64,bm90IGFuIGltYWdl";

    static final double CELL = 150;

    @Override
    public String id() {
        return "images-formats";
    }

    @Override
    public String title() {
        return "Image Formats & ImageView";
    }

    @Override
    public String category() {
        return Categories.IMAGES_CANVAS;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() throws Exception {
        List<Check> checks = new ArrayList<>();
        List<Node> cells = new ArrayList<>();
        ImageLoads loads = new ImageLoads();

        Image png = loads.resource(PNG, 256, 256);
        Image jpg = loads.resource(JPG, 480, 320);
        Image gif = loads.resource(GIF, 256, 256);
        Image bmp = loads.resource(BMP, 256, 256);
        Image texture = loads.resource(TEXTURE, 256, 256);
        Image icon = loads.resource(ICON, 64, 64);
        Image data = loads.load("data URI", 8, 8, () -> new Image(DATA_URI));
        Image stream = loads.load("Image(InputStream)", 480, 320, () -> {
            try (InputStream in = Fx.resource(JPG).openStream()) {
                return new Image(in);
            }
        });
        // an URL without scheme is looked up with the context class loader of the calling thread
        Image noScheme = loads.load("Image(\"" + ICON + "\")", 64, 64, () -> new Image(ICON));
        // on a hi-DPI screen, JavaFX looks for name@2x.png first; when name.png does not exist, it falls back to
        // name@1x.png (only fallback@1x.png exists, a 48x48 green "@1x")
        Image scaled = loads.resource(SCALED, 48, 48);
        Image fallback = loads.load("fallback(@1x).png", 48, 48, () -> new Image(Fx.resourceUrl(SCALED).replace(
                "scaled.png", "fallback.png")));

        // formats
        StackPane alpha = new StackPane(view(texture, 104, 104, true, true), view(png, 104, 104, true, true));
        cells.add(cell("PNG alpha over texture", alpha));
        cells.add(cell("JPEG, fitWidth 150", view(jpg, 150, 0, true, true)));
        cells.add(cell("GIF", view(gif, 104, 104, true, true)));
        cells.add(cell("BMP", view(bmp, 104, 104, true, true)));
        HBox dataBox = new HBox(10, new ImageView(data), view(data, 48, 48, true, false));
        dataBox.setAlignment(Pos.CENTER);
        cells.add(cell("data: URI 8x8 PNG, 1:1 and x6", dataBox));
        cells.add(cell("Image(InputStream), JPEG", view(stream, 150, 0, true, true)));

        // ImageView options
        cells.add(cell("fit 150x80, no ratio", view(jpg, 150, 80, false, true)));
        cells.add(cell("fit 150x80, preserveRatio", view(jpg, 150, 80, true, true)));
        cells.add(cell("8x8 at 96, smooth true", view(data, 96, 96, true, true)));
        // NGImageView.setSmooth is empty (JDK-8127343) : Prism always filters, see "requested 32px, rough" instead
        cells.add(cell("smooth false: no-op in Prism", view(data, 96, 96, true, false)));
        ImageView viewport = view(jpg, 150, 0, true, true);
        viewport.setViewport(new Rectangle2D(160, 100, 180, 120));
        cells.add(cell("viewport 160,100 180x120", viewport));
        ImageView rotated = view(noScheme, 64, 64, true, true);
        rotated.setRotate(30);
        cells.add(cell("rotate 30, URL without scheme", rotated));

        // requested sizes, background loading, hi-DPI variants, animated GIF
        Image requestedSmooth = loads.load("requested 32 smooth", 32, 32, () -> new Image(Fx.resourceUrl(PNG), 32, 32,
                false, true));
        Image requestedRough = loads.load("requested 32 rough", 32, 32, () -> new Image(Fx.resourceUrl(PNG), 32, 32,
                false, false));
        Image requestedRatio = loads.load("requested 100 ratio", 100, 67, () -> new Image(Fx.resourceUrl(JPG), 100, 100,
                true, true));
        cells.add(cell("requested 32px, smooth", view(requestedSmooth, 96, 96, true, false)));
        cells.add(cell("requested 32px, rough", view(requestedRough, 96, 96, true, false)));
        cells.add(cell("requested 100, ratio", view(requestedRatio, 0, 0, true, true)));

        Image background = new Image(Fx.resourceUrl(JPG), 150, 0, true, true, true);
        cells.add(cell("background loading", view(background, 150, 0, true, true)));

        Label scaledName = Ui.caption("scaled.png");
        Label fallbackName = Ui.caption("fallback.png");
        HBox variants = new HBox(12, new VBox(2, new ImageView(scaled), scaledName), new VBox(2, new ImageView(fallback),
                fallbackName));
        variants.setAlignment(Pos.CENTER);
        cells.add(cell("hi-DPI @2x variant, @1x fallback", variants));

        Image animated = loads.resource(ANIMATED, 96, 96);
        if (ShowcaseMode.snapshot()) {
            Label info = new Label("animated.gif\n" + (int) animated.getWidth() + " x " + (int) animated.getHeight()
                    + "\n(animation shown\noutside snapshots)");
            info.setStyle("-fx-font-size: 11px; -fx-text-fill: #455a64;");
            cells.add(cell("animated GIF (snapshot)", info));
        } else {
            cells.add(cell("animated GIF", view(animated, 96, 96, true, true)));
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < cells.size(); i++) {
            grid.add(cells.get(i), i % 6, i / 6);
        }

        // error cases : kept out of ImageLoads, the error is the expected outcome
        String missingUrl = Fx.resourceUrl(PNG).replace("pattern.png", "missing-image.png");
        Check missing = errorCheck(false, () -> new Image(missingUrl));
        Check invalid = errorCheck(true, () -> new Image(INVALID_DATA_URI));
        checks.add(Check.of("missing resource / invalid data URI", missing.ok() && invalid.ok(), missing.value()
                + " / " + invalid.value()));
        checks.add(Checks.run("Image(\"/showcase/no-such.png\") (no scheme)", () -> {
            try {
                Image image = new Image("/showcase/images-pages/no-such.png");
                return "no exception, isError " + image.isError();
            } catch (IllegalArgumentException e) {
                return e.getClass().getName() + ": " + e.getMessage();
            }
        }));

        checks.add(loads.check());
        checks.add(Check.info("sizes png/jpg/gif/bmp/data/stream", String.join(", ", size(png), size(jpg), size(gif),
                size(bmp), size(data), size(stream))));
        checks.add(Checks.run("ARGB png/gif/bmp (10,10), (200,60)", () -> String.join(" ", argb(png, 10, 10),
                argb(gif, 10, 10), argb(bmp, 10, 10)) + " / " + String.join(" ", argb(png, 200, 60),
                        argb(gif, 200, 60), argb(bmp, 200, 60))));
        checks.add(Checks.run("ARGB jpg(40,40) (240,160), stream", () -> String.join(", ", argb(jpg, 40, 40),
                argb(jpg, 240, 160), argb(stream, 240, 160))));
        checks.add(Checks.run("ARGB data URI (1,1) (6,1) (1,6) (6,6)", () -> String.join(", ", argb(data, 1, 1),
                argb(data, 6, 1), argb(data, 1, 6), argb(data, 6, 6))));
        checks.add(Checks.run("requested smooth/rough/ratio, ARGB(16,16)", () -> String.join(", ", size(requestedSmooth),
                size(requestedRough), size(requestedRatio)) + " / " + argb(requestedSmooth, 16, 16) + " vs "
                + argb(requestedRough, 16, 16)));
        checks.add(Checks.expect("no scheme = full URL: size, ARGB(32,32)", "64x64 " + argb(icon, 32, 32),
                () -> size(noScheme) + " " + argb(noScheme, 32, 32)));
        boolean hiDpi = Screen.getScreens().stream()
                .mapToDouble(s -> Math.max(s.getOutputScaleX(), s.getOutputScaleY())).max().orElse(1) >= 1.5;
        String scaledVariant = variant(scaled);
        String fallbackVariant = variant(fallback);
        checks.add(Check.of("hi-DPI " + hiDpi + ": scaled.png / fallback.png", scaledVariant.startsWith(hiDpi ? "@2x"
                : "1x") && fallbackVariant.startsWith("@1x"), scaledVariant + " " + size(scaled) + " / "
                        + fallbackVariant + " " + size(fallback)));
        checks.add(Check.info("animated GIF size / progress / error", size(animated) + ", " + animated.getProgress()
                + ", " + animated.isError()));

        VBox checksHolder = new VBox(Checks.view("Image loading", checks));
        VBox root = new VBox(8, grid, checksHolder);
        root.setPrefWidth(1028);
        root.setMaxWidth(1028);

        // a failed background load reports an error, without necessarily reaching progress 1
        CompletionStage<?> ready = Fx.when(Bindings.createBooleanBinding(
                () -> background.getProgress() >= 1.0 || background.isError(), background.progressProperty(),
                background.errorProperty()), done -> done)
                .thenCompose(p -> Fx.pulses(1))
                .thenRun(() -> {
                    List<Check> all = new ArrayList<>(checks);
                    all.add(Check.of("background image progress / error / size", !background.isError(),
                            background.getProgress() + ", " + background.isError() + ", " + size(background)
                                    + (background.getException() == null ? ""
                                            : ", " + Checks.describe(background.getException()))));
                    checksHolder.getChildren().setAll(Checks.view("Image loading", all));
                });
        root.getProperties().put(READY, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return (CompletionStage<?>) content.getProperties().get(READY);
    }

    /**
     * An image expected to fail : isError and the exception, or what was thrown. Only the exception class is shown
     * when {@code messages} is false: the message of a missing resource depends on the URL handler (jar: in JVM mode,
     * resource: in a native image).
     */
    private static Check errorCheck(boolean messages, java.util.concurrent.Callable<Image> loader) {
        try {
            Image image = loader.call();
            Exception e = image.getException();
            return Check.of("", image.isError(), image.isError() + ", " + (e == null ? "null"
                    : messages ? Checks.describe(e) : e.getClass().getName()));
        } catch (Throwable t) {
            return Check.fail("", "thrown: " + Checks.describe(t));
        }
    }

    /**
     * Which of the generated variants was loaded, from the color of its background.
     */
    private static String variant(Image image) {
        int argb = image.getPixelReader().getArgb(24, 10);
        String name = switch (argb) {
            case 0xFF1565C0 -> "1x";
            case 0xFFEF6C00 -> "@2x";
            case 0xFF2E7D32 -> "@1x";
            default -> "?";
        };
        return name + " (" + Ui.argb(argb) + ")";
    }

    static ImageView view(Image image, double fitWidth, double fitHeight, boolean preserveRatio, boolean smooth) {
        ImageView view = new ImageView(image);
        view.setFitWidth(fitWidth);
        view.setFitHeight(fitHeight);
        view.setPreserveRatio(preserveRatio);
        view.setSmooth(smooth);
        return view;
    }

    static VBox cell(String caption, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPrefSize(CELL, 104);
        holder.setMinSize(CELL, 104);
        holder.setMaxSize(CELL, 104);
        holder.setAlignment(Pos.CENTER);
        VBox tile = Ui.tile(caption, holder);
        tile.setPrefWidth(CELL + 14);
        tile.setMaxWidth(CELL + 14);
        return tile;
    }

    static String size(Image image) {
        return (int) image.getWidth() + "x" + (int) image.getHeight();
    }

    static String argb(Image image, int x, int y) {
        return Ui.argb(image.getPixelReader().getArgb(x, y));
    }
}
