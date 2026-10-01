package io.quarkiverse.fx.showcase.pages.swing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

/**
 * Swing interoperability : a SwingNode hosting Swing components created on the EDT, SwingFXUtils conversions between
 * JavaFX images and AWT BufferedImages.
 */
@Singleton
public class SwingInteropPage implements FeaturePage {

    private static final String STATE = SwingInteropPage.class.getName();

    /** Test pixels (ARGB) : opaque, semi-transparent and transparent. */
    private static final int[] PIXELS = {
            0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF,
            0x80FF0000, 0x40336699, 0x00000000, 0xFF123456 };

    @Override
    public String id() {
        return "swing-interop";
    }

    @Override
    public String title() {
        return "SwingNode & SwingFXUtils";
    }

    @Override
    public String category() {
        return Categories.SWING;
    }

    @Override
    public int order() {
        return 10;
    }

    private static final class State {
        final CompletableFuture<List<Check>> swing = new CompletableFuture<>();
        final CompletableFuture<Void> painted = new CompletableFuture<>();
        final VBox swingChecks = new VBox();
        final VBox fxChecks = new VBox();
        final List<Check> conversions = new ArrayList<>();
        SwingNode swingNode;
        CompletionStage<?> ready;
    }

    @Override
    public Node build() {
        State state = new State();

        StackPane swingHolder = new StackPane();
        swingHolder.setAlignment(Pos.TOP_LEFT);
        swingHolder.setPrefSize(520, 390);
        swingHolder.setMinSize(520, 390);
        swingHolder.setMaxSize(520, 390);
        swingHolder.setStyle("-fx-border-color: #9fb3c8; -fx-border-width: 1;");
        try {
            SwingNode swingNode = new SwingNode();
            state.swingNode = swingNode;
            swingHolder.getChildren().add(swingNode);
            SwingUtilities.invokeLater(() -> createSwingContent(state, swingNode));
        } catch (Throwable t) {
            Label error = new Label("SwingNode failed: " + Checks.describe(t));
            error.getStyleClass().add("check-fail");
            error.setWrapText(true);
            swingHolder.getChildren().add(error);
            state.swing.complete(List.of(Check.fail("new SwingNode()", Checks.describe(t))));
            state.painted.completeExceptionally(t);
        }
        VBox left = new VBox(4, caption("SwingNode hosting a JPanel created on the EDT"), swingHolder);

        VBox right = new VBox(8, caption("SwingFXUtils conversions"));
        right.setPrefWidth(492);
        right.getChildren().add(conversions(state));

        HBox top = new HBox(16, left, right);

        state.swingChecks.getChildren().add(new Label("Waiting for the Swing content..."));
        state.swingChecks.setPrefWidth(506);
        state.fxChecks.setPrefWidth(506);
        HBox.setHgrow(state.swingChecks, Priority.ALWAYS);
        HBox.setHgrow(state.fxChecks, Priority.ALWAYS);
        state.fxChecks.getChildren().setAll(Checks.view("SwingFXUtils", state.conversions));
        HBox checks = new HBox(16, state.swingChecks, state.fxChecks);

        VBox root = new VBox(12, top, checks);
        root.getProperties().put(STATE, state);

        CompletionStage<String> painted = Fx.timeout(state.painted, 20_000, "Swing content painted")
                .handle((v, error) -> error == null ? null : describe(error));
        state.ready = painted
                .thenCombine(state.swing, (paintError, swingChecks) -> {
                    List<Check> all = new ArrayList<>(swingChecks);
                    all.addFirst(paintError == null ? Check.pass("Swing content painted", true)
                            : Check.fail("Swing content painted", paintError));
                    return all;
                })
                .thenAcceptAsync(all -> state.swingChecks.getChildren().setAll(Checks.view("Swing", all)), Fx.FX_THREAD)
                // lets the painted Swing content reach the SwingNode
                .thenCompose(v -> Fx.pulses(10))
                .thenCompose(v -> Fx.delay(600))
                .thenCompose(v -> stable(swingHolder, 10_000))
                .thenCompose(v -> Fx.pulses(5));
        // the Swing checks never arrive when the EDT task could not run
        PauseTransition guard = new PauseTransition(Duration.seconds(20));
        guard.setOnFinished(e -> state.swing.complete(List.of(Check.fail("Swing content created", "timeout"))));
        guard.play();
        state.swing.whenComplete((v, e) -> Platform.runLater(guard::stop));
        return root;
    }

    /**
     * Completes once snapshots of {@code node}, taken every 4 pulses, were identical 3 times in a row (the Swing
     * content is painted asynchronously, on the EDT, then copied to the SwingNode). A timeout only ends the wait.
     */
    private static CompletionStage<Void> stable(Node node, double timeoutMillis) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        new AnimationTimer() {
            private long start = -1;
            private int pulses;
            private int[] previous;
            private int identical;

            @Override
            public void handle(long now) {
                if (start < 0) {
                    start = now;
                }
                if (++pulses % 4 != 0) {
                    return;
                }
                try {
                    WritableImage image = node.snapshot(null, null);
                    int width = (int) image.getWidth();
                    int height = (int) image.getHeight();
                    int[] pixels = new int[width * height];
                    image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0,
                            width);
                    identical = java.util.Arrays.equals(pixels, previous) ? identical + 1 : 0;
                    previous = pixels;
                } catch (Throwable t) {
                    // rendering failures show in the page snapshot : the wait ends
                    identical = Integer.MAX_VALUE;
                }
                if (identical >= 2 || now - start > timeoutMillis * 1_000_000) {
                    stop();
                    done.complete(null);
                }
            }
        }.start();
        return done;
    }

    private static Label caption(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #52606d; -fx-font-weight: bold;");
        return label;
    }

    private static String describe(Throwable error) {
        while (error instanceof java.util.concurrent.CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        return Checks.describe(error);
    }

    // --- Swing (EDT) --------------------------------------------------------------------------------------------

    /**
     * A panel painting a gradient background, reporting its first paint.
     */
    private static final class ShowcasePanel extends JPanel {
        private final Runnable onFirstPaint;
        private boolean painted;

        ShowcasePanel(Runnable onFirstPaint) {
            super(new BorderLayout(8, 8));
            this.onFirstPaint = onFirstPaint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, new Color(0xE3F2FD), 0, getHeight(), new Color(0xFFF8E1)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (!painted && getWidth() > 0 && getHeight() > 0) {
                painted = true;
                onFirstPaint.run();
            }
        }
    }

    private static void createSwingContent(State state, SwingNode swingNode) {
        List<Check> checks = new ArrayList<>();
        try {
            checks.add(Checks.expect("created on the EDT", true, SwingUtilities::isEventDispatchThread));
            checks.add(Checks.expect("GraphicsEnvironment.isHeadless()", false, GraphicsEnvironment::isHeadless));
            checks.add(Checks.run("Toolkit", () -> Toolkit.getDefaultToolkit().getClass().getName()));
            checks.add(Checks.run("look and feel", () -> UIManager.getLookAndFeel().getName()));

            ShowcasePanel panel = new ShowcasePanel(() -> Platform.runLater(() -> state.painted.complete(null)));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel title = new JLabel("Swing components in a SwingNode");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
            try (InputStream in = Fx.resource("/showcase/images/icon-16.png").openStream()) {
                BufferedImage icon = ImageIO.read(in);
                title.setIcon(new ImageIcon(icon));
                checks.add(Check.pass("ImageIO.read(icon-16.png) as ImageIcon", icon.getWidth() + "x" + icon.getHeight()));
            } catch (Throwable t) {
                checks.add(Check.fail("ImageIO.read(icon-16.png) as ImageIcon", Checks.describe(t)));
            }
            panel.add(title, BorderLayout.NORTH);

            JPanel center = new JPanel(new GridBagLayout());
            center.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            buttons.setOpaque(false);
            JButton button = new JButton("JButton");
            JCheckBox checkBox = new JCheckBox("JCheckBox", true);
            checkBox.setOpaque(false);
            JRadioButton radioA = new JRadioButton("Radio A", true);
            JRadioButton radioB = new JRadioButton("Radio B");
            radioA.setOpaque(false);
            radioB.setOpaque(false);
            ButtonGroup group = new ButtonGroup();
            group.add(radioA);
            group.add(radioB);
            for (javax.swing.JComponent component : List.of(button, checkBox, radioA, radioB)) {
                component.setFocusable(false);
                buttons.add(component);
            }
            center.add(buttons, c);

            c.gridy++;
            JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            fields.setOpaque(false);
            JTextField text = new JTextField("JTextField", 14);
            text.setFocusable(false);
            JComboBox<String> combo = new JComboBox<>(new String[] { "JComboBox", "Second", "Third" });
            combo.setFocusable(false);
            fields.add(text);
            fields.add(combo);
            center.add(fields, c);

            c.gridy++;
            // HTML text : javax.swing.text.html parser, CSS parser and the default.css resource of java.desktop
            try {
                JLabel html = new JLabel("<html>HTML in a <b>JLabel</b>: <i>italic</i>, <u>underlined</u>, "
                        + "<font color='#c62828'>red</font>, <span style='color: #1565c0; font-weight: bold'>CSS</span>"
                        + "</html>");
                Dimension size = html.getPreferredSize();
                Object view = html.getClientProperty(BasicHTML.propertyKey);
                if (view == null) {
                    throw new IllegalStateException("no HTML view");
                }
                center.add(html, c);
                boolean sized = size.width > 0 && size.height > 0;
                checks.add(Check.of("JLabel HTML view", sized, sized ? "rendered" : "empty preferred size"));
            } catch (Throwable t) {
                checks.add(Check.fail("JLabel HTML view", Checks.describe(t)));
                JLabel failed = new JLabel("JLabel HTML view failed: " + t.getClass().getSimpleName());
                failed.setForeground(new Color(0xC62828));
                center.add(failed, c);
            }
            checks.add(Checks.expect("HTMLEditorKit default.css rule h1", "font-size=x-large, font-weight=bold", () -> {
                AttributeSet h1 = new HTMLEditorKit().getStyleSheet().getRule("h1");
                return "font-size=" + h1.getAttribute(CSS.Attribute.FONT_SIZE) + ", font-weight="
                        + h1.getAttribute(CSS.Attribute.FONT_WEIGHT);
            }));

            c.gridy++;
            JSlider slider = new JSlider(0, 100, 60);
            slider.setMajorTickSpacing(25);
            slider.setMinorTickSpacing(5);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.setOpaque(false);
            slider.setFocusable(false);
            center.add(slider, c);

            c.gridy++;
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            DefaultTableModel model = new DefaultTableModel(new Object[][] {
                    { "javafx.swing", "SwingNode", 3 },
                    { "javafx.swing", "SwingFXUtils", 2 },
                    { "java.desktop", "JTable", 7 },
                    { "java.desktop", "JSlider", 5 },
                    { "jdk.swing.interop", "LightweightFrameWrapper", 11 },
                    { "javafx.graphics", "Prism", 13 } },
                    new Object[] { "Module", "Class", "Count" });
            JTable table = new JTable(model);
            table.setFocusable(false);
            table.setRowHeight(20);
            table.setSelectionBackground(new Color(0x1565C0));
            table.setSelectionForeground(Color.WHITE);
            table.setRowSelectionInterval(2, 2);
            DefaultTableCellRenderer header = new DefaultTableCellRenderer();
            header.setBackground(new Color(0x334E68));
            header.setForeground(Color.WHITE);
            header.setFont(header.getFont().deriveFont(Font.BOLD));
            header.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            table.getTableHeader().setDefaultRenderer(header);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(480, 120));
            center.add(scroll, c);
            panel.add(center, BorderLayout.CENTER);

            checks.add(Checks.expect("JTable model", "6x3, (1, 1) = SwingFXUtils",
                    () -> model.getRowCount() + "x" + model.getColumnCount() + ", (1, 1) = " + model.getValueAt(1, 1)));
            checks.add(Checks.expect("JSlider value / JCheckBox selected", "60 / true",
                    () -> slider.getValue() + " / " + checkBox.isSelected()));
            checks.add(Checks.run("ImageIO reader formats", () -> {
                TreeSet<String> formats = new TreeSet<>();
                for (String name : ImageIO.getReaderFormatNames()) {
                    formats.add(name.toLowerCase(Locale.ROOT));
                }
                return String.join(" ", formats);
            }));

            swingNode.setContent(panel);
            checks.add(Checks.expect("SwingNode.getContent()", "ShowcasePanel",
                    () -> swingNode.getContent().getClass().getSimpleName()));
        } catch (Throwable t) {
            checks.add(Check.fail("Swing content created", Checks.describe(t)));
            Platform.runLater(() -> state.painted.completeExceptionally(t));
        }
        state.swing.complete(checks);
    }

    // --- SwingFXUtils (FX thread) -------------------------------------------------------------------------------

    private static GridPane conversions(State state) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        List<Check> checks = state.conversions;

        // Java2D drawing converted to a JavaFX image
        try {
            BufferedImage java2d = java2dImage();
            WritableImage fx = SwingFXUtils.toFXImage(java2d, null);
            grid.add(new VBox(4, caption("Java2D BufferedImage → toFXImage"), new ImageView(fx)), 0, 0);
            checks.add(Checks.run("Java2D → toFXImage", () -> (int) fx.getWidth() + "x" + (int) fx.getHeight()
                    + ", (10,10) " + hex(fx.getPixelReader().getArgb(10, 10))));
        } catch (Throwable t) {
            checks.add(Check.fail("Java2D → toFXImage", Checks.describe(t)));
            grid.add(error(t), 0, 0);
        }

        // JavaFX canvas snapshot converted to AWT, filtered by Java2D (color conversion), and back
        try {
            Canvas canvas = new Canvas(230, 150);
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, javafx.scene.paint.Color.web("#ff7043")),
                    new Stop(1, javafx.scene.paint.Color.web("#5c6bc0"))));
            g.fillRoundRect(0, 0, 230, 150, 30, 30);
            g.setFill(javafx.scene.paint.Color.web("#ffee58"));
            g.fillOval(20, 20, 80, 80);
            g.setStroke(javafx.scene.paint.Color.WHITE);
            g.setLineWidth(6);
            g.strokeLine(120, 30, 210, 120);
            g.setFill(javafx.scene.paint.Color.web("#26a69a"));
            g.fillRect(130, 90, 70, 40);
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(javafx.scene.paint.Color.TRANSPARENT);
            WritableImage snapshot = canvas.snapshot(parameters, null);
            grid.add(new VBox(4, caption("JavaFX Canvas snapshot"), new ImageView(snapshot)), 1, 0);

            BufferedImage awt = SwingFXUtils.fromFXImage(snapshot, null);
            BufferedImage gray = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(awt, null);
            WritableImage back = SwingFXUtils.toFXImage(gray, null);
            grid.add(new VBox(4, caption("→ fromFXImage → CS_GRAY → toFXImage"), new ImageView(back)), 0, 1);
            checks.add(Checks.run("fromFXImage(canvas snapshot)", () -> imageType(awt.getType())
                    + ", (60,60) " + hex(awt.getRGB(60, 60))));
            checks.add(Checks.run("ColorConvertOp(CS_GRAY) → toFXImage", () -> imageType(gray.getType())
                    + ", (60,60) " + hex(back.getPixelReader().getArgb(60, 60))));
        } catch (Throwable t) {
            checks.add(Check.fail("FX snapshot → fromFXImage → gray → toFXImage", Checks.describe(t)));
            grid.add(error(t), 1, 0);
        }

        // pixel round trips
        try {
            WritableImage source = new WritableImage(4, 2);
            PixelWriter writer = source.getPixelWriter();
            for (int i = 0; i < PIXELS.length; i++) {
                writer.setArgb(i % 4, i / 4, PIXELS[i]);
            }
            BufferedImage awt = SwingFXUtils.fromFXImage(source, null);
            WritableImage back = SwingFXUtils.toFXImage(awt, null);

            ImageView sourceView = new ImageView(enlarge(source, 36));
            ImageView backView = new ImageView(enlarge(back, 36));
            StackPane sourceFrame = new StackPane(sourceView);
            sourceFrame.setStyle("-fx-background-color: #cfd8dc;");
            sourceFrame.setMaxWidth(Region.USE_PREF_SIZE);
            StackPane backFrame = new StackPane(backView);
            backFrame.setStyle("-fx-background-color: #cfd8dc;");
            backFrame.setMaxWidth(Region.USE_PREF_SIZE);
            grid.add(new VBox(4, caption("4x2 test pixels (WritableImage)"), sourceFrame,
                    caption("→ fromFXImage → toFXImage"), backFrame), 1, 1);

            checks.add(Checks.run("fromFXImage(WritableImage) type", () -> imageType(awt.getType())));
            checks.add(Checks.expect("fromFXImage opaque pixels", "ffff0000 ff0000ff ff123456",
                    () -> String.join(" ", hex(awt.getRGB(0, 0)), hex(awt.getRGB(2, 0)), hex(awt.getRGB(3, 1)))));
            checks.add(Checks.run("fromFXImage translucent pixels", () -> String.join(" ", hex(awt.getRGB(0, 1)),
                    hex(awt.getRGB(1, 1)), hex(awt.getRGB(2, 1)))));
            checks.add(Checks.run("round trip FX → AWT → FX", () -> {
                PixelReader reader = back.getPixelReader();
                List<String> differences = new ArrayList<>();
                for (int i = 0; i < PIXELS.length; i++) {
                    int argb = reader.getArgb(i % 4, i / 4);
                    if (argb != PIXELS[i]) {
                        differences.add(hex(PIXELS[i]) + "→" + hex(argb));
                    }
                }
                return differences.isEmpty() ? "identical" : "rounded: " + String.join(" ", differences);
            }));
        } catch (Throwable t) {
            checks.add(Check.fail("pixel round trip", Checks.describe(t)));
            grid.add(error(t), 1, 1);
        }

        // AWT image types converted to JavaFX
        checks.add(Checks.expect("toFXImage(TYPE_INT_RGB)", "ff112233 ff445566 ff778899", () -> {
            BufferedImage rgb = new BufferedImage(3, 1, BufferedImage.TYPE_INT_RGB);
            rgb.setRGB(0, 0, 0x112233);
            rgb.setRGB(1, 0, 0x445566);
            rgb.setRGB(2, 0, 0x778899);
            Image fx = SwingFXUtils.toFXImage(rgb, null);
            PixelReader reader = fx.getPixelReader();
            return String.join(" ", hex(reader.getArgb(0, 0)), hex(reader.getArgb(1, 0)), hex(reader.getArgb(2, 0)));
        }));
        checks.add(Checks.expect("toFXImage(TYPE_BYTE_GRAY)", "ff000000 ff808080 ffffffff", () -> {
            BufferedImage grayImage = new BufferedImage(3, 1, BufferedImage.TYPE_BYTE_GRAY);
            grayImage.getRaster().setPixels(0, 0, 3, 1, new int[] { 0, 128, 255 });
            Image fx = SwingFXUtils.toFXImage(grayImage, null);
            PixelReader reader = fx.getPixelReader();
            return String.join(" ", hex(reader.getArgb(0, 0)), hex(reader.getArgb(1, 0)), hex(reader.getArgb(2, 0)));
        }));
        return grid;
    }

    /**
     * Nearest neighbor enlargement, to show individual pixels.
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

    private static Label error(Throwable t) {
        Label label = new Label(Checks.describe(t));
        label.getStyleClass().add("check-fail");
        label.setWrapText(true);
        label.setMaxWidth(230);
        return label;
    }

    private static BufferedImage java2dImage() {
        BufferedImage image = new BufferedImage(230, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(0x1E88E5), 230, 150, new Color(0x8E24AA)));
            g.fill(new RoundRectangle2D.Double(0, 0, 230, 150, 30, 30));
            g.setPaint(new Color(255, 255, 255, 170));
            g.fill(new Ellipse2D.Double(140, 20, 70, 70));
            g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0xFFD54F));
            g.drawLine(20, 120, 110, 40);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            g.drawString("Java2D", 20, 40);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static String hex(int argb) {
        return String.format(Locale.ROOT, "%08x", argb);
    }

    private static String imageType(int type) {
        return switch (type) {
            case BufferedImage.TYPE_INT_RGB -> "TYPE_INT_RGB";
            case BufferedImage.TYPE_INT_ARGB -> "TYPE_INT_ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE -> "TYPE_INT_ARGB_PRE";
            case BufferedImage.TYPE_BYTE_GRAY -> "TYPE_BYTE_GRAY";
            case BufferedImage.TYPE_CUSTOM -> "TYPE_CUSTOM";
            default -> "type " + type;
        };
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ((State) content.getProperties().get(STATE)).ready;
    }

    @Override
    public void dispose(Node content) {
        State state = (State) content.getProperties().get(STATE);
        if (state != null && state.swingNode != null) {
            SwingNode swingNode = state.swingNode;
            try {
                SwingUtilities.invokeLater(() -> swingNode.setContent(null));
            } catch (Throwable t) {
                // AWT not available : nothing to release, the failure is reported by the page checks
            }
        }
    }
}
