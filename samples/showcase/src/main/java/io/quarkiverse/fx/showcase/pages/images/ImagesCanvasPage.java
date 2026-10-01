package io.quarkiverse.fx.showcase.pages.images;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms.Families;
import io.quarkiverse.fx.showcase.pages.text.PageFonts;
import io.quarkiverse.fx.showcase.pages.text.Ui;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

/**
 * Canvas : a sweep of the GraphicsContext API, one small canvas per feature.
 */
@Singleton
public class ImagesCanvasPage implements FeaturePage {

    private static final String READY = "images-canvas.ready";
    static final double W = 150;
    static final double H = 92;

    @Override
    public String id() {
        return "images-canvas";
    }

    @Override
    public String title() {
        return "Canvas";
    }

    @Override
    public String category() {
        return Categories.IMAGES_CANVAS;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Node build() {
        ImageLoads loads = new ImageLoads();
        Image icon = loads.resource("/showcase/images/icon.png", 64, 64);
        Image small = loads.load("data URI", 8, 8, () -> new Image(ImagesFormatsPage.DATA_URI));
        Image smallCopy = loads.load("data URI (2)", 8, 8, () -> new Image(ImagesFormatsPage.DATA_URI));
        Image photo = loads.resource("/showcase/images/photo.jpg", 480, 320);
        Image tile = loads.resource("/showcase/images/tile.png", 32, 32);
        Image texture = loads.resource("/showcase/images/texture.png", 256, 256);

        Map<String, Canvas> canvases = new LinkedHashMap<>();
        List<Check> checks = new ArrayList<>();
        // "Georgia", "Helvetica Neue", "Menlo", "Geeza Pro" and "Hiragino Sans" on macOS
        String georgia = PageFonts.georgia();
        String sans = Families.sans();
        String mono = Families.mono();
        String arabic = Families.arabic();
        String japanese = Families.japanese();

        canvases.put("shapes, line, polygon", draw(gc -> {
            gc.setLineWidth(2);
            gc.setFill(Color.web("#42a5f5"));
            gc.fillRect(8, 8, 40, 30);
            gc.setStroke(Color.web("#1565c0"));
            gc.strokeRect(8, 8, 40, 30);
            gc.setFill(Color.web("#66bb6a"));
            gc.fillRoundRect(56, 8, 40, 30, 14, 14);
            gc.setStroke(Color.web("#2e7d32"));
            gc.strokeRoundRect(56, 8, 40, 30, 14, 14);
            gc.setFill(Color.web("#ffa726"));
            gc.fillOval(104, 8, 40, 30);
            gc.setStroke(Color.web("#e65100"));
            gc.strokeOval(104, 8, 40, 30);
            gc.setStroke(Color.web("#6d4c41"));
            gc.strokeLine(8, 50, 48, 88);
            gc.setFill(Color.web("#ab47bc"));
            gc.fillPolygon(new double[] { 56, 96, 76 }, new double[] { 88, 88, 50 }, 3);
            gc.setStroke(Color.web("#00897b"));
            gc.strokePolyline(new double[] { 104, 114, 124, 134, 144 }, new double[] { 88, 52, 88, 52, 88 }, 5);
        }));

        canvases.put("arcs ROUND, CHORD, OPEN", draw(gc -> {
            ArcType[] types = { ArcType.ROUND, ArcType.CHORD, ArcType.OPEN };
            Color[] fills = { Color.web("#ef5350"), Color.web("#26a69a"), Color.web("#7e57c2") };
            for (int i = 0; i < types.length; i++) {
                gc.setFill(fills[i]);
                gc.fillArc(8 + i * 48, 4, 40, 40, 30, 270, types[i]);
                gc.setStroke(fills[i].darker());
                gc.setLineWidth(2);
                gc.strokeArc(8 + i * 48, 50, 40, 40, 30, 270, types[i]);
            }
        }));

        canvases.put("path: quadratic, bezier", draw(gc -> {
            gc.beginPath();
            gc.moveTo(8, 86);
            gc.lineTo(30, 20);
            gc.quadraticCurveTo(60, -4, 80, 40);
            gc.bezierCurveTo(100, 96, 124, -10, 144, 50);
            gc.lineTo(144, 86);
            gc.closePath();
            gc.setFill(Color.web("#ce93d8"));
            gc.fill();
            gc.setStroke(Color.web("#6a1b9a"));
            gc.setLineWidth(2.5);
            gc.stroke();
        }));

        canvases.put("arcTo, arc, rect, SVG path", draw(gc -> {
            gc.setStroke(Color.web("#37474f"));
            gc.setLineWidth(3);
            gc.beginPath();
            gc.moveTo(8, 12);
            gc.arcTo(60, 12, 60, 60, 24);
            gc.lineTo(60, 88);
            gc.stroke();
            gc.setFill(Color.web("#29b6f6"));
            gc.beginPath();
            gc.moveTo(108, 32);
            gc.arc(108, 32, 26, 22, 30, 300);
            gc.closePath();
            gc.fill();
            gc.setFill(Color.web("#ffca28"));
            gc.setLineWidth(1.5);
            gc.beginPath();
            gc.rect(76, 62, 30, 26);
            gc.appendSVGPath("M 112 88 L 128 60 L 144 88 Z");
            gc.fill();
            gc.stroke();
        }));

        canvases.put("fillText, strokeText", draw(gc -> {
            gc.setFill(Color.web("#0d47a1"));
            gc.setFont(Font.font(georgia, FontWeight.BOLD, 20));
            gc.fillText("fillText", 6, 24);
            gc.setStroke(Color.web("#c62828"));
            gc.setLineWidth(1);
            gc.setFont(Font.font(sans, FontWeight.BOLD, 26));
            gc.strokeText("strokeText", 6, 56);
            gc.setFill(Color.web("#2e7d32"));
            gc.setFont(Font.font(mono, FontPosture.ITALIC, 12));
            gc.fillText(mono + ", maxWidth 90 squeezed", 6, 84, 90);
        }));

        canvases.put("TextAlignment L, C, R", draw(gc -> {
            gc.setStroke(Color.web("#ef9a9a"));
            gc.setLineWidth(1);
            gc.strokeLine(76.5, 0, 76.5, H);
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", 14));
            TextAlignment[] alignments = { TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT };
            for (int i = 0; i < alignments.length; i++) {
                gc.setTextAlign(alignments[i]);
                gc.fillText(alignments[i].name(), 76, 24 + i * 26);
            }
            gc.setTextAlign(TextAlignment.LEFT);
        }));

        canvases.put("VPos TOP .. BOTTOM", draw(gc -> {
            gc.setStroke(Color.web("#ef9a9a"));
            gc.setLineWidth(1);
            gc.strokeLine(0, 48.5, W, 48.5);
            gc.setFill(Color.BLACK);
            // a size no label of the showcase uses : JavaFX caches text layouts by text and font, and a Label laid out
            // with Modena's centered bounds (-fx-bounds-type: LOGICAL_VERTICAL_CENTER) leaves runs whose metrics move
            // the canvas text drawn with the same text and font (PrismTextLayout), depending on what is still cached
            gc.setFont(Font.font("System", 11.5));
            VPos[] positions = { VPos.TOP, VPos.CENTER, VPos.BASELINE, VPos.BOTTOM };
            String[] names = { "Top", "Center", "Base", "Bottom" };
            double[] xs = { 2, 30, 74, 106 };
            for (int i = 0; i < positions.length; i++) {
                gc.setTextBaseline(positions[i]);
                gc.fillText(names[i], xs[i], 48);
            }
            gc.setTextBaseline(VPos.BASELINE);
        }));

        canvases.put("drawImage: plain, scaled, src", draw(gc -> {
            gc.drawImage(icon, 2, 16);
            gc.drawImage(icon, 70, 4, 32, 32);
            gc.drawImage(icon, 108, 4, 40, 20);
            gc.drawImage(photo, 200, 120, 160, 90, 70, 42, 78, 50);
        }));

        canvases.put("Linear, Radial gradients", draw(gc -> {
            gc.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#ff5722")),
                    new Stop(0.5, Color.web("#ffeb3b")), new Stop(1, Color.web("#4caf50"))));
            gc.fillRect(4, 4, 144, 30);
            gc.setFill(new LinearGradient(0, 0, 12, 12, false, CycleMethod.REPEAT, new Stop(0, Color.web("#3f51b5")),
                    new Stop(1, Color.web("#e8eaf6"))));
            gc.fillRect(4, 40, 64, 52);
            gc.setFill(new RadialGradient(30, 0.4, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE, new Stop(0, Color.WHITE),
                    new Stop(1, Color.web("#8e24aa"))));
            gc.fillOval(78, 40, 70, 52);
        }));

        canvases.put("ImagePattern fill, stroke", draw(gc -> {
            gc.setFill(new ImagePattern(tile, 0, 0, 32, 32, false));
            gc.fillRoundRect(4, 4, 90, 88, 20, 20);
            gc.setStroke(new ImagePattern(texture, 0, 0, 24, 24, false));
            gc.setLineWidth(10);
            gc.strokeOval(102, 14, 42, 68);
        }));

        canvases.put("setEffect(DropShadow)", draw(gc -> {
            gc.setEffect(new DropShadow(6, 3, 3, Color.rgb(0, 0, 0, 0.6)));
            gc.setFill(Color.web("#4db6ac"));
            gc.fillRoundRect(10, 10, 56, 40, 10, 10);
            gc.setFill(Color.web("#f06292"));
            gc.fillOval(84, 12, 50, 50);
            gc.setFill(Color.web("#3949ab"));
            gc.setFont(Font.font(sans, FontWeight.BOLD, 18));
            gc.fillText("Shadow", 12, 84);
            gc.setEffect(null);
        }));

        canvases.put("applyEffect(BoxBlur)", draw(gc -> {
            for (int i = 0; i < 8; i++) {
                gc.setFill(i % 2 == 0 ? Color.web("#1e88e5") : Color.web("#fdd835"));
                gc.fillRect(4 + i * 18, 6, 18, 50);
            }
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.BOLD, 16));
            gc.fillText("Blurred", 40, 36);
            gc.applyEffect(new BoxBlur(6, 6, 2));
            gc.setFill(Color.web("#c62828"));
            gc.fillText("Sharp", 50, 84);
        }));

        canvases.put("globalAlpha 1, 0.6, 0.3", draw(gc -> {
            for (int i = 0; i < 10; i++) {
                gc.setFill(i % 2 == 0 ? Color.web("#263238") : Color.web("#eceff1"));
                gc.fillRect(0, i * 9.6, W, 9.6);
            }
            double[] alphas = { 1.0, 0.6, 0.3 };
            for (int i = 0; i < alphas.length; i++) {
                gc.setGlobalAlpha(alphas[i]);
                gc.setFill(Color.web("#ff7043"));
                gc.fillRect(8 + i * 48, 14, 40, 68);
            }
            gc.setGlobalAlpha(1);
        }));

        canvases.put("blend MULTIPLY, SCREEN", draw(gc -> {
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, W / 2, H);
            gc.setFill(Color.BLACK);
            gc.fillRect(W / 2, 0, W / 2, H);
            gc.setGlobalBlendMode(BlendMode.MULTIPLY);
            circles(gc, 38, Color.CYAN, Color.MAGENTA, Color.YELLOW);
            gc.setGlobalBlendMode(BlendMode.SCREEN);
            circles(gc, 114, Color.RED, Color.LIME, Color.BLUE);
            gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        }));

        canvases.put("clip() circle, restore", draw(gc -> {
            gc.save();
            gc.beginPath();
            gc.arc(76, 48, 42, 42, 0, 360);
            gc.closePath();
            gc.clip();
            for (int i = -10; i < 20; i++) {
                gc.setFill(i % 2 == 0 ? Color.web("#8bc34a") : Color.web("#33691e"));
                gc.fillPolygon(new double[] { i * 10, i * 10 + 10, i * 10 + 110, i * 10 + 100 },
                        new double[] { 0, 0, H, H }, 4);
            }
            gc.restore();
            gc.setStroke(Color.web("#1b5e20"));
            gc.setLineWidth(2);
            gc.strokeOval(34, 6, 84, 84);
            gc.setFill(Color.web("#e53935"));
            gc.fillRect(2, 2, 16, 16);
        }));

        canvases.put("transforms, Affine", draw(gc -> {
            Color[] colors = { Color.web("#546e7a"), Color.web("#e53935"), Color.web("#43a047"), Color.web("#1e88e5"),
                    Color.web("#fb8c00") };
            for (int i = 0; i < colors.length; i++) {
                gc.save();
                switch (i) {
                    case 0 -> gc.translate(4, 8);
                    case 1 -> {
                        gc.translate(48, 10);
                        gc.rotate(30);
                    }
                    case 2 -> {
                        gc.translate(80, 6);
                        gc.scale(1.4, 0.8);
                    }
                    case 3 -> gc.transform(new Affine(1, 0.5, 8, 0, 1, 52));
                    default -> gc.setTransform(0.8, -0.3, 0.3, 0.8, 96, 62);
                }
                letterF(gc, colors[i]);
                gc.restore();
            }
        }));

        canvases.put("dashes, caps, joins, miter", draw(gc -> {
            gc.setStroke(Color.web("#3949ab"));
            gc.setLineWidth(3);
            gc.setLineDashes(12, 5);
            gc.strokeLine(6, 8, 146, 8);
            gc.setLineDashes(2, 4, 8, 4);
            gc.setLineDashOffset(3);
            gc.strokeLine(6, 18, 146, 18);
            gc.setLineDashes((double[]) null);
            gc.setLineDashOffset(0);
            gc.setLineWidth(8);
            StrokeLineCap[] caps = { StrokeLineCap.BUTT, StrokeLineCap.ROUND, StrokeLineCap.SQUARE };
            for (int i = 0; i < caps.length; i++) {
                gc.setLineCap(caps[i]);
                gc.setStroke(Color.web("#00897b"));
                gc.strokeLine(12, 32 + i * 12, 60, 32 + i * 12);
            }
            gc.setLineCap(StrokeLineCap.BUTT);
            StrokeLineJoin[] joins = { StrokeLineJoin.MITER, StrokeLineJoin.ROUND, StrokeLineJoin.BEVEL };
            for (int i = 0; i < joins.length; i++) {
                gc.setLineJoin(joins[i]);
                gc.setStroke(Color.web("#d81b60"));
                double x = 76 + i * 25;
                gc.strokePolyline(new double[] { x, x + 9, x + 18 }, new double[] { 60, 32, 60 }, 3);
            }
            gc.setLineWidth(4);
            gc.setLineJoin(StrokeLineJoin.MITER);
            gc.setStroke(Color.web("#6d4c41"));
            gc.setMiterLimit(10);
            gc.strokePolyline(new double[] { 10, 60, 10 }, new double[] { 72, 80, 88 }, 3);
            gc.setMiterLimit(1.5);
            gc.strokePolyline(new double[] { 80, 130, 80 }, new double[] { 72, 80, 88 }, 3);
            gc.setMiterLimit(10);
        }));

        canvases.put("NON_ZERO vs EVEN_ODD", draw(gc -> {
            FillRule[] rules = { FillRule.NON_ZERO, FillRule.EVEN_ODD };
            for (int i = 0; i < rules.length; i++) {
                gc.setFillRule(rules[i]);
                gc.setFill(Color.web("#ffb300"));
                gc.setStroke(Color.web("#e65100"));
                gc.setLineWidth(1.5);
                gc.beginPath();
                double cx = 38 + i * 76;
                for (int k = 0; k < 5; k++) {
                    double angle = Math.toRadians(-90 + k * 144);
                    double x = cx + 34 * Math.cos(angle);
                    double y = 50 + 34 * Math.sin(angle);
                    if (k == 0) {
                        gc.moveTo(x, y);
                    } else {
                        gc.lineTo(x, y);
                    }
                }
                gc.closePath();
                gc.fill();
                gc.stroke();
            }
            gc.setFillRule(FillRule.NON_ZERO);
        }));

        canvases.put("imageSmoothing true, false", draw(gc -> {
            gc.setImageSmoothing(true);
            gc.drawImage(small, 6, 12, 64, 64);
            gc.setImageSmoothing(false);
            // another Image instance : the texture filtering mode of a shared texture is applied when the batch is
            // flushed, so the same image drawn twice in a frame would use a single mode
            gc.drawImage(smallCopy, 80, 12, 64, 64);
            gc.setImageSmoothing(true);
        }));

        canvases.put("getPixelWriter()", draw(gc -> {
            PixelWriter writer = gc.getPixelWriter();
            // 4x4 pixel blocks of the same color : the pixel read back below is inside a block, so that the canvas
            // resampled at a fractional output scale (e.g. 150%) still shows its exact color
            for (int y = 0; y < 80; y++) {
                for (int x = 0; x < 64; x++) {
                    writer.setArgb(6 + x, 8 + y, 0xFF000000 | (x / 4 * 16 << 16) | (y / 4 * 12 << 8) | 0x80);
                }
            }
            int[] block = new int[60 * 60];
            for (int i = 0; i < block.length; i++) {
                int x = i % 60;
                int y = i / 60;
                block[i] = ((x / 6) + (y / 6)) % 2 == 0 ? 0xFFFFFFFF : 0xFF7B1FA2;
            }
            writer.setPixels(82, 18, 60, 60, PixelFormat.getIntArgbInstance(), block, 0, 60);
            gc.setStroke(Color.web("#ffd600"));
            gc.setLineWidth(3);
            gc.strokeLine(6, 88, 146, 8);
        }));

        canvases.put("clearRect", draw(gc -> {
            for (int i = 0; i < 16; i++) {
                gc.setFill(Color.hsb(i * 22.5, 0.7, 0.9));
                gc.fillRect(i * 9.5, 0, 9.5, H);
            }
            gc.clearRect(30, 22, 92, 52);
            gc.setFill(Color.web("#455a64"));
            gc.setFont(Font.font("System", 12));
            gc.fillText("cleared", 56, 52);
        }));

        canvases.put("FontSmoothing GRAY, LCD", draw(gc -> {
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, W, H);
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", 13));
            gc.setFontSmoothingType(FontSmoothingType.GRAY);
            gc.fillText("GRAY smoothing text", 6, 30);
            gc.setFontSmoothingType(FontSmoothingType.LCD);
            gc.fillText("LCD smoothing text", 6, 60);
            gc.setFontSmoothingType(FontSmoothingType.GRAY);
        }));

        canvases.put("Arabic, CJK, emoji text", draw(gc -> {
            gc.setFill(Color.web("#1a237e"));
            gc.setFont(Font.font(arabic, 20));
            gc.fillText("مرحبا بالعالم", 6, 28);
            gc.setFont(Font.font(japanese, 16));
            gc.fillText("日本語のテキスト", 6, 56);
            gc.setFont(Font.font("System", 18));
            gc.fillText("Emoji 😀🚀❤️", 6, 86);
        }));

        canvases.put("save/restore stack", draw(gc -> {
            gc.setFill(Color.web("#90a4ae"));
            gc.fillRect(4, 4, 30, 88);
            gc.save();
            gc.setFill(Color.web("#e53935"));
            gc.setGlobalAlpha(0.5);
            gc.translate(40, 0);
            gc.fillRect(4, 4, 30, 88);
            gc.save();
            gc.setFill(Color.web("#1e88e5"));
            gc.translate(40, 0);
            gc.fillRect(4, 4, 30, 88);
            gc.restore();
            gc.restore();
            gc.fillRect(118, 4, 30, 88);
        }));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        int i = 0;
        for (Map.Entry<String, Canvas> entry : canvases.entrySet()) {
            VBox cell = Ui.tile(entry.getKey(), entry.getValue());
            cell.setPrefWidth(W + 14);
            cell.setMaxWidth(W + 14);
            grid.add(cell, i % 6, i / 6);
            i++;
        }

        // GraphicsContext state checks
        Canvas probe = new Canvas(10, 10);
        GraphicsContext gc = probe.getGraphicsContext2D();
        checks.add(loads.check());
        checks.add(Checks.run("defaults: fill stroke width font align vpos", () -> gc.getFill() + ", "
                + gc.getStroke() + ", " + gc.getLineWidth() + ", " + gc.getFont().getName() + " " + Ui.num(gc.getFont()
                        .getSize()) + ", " + gc.getTextAlign() + ", " + gc.getTextBaseline()));
        checks.add(Checks.expect("save/restore; translate rotate scale",
                "0x000000ff, 1.0, SRC_OVER, IDENTITY / [0.0, -3.0, 10.0; 2.0, 0.0, 5.0]", () -> {
                    gc.save();
                    gc.setFill(Color.RED);
                    gc.setGlobalAlpha(0.3);
                    gc.setGlobalBlendMode(BlendMode.MULTIPLY);
                    gc.translate(5, 5);
                    gc.restore();
                    String restored = gc.getFill() + ", " + gc.getGlobalAlpha() + ", " + gc.getGlobalBlendMode() + ", "
                            + (gc.getTransform().isIdentity() ? "IDENTITY" : gc.getTransform());
                    gc.save();
                    gc.translate(10, 5);
                    gc.rotate(90);
                    gc.scale(2, 3);
                    Affine a = gc.getTransform();
                    gc.restore();
                    return restored + " / [" + Ui.num(a.getMxx()) + ", " + Ui.num(a.getMxy()) + ", " + Ui.num(a.getTx())
                            + "; " + Ui.num(a.getMyx()) + ", " + Ui.num(a.getMyy()) + ", " + Ui.num(a.getTy()) + "]";
                }));
        checks.add(Checks.run("lineDashes, fillRule, imageSmoothing, miterLimit", () -> {
            gc.setLineDashes(4, 2, 1);
            gc.setFillRule(FillRule.EVEN_ODD);
            gc.setImageSmoothing(false);
            gc.setMiterLimit(3);
            return Arrays.toString(gc.getLineDashes()) + ", " + gc.getFillRule() + ", " + gc.isImageSmoothing() + ", "
                    + gc.getMiterLimit();
        }));

        VBox checksHolder = new VBox(Checks.view("GraphicsContext", checks));
        VBox root = new VBox(8, grid, checksHolder);
        root.setPrefWidth(1028);
        root.setMaxWidth(1028);

        // pixels read back from snapshots of the rendered canvases
        CompletionStage<?> ready = Fx.pulses(2).thenRun(() -> {
            List<Check> all = new ArrayList<>(checks);
            WritableImage shapes = canvases.get("shapes, line, polygon").snapshot(null, null);
            WritableImage blend = canvases.get("blend MULTIPLY, SCREEN").snapshot(null, null);
            WritableImage alpha = canvases.get("globalAlpha 1, 0.6, 0.3").snapshot(null, null);
            WritableImage clip = canvases.get("clip() circle, restore").snapshot(null, null);
            WritableImage pixels = canvases.get("getPixelWriter()").snapshot(null, null);
            WritableImage smoothing = canvases.get("imageSmoothing true, false").snapshot(null, null);
            all.add(Checks.expect("rect/roundRect/oval, PixelWriter pixels",
                    "#FF42A5F5 #FF66BB6A #FFFFA726 / #FF000080 #FFFFFFFF #FF7B1FA2",
                    () -> argb(shapes, 28, 23) + " " + argb(shapes, 76, 23) + " " + argb(shapes, 124, 23) + " / "
                            + argb(pixels, 8, 10) + " " + argb(pixels, 85, 21) + " " + argb(pixels, 91, 21)));
            all.add(Checks.expect("MULTIPLY C×M, C×M×Y / SCREEN R+G, RGB", "#FF0000FF, #FF000000, #FFFFFF00, #FFFFFFFF",
                    () -> argb(blend, 33, 24) + ", " + argb(blend, 35, 40) + ", " + argb(blend, 109, 24) + ", "
                            + argb(blend, 111, 40)));
            all.add(Checks.run("alpha 1/.6/.3, clip out/in/after, smoothing", () -> String.join(" ",
                    argb(alpha, 28, 24), argb(alpha, 76, 24), argb(alpha, 124, 24)) + " / " + String.join(" ",
                            argb(clip, 140, 88), argb(clip, 76, 48), argb(clip, 10, 10)) + " / " + String.join(" ",
                                    argb(smoothing, 36, 20), argb(smoothing, 110, 20), argb(smoothing, 20, 42),
                                    argb(smoothing, 94, 42))));
            checksHolder.getChildren().setAll(Checks.view("GraphicsContext state and rendered pixels", all));
        });
        root.getProperties().put(READY, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return (CompletionStage<?>) content.getProperties().get(READY);
    }

    private static Canvas draw(Consumer<GraphicsContext> drawing) {
        Canvas canvas = new Canvas(W, H);
        drawing.accept(canvas.getGraphicsContext2D());
        return canvas;
    }

    private static void circles(GraphicsContext gc, double cx, Color a, Color b, Color c) {
        gc.setFill(a);
        gc.fillOval(cx - 30, 14, 40, 40);
        gc.setFill(b);
        gc.fillOval(cx - 10, 14, 40, 40);
        gc.setFill(c);
        gc.fillOval(cx - 20, 34, 40, 40);
    }

    private static void letterF(GraphicsContext gc, Color color) {
        gc.setFill(color);
        gc.fillRect(0, 0, 6, 30);
        gc.fillRect(0, 0, 20, 6);
        gc.fillRect(0, 12, 14, 5);
    }

    private static String argb(WritableImage image, int x, int y) {
        return Ui.argb(image.getPixelReader().getArgb(x, y));
    }
}
