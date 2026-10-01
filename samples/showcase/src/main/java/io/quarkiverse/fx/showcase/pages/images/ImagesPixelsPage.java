package io.quarkiverse.fx.showcase.pages.images;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms.Families;
import io.quarkiverse.fx.showcase.pages.text.Ui;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.SnapshotResult;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;

/**
 * Pixel level APIs : PixelWriter / PixelReader on WritableImage in every pixel format, PixelBuffer with dirty region
 * updates, copies of images and node snapshots.
 */
@Singleton
public class ImagesPixelsPage implements FeaturePage {

    private static final String READY = "images-pixels.ready";
    static final int W = 160;
    static final int H = 100;
    static final int[] PALETTE = { 0xFF263238, 0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0x80FFFFFF };

    @Override
    public String id() {
        return "images-pixels";
    }

    @Override
    public String title() {
        return "Pixels & WritableImage";
    }

    @Override
    public String category() {
        return Categories.IMAGES_CANVAS;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        List<Check> checks = new ArrayList<>();
        List<Node> cells = new ArrayList<>();
        ImageLoads loads = new ImageLoads();
        Image photo = loads.resource("/showcase/images/photo.jpg", 480, 320);
        Image png = loads.resource("/showcase/images/pattern.png", 256, 256);
        Image gif = loads.resource("/showcase/images/pattern.gif", 256, 256);

        // 1. setArgb gradient
        WritableImage gradient = new WritableImage(W, H);
        PixelWriter gw = gradient.getPixelWriter();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                gw.setArgb(x, y, gradientArgb(x, y));
            }
        }
        cells.add(cell("PixelWriter.setArgb gradient", new ImageView(gradient)));

        // 2. setPixels with an int[] (INT_ARGB) pattern
        WritableImage ints = new WritableImage(W, H);
        int[] pattern = new int[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                boolean check = ((x / 10) + (y / 10)) % 2 == 0;
                int ring = (int) Math.hypot(x - W / 2.0, y - H / 2.0) / 8;
                pattern[y * W + x] = check ? (ring % 2 == 0 ? 0xFF3949AB : 0xFF7986CB) : (ring % 2 == 0 ? 0xFFFFB300
                        : 0xFFFFE082);
            }
        }
        ints.getPixelWriter().setPixels(0, 0, W, H, PixelFormat.getIntArgbInstance(), pattern, 0, W);
        cells.add(cell("setPixels(int[], INT_ARGB) pattern", new ImageView(ints)));

        // 3. setColor with alpha, and BYTE_BGRA_PRE bytes, over a checker
        WritableImage colors = new WritableImage(W, H);
        PixelWriter cw = colors.getPixelWriter();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                cw.setColor(x, y, Color.hsb(x * 360.0 / W, 0.8, 0.95, 0.25 + 0.75 * y / (H - 1)));
            }
        }
        byte[] bgraPre = new byte[40 * 30 * 4];
        for (int i = 0; i < 40 * 30; i++) {
            // premultiplied half transparent black
            bgraPre[i * 4] = 0;
            bgraPre[i * 4 + 1] = 0;
            bgraPre[i * 4 + 2] = 0;
            bgraPre[i * 4 + 3] = (byte) 0x80;
        }
        cw.setPixels(110, 60, 40, 30, PixelFormat.getByteBgraPreInstance(), bgraPre, 0, 40 * 4);
        StackPane overChecker = new StackPane(checker(W, H), new ImageView(colors));
        cells.add(cell("setColor(hsb, alpha) + BGRA_PRE", overChecker));

        // 4. indexed (palette) pixels
        WritableImage indexed = new WritableImage(W, H);
        byte[] indices = new byte[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                indices[y * W + x] = (byte) (((x / 20) + (y / 25) * 2) % PALETTE.length);
            }
        }
        indexed.getPixelWriter().setPixels(0, 0, W, H, PixelFormat.createByteIndexedInstance(PALETTE), indices, 0, W);
        cells.add(cell("setPixels BYTE_INDEXED palette", new StackPane(checker(W, H), new ImageView(indexed))));

        // 5. PixelBuffer<IntBuffer>, updated with a dirty region once shown
        IntBuffer intBuffer = IntBuffer.allocate(W * H);
        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer<>(W, H, intBuffer, PixelFormat.getIntArgbPreInstance());
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                intBuffer.put(y * W + x, 0xFF000000 | ((x * 255 / (W - 1)) << 8) | (0x60 + y));
            }
        }
        WritableImage bufferImage = new WritableImage(pixelBuffer);
        cells.add(cell("PixelBuffer<IntBuffer>, dirty", new ImageView(bufferImage)));

        // 6. PixelBuffer<ByteBuffer> (BYTE_BGRA_PRE, direct buffer)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(W * H * 4);
        PixelBuffer<ByteBuffer> bytePixelBuffer = new PixelBuffer<>(W, H, byteBuffer, PixelFormat.getByteBgraPreInstance());
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int i = (y * W + x) * 4;
                boolean stripe = ((x + y) / 12) % 2 == 0;
                byteBuffer.put(i, (byte) (stripe ? 0x30 : 0xE0)); // B
                byteBuffer.put(i + 1, (byte) (stripe ? 0x90 : 0x70)); // G
                byteBuffer.put(i + 2, (byte) (stripe ? 0xF0 : 0x20)); // R
                byteBuffer.put(i + 3, (byte) 0xFF); // A
            }
        }
        WritableImage byteBufferImage = new WritableImage(bytePixelBuffer);
        cells.add(cell("PixelBuffer<ByteBuffer>, dirty", new StackPane(checker(W, H), new ImageView(
                byteBufferImage))));

        // 7. copy of a region of the JPEG
        WritableImage crop = new WritableImage(photo.getPixelReader(), 150, 110, W, H);
        cells.add(cell("WritableImage(reader, rect)", new ImageView(crop)));

        // 8. copy of the PNG, inverted
        WritableImage inverted = new WritableImage(png.getPixelReader(), (int) png.getWidth(), (int) png.getHeight());
        PixelReader ir = inverted.getPixelReader();
        PixelWriter iw = inverted.getPixelWriter();
        for (int y = 0; y < inverted.getHeight(); y++) {
            for (int x = 0; x < inverted.getWidth(); x++) {
                int argb = ir.getArgb(x, y);
                iw.setArgb(x, y, (argb & 0xFF000000) | (~argb & 0x00FFFFFF));
            }
        }
        ImageView invertedView = new ImageView(inverted);
        invertedView.setFitHeight(H);
        invertedView.setPreserveRatio(true);
        cells.add(cell("copy of pattern.png, RGB inverted", new StackPane(checker(W, H), invertedView)));

        // 9. node snapshot, shown scaled x2 without smoothing
        Group group = snapshotGroup();
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.web("#fffde7"));
        WritableImage snapshot = group.snapshot(params, null);
        ImageView snapshotView = new ImageView(snapshot);
        snapshotView.setFitWidth(snapshot.getWidth() * 2);
        snapshotView.setPreserveRatio(true);
        snapshotView.setSmooth(false);
        cells.add(cell("Group.snapshot(fill), shown x2", snapshotView));

        // 10. snapshot with a transform, a viewport and a transparent fill
        SnapshotParameters params2 = new SnapshotParameters();
        params2.setFill(Color.TRANSPARENT);
        params2.setTransform(Transform.rotate(-20, 40, 25).createConcatenation(Transform.scale(1.6, 1.6)));
        params2.setViewport(new Rectangle2D(-10, -10, W, H));
        WritableImage transformed = snapshotGroup().snapshot(params2, null);
        cells.add(cell("snapshot: transform, viewport", new StackPane(checker(W, H), new ImageView(
                transformed))));

        // 11. grayscale : getPixels(BYTE_BGRA) then setPixels(BYTE_RGB)
        byte[] bgra = new byte[W * H * 4];
        photo.getPixelReader().getPixels(200, 150, W, H, PixelFormat.getByteBgraInstance(), bgra, 0, W * 4);
        byte[] rgb = new byte[W * H * 3];
        for (int i = 0; i < W * H; i++) {
            int b = bgra[i * 4] & 0xFF;
            int g = bgra[i * 4 + 1] & 0xFF;
            int r = bgra[i * 4 + 2] & 0xFF;
            int gray = (r * 299 + g * 587 + b * 114) / 1000;
            rgb[i * 3] = rgb[i * 3 + 1] = rgb[i * 3 + 2] = (byte) gray;
        }
        WritableImage grayscale = new WritableImage(W, H);
        grayscale.getPixelWriter().setPixels(0, 0, W, H, PixelFormat.getByteRgbInstance(), rgb, 0, W * 3);
        cells.add(cell("BYTE_BGRA → gray BYTE_RGB", new ImageView(grayscale)));

        // 12. horizontal mirror through an IntBuffer (INT_ARGB_PRE)
        IntBuffer mirrorBuffer = IntBuffer.allocate(128 * H);
        png.getPixelReader().getPixels(64, 78, 128, H, PixelFormat.getIntArgbPreInstance(), mirrorBuffer, 128);
        IntBuffer flipped = IntBuffer.allocate(128 * H);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < 128; x++) {
                flipped.put(y * 128 + x, mirrorBuffer.get(y * 128 + 127 - x));
            }
        }
        WritableImage mirrored = new WritableImage(128, H);
        mirrored.getPixelWriter().setPixels(0, 0, 128, H, PixelFormat.getIntArgbPreInstance(), flipped, 128);
        cells.add(cell("IntBuffer read, mirrored write", new StackPane(checker(W, H), new ImageView(mirrored))));

        // 13. a WritableImage modified after it was displayed (see ready)
        WritableImage live = new WritableImage(W, H);
        PixelWriter lw = live.getPixelWriter();
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                lw.setArgb(x, y, ((x / 16) % 2 == 0) ? 0xFF26A69A : 0xFF80CBC4);
            }
        }
        cells.add(cell("PixelWriter update after display", new ImageView(live)));

        // 14. off-screen Scene snapshot (controls styled by the default user agent stylesheet)
        Button button = new Button("Button");
        CheckBox box = new CheckBox("CheckBox");
        box.setSelected(true);
        ProgressBar progress = new ProgressBar(0.6);
        progress.setPrefWidth(140);
        VBox controls = new VBox(6, button, box, progress);
        controls.setStyle("-fx-padding: 6; -fx-background-color: #eceff1;");
        Scene offscreen = new Scene(controls, W, H);
        WritableImage sceneImage = offscreen.snapshot(null);
        cells.add(cell("Scene.snapshot of an off-screen scene", new ImageView(sceneImage)));

        // 15. snapshot of nodes with effects
        Text fx = new Text("FX");
        fx.setFont(Font.font(Families.sans(), FontWeight.BOLD, 44)); // "Helvetica Neue" on macOS
        fx.setFill(Color.web("#ef6c00"));
        fx.setEffect(new DropShadow(6, 3, 3, Color.web("#00000080")));
        Circle glow = new Circle(22, Color.web("#7e57c2"));
        glow.setEffect(new InnerShadow(10, Color.WHITE));
        HBox effects = new HBox(10, fx, glow);
        effects.setStyle("-fx-padding: 8;");
        SnapshotParameters params3 = new SnapshotParameters();
        params3.setFill(Color.WHITE);
        // asynchronous variant : the callback runs once the next pulse rendered the snapshot
        ImageView effectsView = new ImageView();
        CompletableFuture<SnapshotResult> effectsSnapshot = new CompletableFuture<>();
        effects.snapshot(result -> {
            effectsView.setImage(result.getImage());
            effectsSnapshot.complete(result);
            return null;
        }, params3, null);
        cells.add(cell("snapshot(callback), effects", effectsView));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < cells.size(); i++) {
            grid.add(cells.get(i), i % 5, i / 5);
        }

        // Checks
        checks.add(Checks.expect("gradient getArgb (0,0) (159,0) (0,99) (159,99)",
                String.join(", ", Ui.argb(gradientArgb(0, 0)), Ui.argb(gradientArgb(159, 0)), Ui.argb(gradientArgb(0, 99)),
                        Ui.argb(gradientArgb(159, 99))),
                () -> String.join(", ", argb(gradient, 0, 0), argb(gradient, 159, 0), argb(gradient, 0, 99),
                        argb(gradient, 159, 99))));
        checks.add(Checks.run("getColor (0,0) (80,99), BGRA_PRE (120,70)", () -> {
            int[] pre = new int[1];
            colors.getPixelReader().getPixels(120, 70, 1, 1, PixelFormat.getIntArgbPreInstance(), pre, 0, 1);
            return colors.getPixelReader().getColor(0, 0) + " " + colors.getPixelReader().getColor(80, 99) + " / ARGB "
                    + argb(colors, 120, 70) + ", ARGB_PRE " + Ui.argb(pre[0]);
        }));
        checks.add(Checks.expect("BYTE_INDEXED (0,0) (20,0) (0,25) (100,0)",
                String.join(", ", Ui.argb(PALETTE[0]), Ui.argb(PALETTE[1]), Ui.argb(PALETTE[2]), Ui.argb(PALETTE[5])),
                () -> String.join(", ", argb(indexed, 0, 0), argb(indexed, 20, 0), argb(indexed, 0, 25),
                        argb(indexed, 100, 0))));
        checks.add(Checks.expect("read back = written: int[] / crop copy", "true / true", () -> {
            int[] back = new int[W * H];
            ints.getPixelReader().getPixels(0, 0, W, H, PixelFormat.getIntArgbInstance(), back, 0, W);
            boolean same = true;
            for (int y = 0; y < H; y += 7) {
                for (int x = 0; x < W; x += 7) {
                    same &= crop.getPixelReader().getArgb(x, y) == photo.getPixelReader().getArgb(150 + x, 110 + y);
                }
            }
            return java.util.Arrays.equals(back, pattern) + " / " + same;
        }));
        checks.add(Checks.run("inverted pattern (10,10) (128,128)", () -> argb(png, 10, 10) + "→" + argb(inverted, 10, 10)
                + ", " + argb(png, 128, 128) + "→" + argb(inverted, 128, 128)));
        checks.add(Checks.run("snapshots: group / transformed / scene", () -> ImagesFormatsPage.size(snapshot) + " "
                + argb(snapshot, 10, 10) + " " + argb(snapshot, 45, 35) + " / " + ImagesFormatsPage.size(transformed)
                + " " + argb(transformed, 0, 0) + " / " + ImagesFormatsPage.size(sceneImage) + " " + argb(sceneImage, 2,
                        2)));
        checks.add(Checks.run("gray (0,0) (80,50), mirror (0,0)=png(191,78)", () -> argb(grayscale, 0, 0) + " "
                + argb(grayscale, 80, 50) + " / " + argb(mirrored, 0, 0) + " = " + argb(png, 191, 78)));
        checks.add(Checks.run("PixelReader.getPixelFormat()", () -> {
            Map<String, List<String>> kinds = new LinkedHashMap<>();
            Map<String, Image> images = new LinkedHashMap<>();
            images.put("png", png);
            images.put("jpg", photo);
            images.put("gif", gif);
            images.put("writable", gradient);
            images.put("IntBuffer", bufferImage);
            images.put("ByteBuffer", byteBufferImage);
            images.put("snapshot", snapshot);
            images.forEach((kind, image) -> kinds.computeIfAbsent(image.getPixelReader().getPixelFormat().getType()
                    .name(), k -> new ArrayList<>()).add(kind));
            List<String> parts = new ArrayList<>();
            kinds.forEach((format, names) -> parts.add(String.join(" ", names) + ": " + format));
            return String.join(" / ", parts);
        }));
        checks.add(Checks.run("PixelFormat isWritable / isPremultiplied", () -> String.join(", ",
                flags(PixelFormat.getIntArgbInstance()), flags(PixelFormat.getByteRgbInstance()),
                flags(PixelFormat.createByteIndexedInstance(PALETTE)))));
        checks.add(loads.check());

        VBox checksHolder = new VBox(Checks.view("Pixel read back", checks));
        VBox root = new VBox(8, grid, checksHolder);
        root.setPrefWidth(1028);
        root.setMaxWidth(1028);

        // update the pixel buffers once they were rendered : only the dirty regions are uploaded again
        CompletionStage<?> ready = Fx.pulses(2).thenRun(() -> {
            pixelBuffer.updateBuffer(pb -> {
                IntBuffer b = pb.getBuffer();
                for (int y = 30; y < 70; y++) {
                    for (int x = 50; x < 110; x++) {
                        boolean border = x < 53 || x >= 107 || y < 33 || y >= 67;
                        b.put(y * W + x, border ? 0xFF000000 : 0xFFFFD600);
                    }
                }
                return new Rectangle2D(50, 30, 60, 40);
            });
            bytePixelBuffer.updateBuffer(pb -> {
                ByteBuffer b = pb.getBuffer();
                // half transparent white square (premultiplied : 0x80 components)
                for (int y = 20; y < 80; y++) {
                    for (int x = 20; x < 80; x++) {
                        int i = (y * W + x) * 4;
                        b.put(i, (byte) 0x80).put(i + 1, (byte) 0x80).put(i + 2, (byte) 0x80).put(i + 3, (byte) 0x80);
                    }
                }
                return new Rectangle2D(20, 20, 60, 60);
            });
            PixelWriter writer = live.getPixelWriter();
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int d = Math.abs(x - y * W / H);
                    if (d < 12) {
                        writer.setArgb(x, y, d < 4 ? 0xFFFFFFFF : 0xFFE53935);
                    }
                }
            }
        }).thenCompose(v -> Fx.pulses(2))
                .thenCompose(v -> Fx.timeout(effectsSnapshot, 10_000, "snapshot callback")
                        .handle((result, error) -> error == null ? Check.pass("snapshot(callback): size, (20,20)",
                                ImagesFormatsPage.size(result.getImage()) + ", " + argb(result.getImage(), 20, 20)
                                        + ", source " + result.getSource().getClass().getSimpleName())
                                : Check.fail("snapshot(callback): size, (20,20)", Checks.describe(error))))
                .thenAccept(snapshotCheck -> {
                    List<Check> all = new ArrayList<>(checks);
                    all.add(snapshotCheck);
                    all.add(Checks.run("after update: buffers / live image", () -> "int (80,50) "
                            + argb(bufferImage, 80, 50) + " (0,0) " + argb(bufferImage, 0, 0) + ", byte (30,30) "
                            + argb(byteBufferImage, 30, 30) + " / live (80,50) " + argb(live, 80, 50) + " (8,50) "
                            + argb(live, 8, 50)));
                    checksHolder.getChildren().setAll(Checks.view("Pixel read back", all));
                });
        root.getProperties().put(READY, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return (CompletionStage<?>) content.getProperties().get(READY);
    }

    static int gradientArgb(int x, int y) {
        int r = x * 255 / (W - 1);
        int g = y * 255 / (H - 1);
        int b = 255 - (x + y) * 255 / (W + H - 2);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static Group snapshotGroup() {
        Rectangle background = new Rectangle(0, 0, 80, 56);
        background.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#bbdefb")),
                new Stop(1, Color.web("#e1bee7"))));
        Rectangle solid = new Rectangle(4, 4, 20, 14);
        solid.setFill(Color.web("#e53935"));
        Circle circle = new Circle(45, 35, 14, Color.web("#43a047"));
        Polygon triangle = new Polygon(58, 52, 78, 52, 68, 34);
        triangle.setFill(Color.web("#fb8c00"));
        Text text = new Text(28, 16, "FX");
        text.setFont(Font.font(Families.helvetica(), FontWeight.BOLD, 14)); // "Helvetica" on macOS
        text.setFill(Color.web("#0d47a1"));
        return new Group(background, solid, circle, triangle, text);
    }

    private static Node checker(int w, int h) {
        WritableImage image = new WritableImage(w, h);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                writer.setArgb(x, y, ((x / 8) + (y / 8)) % 2 == 0 ? 0xFFE0E0E0 : 0xFFFFFFFF);
            }
        }
        return new ImageView(image);
    }

    static VBox cell(String caption, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPrefSize(W, H + 2);
        holder.setMinSize(W, H + 2);
        holder.setMaxSize(W, H + 2);
        VBox tile = Ui.tile(caption, holder);
        tile.setPrefWidth(W + 38);
        tile.setMaxWidth(W + 38);
        return tile;
    }

    private static String flags(PixelFormat<?> format) {
        return format.getType() + " " + format.isWritable() + "/" + format.isPremultiplied();
    }

    private static String argb(Image image, int x, int y) {
        return Ui.argb(image.getPixelReader().getArgb(x, y));
    }
}
