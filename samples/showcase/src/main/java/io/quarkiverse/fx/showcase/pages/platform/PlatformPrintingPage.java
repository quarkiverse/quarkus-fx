package io.quarkiverse.fx.showcase.pages.platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.geometry.Pos;
import javafx.print.JobSettings;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterAttributes;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

/**
 * javafx.print : printers, their attributes, page layouts and a printer job that is created, inspected and cancelled
 * (nothing is ever printed). Failures are reported as failed checks.
 */
@Singleton
public class PlatformPrintingPage implements FeaturePage {

    @Override
    public String id() {
        return "platform-printing";
    }

    @Override
    public String title() {
        return "Printers, Page Layouts & PrinterJob";
    }

    @Override
    public String category() {
        return Categories.PLATFORM;
    }

    @Override
    public int order() {
        return 40;
    }

    /** Runs {@code action}, recording its outcome ; returns null on failure. */
    private static <T> T attempt(List<Check> checks, String name, Callable<T> action, java.util.function.Function<T, Object> describe) {
        try {
            T value = action.call();
            checks.add(Checks.run(name, () -> describe.apply(value)));
            return value;
        } catch (Throwable t) {
            checks.add(Check.fail(name, Checks.describe(t)));
            return null;
        }
    }

    @Override
    public Node build() {
        double half = PlatformUi.HALF_WIDTH;
        List<Check> checks = new ArrayList<>();

        // Paper constants do not need a printer
        checks.add(Checks.expect("Paper.A4 / NA_LETTER (points)", "A4 595.3x841.9 / Letter 612.0x792.0",
                () -> Paper.A4.getName() + " " + size(Paper.A4) + " / " + Paper.NA_LETTER.getName() + " "
                        + size(Paper.NA_LETTER)));

        Collection<Printer> printers = attempt(checks, "Printer.getAllPrinters()", Printer::getAllPrinters,
                all -> all.size() + ": " + all.stream().map(Printer::getName).sorted().collect(Collectors.joining(", ")));
        Printer printer = attempt(checks, "Printer.getDefaultPrinter()", Printer::getDefaultPrinter,
                p -> p == null ? "none" : p.getName());

        PageLayout defaultLayout = null;
        PageLayout landscape = null;
        PrinterAttributes attributes = null;
        if (printer != null) {
            attributes = attempt(checks, "default paper / orientation", printer::getPrinterAttributes,
                    a -> a.getDefaultPaper().getName() + " " + size(a.getDefaultPaper()) + " / "
                            + a.getDefaultPageOrientation());
            if (attributes != null) {
                PrinterAttributes a = attributes;
                checks.add(Checks.run("supported papers / orientations", () -> a.getSupportedPapers().size()
                        + " papers / " + a.getSupportedPageOrientations().stream().map(Enum::name).sorted()
                                .collect(Collectors.joining(","))));
                checks.add(Checks.run("colors / collations / sides", () -> sorted(a.getSupportedPrintColors())
                        + " / " + sorted(a.getSupportedCollations()) + " / "
                        + sorted(a.getSupportedPrintSides())));
                checks.add(Checks.run("copies (default, max) / page ranges", () -> a.getDefaultCopies() + ", "
                        + a.getMaxCopies() + " / " + a.supportsPageRanges()));
                checks.add(Checks.run("default resolution / quality", () -> a.getDefaultPrintResolution() + " / "
                        + a.getDefaultPrintQuality()));
            }
            defaultLayout = attempt(checks, "getDefaultPageLayout()", printer::getDefaultPageLayout,
                    PlatformPrintingPage::describe);
            landscape = attempt(checks, "createPageLayout(A4, LANDSCAPE, HARDWARE_MINIMUM)",
                    () -> printer.createPageLayout(Paper.A4, PageOrientation.LANDSCAPE, Printer.MarginType.HARDWARE_MINIMUM),
                    PlatformPrintingPage::describe);
            attempt(checks, "createPageLayout(NA_LETTER, PORTRAIT, 54 pt margins)",
                    () -> printer.createPageLayout(Paper.NA_LETTER, PageOrientation.PORTRAIT, 54, 54, 54, 54),
                    PlatformPrintingPage::describe);
        }

        // A printer job : created, inspected, cancelled, never printed
        List<String> jobLines = new ArrayList<>();
        PrinterJob job = attempt(checks, "PrinterJob.createPrinterJob()", PrinterJob::createPrinterJob,
                j -> j == null ? "null (no printer)" : j.getPrinter().getName() + ", " + j.getJobStatus());
        if (job != null) {
            JobSettings settings = job.getJobSettings();
            checks.add(Checks.run("JobSettings name / copies / collation", () -> settings.getJobName() + " / "
                    + settings.getCopies() + " / " + settings.getCollation()));
            checks.add(Checks.run("JobSettings color / quality / sides", () -> settings.getPrintColor() + " / "
                    + settings.getPrintQuality() + " / " + settings.getPrintSides()));
            checks.add(Checks.run("JobSettings page layout", () -> describe(settings.getPageLayout())));
            checks.add(Checks.run("JobSettings ranges / paper source", () -> (settings.getPageRanges() == null ? "all pages"
                    : settings.getPageRanges().length + " range(s)") + " / " + settings.getPaperSource()));
            settings.setJobName("Quarkus FX showcase (never printed)");
            settings.setCopies(2);
            jobLines.add("job name   " + settings.getJobName());
            jobLines.add("copies     " + settings.getCopies() + ", " + settings.getCollation() + ", " + settings.getPrintColor());
            jobLines.add("layout     " + describe(settings.getPageLayout()));
            checks.add(Checks.expect("cancelJob() / endJob() / status", "CANCELED / false",
                    () -> {
                        job.cancelJob();
                        boolean ended = job.endJob();
                        return job.getJobStatus() + " / " + ended;
                    }));
            jobLines.add("status     " + job.getJobStatus() + " (cancelled, nothing printed)");
        }

        // Visuals
        HBox previews = new HBox(16,
                preview("Default page layout", defaultLayout),
                preview("A4 landscape, hardware minimum", landscape));
        previews.setAlignment(Pos.TOP_LEFT);
        VBox printerLines = new VBox(2);
        if (printers != null) {
            printers.stream().map(Printer::getName).sorted().forEach(name -> printerLines.getChildren()
                    .add(PlatformUi.line((printer != null && name.equals(printer.getName()) ? "★ " : "  ") + name)));
        }
        if (printerLines.getChildren().isEmpty()) {
            printerLines.getChildren().add(PlatformUi.line("no printer"));
        }
        VBox jobBox = new VBox(2);
        jobLines.forEach(line -> jobBox.getChildren().add(PlatformUi.line(line)));
        if (jobLines.isEmpty()) {
            jobBox.getChildren().add(PlatformUi.line("no printer job"));
        }

        GridPane papers = new GridPane(14, 1);
        if (attributes != null) {
            List<Paper> supported = attributes.getSupportedPapers().stream()
                    .sorted(java.util.Comparator.comparing(Paper::getName)).toList();
            int rows = (supported.size() + 2) / 3;
            for (int i = 0; i < supported.size(); i++) {
                Paper paper = supported.get(i);
                Label label = new Label(paper.getName() + "  " + (int) Math.round(paper.getWidth()) + "x"
                        + (int) Math.round(paper.getHeight()));
                label.getStyleClass().add("swatch-label");
                label.setMaxWidth(150);
                papers.add(label, i / rows, i % rows);
            }
        } else {
            papers.add(PlatformUi.line("no printer attributes"), 0, 0);
        }

        VBox left = new VBox(10,
                PlatformUi.demo("Page layouts : paper, printable area and a node scaled to fit it", previews),
                PlatformUi.demo("Printer.getAllPrinters() (★ default)", printerLines),
                PlatformUi.demo("PrinterJob.createPrinterJob() : settings, then cancelJob()", jobBox),
                PlatformUi.demo("Default printer : supported papers (points)", papers));
        PlatformUi.width(left, half);
        VBox right = PlatformUi.checks("Printing checks", checks, 200, half);
        return PlatformUi.page(0, new HBox(12, left, right));
    }

    private static String sorted(Collection<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    private static String size(Paper paper) {
        return PlatformUi.round(paper.getWidth()) + "x" + PlatformUi.round(paper.getHeight());
    }

    private static String describe(PageLayout layout) {
        if (layout == null) {
            return "null";
        }
        return layout.getPaper().getName() + " " + layout.getPageOrientation() + ", printable "
                + PlatformUi.round(layout.getPrintableWidth()) + "x" + PlatformUi.round(layout.getPrintableHeight())
                + ", margins " + PlatformUi.round(layout.getLeftMargin()) + "/" + PlatformUi.round(layout.getRightMargin())
                + "/" + PlatformUi.round(layout.getTopMargin()) + "/" + PlatformUi.round(layout.getBottomMargin());
    }

    /**
     * A scaled drawing of a page layout : the paper, the printable area and a sample document scaled to fit it (the
     * usual code before {@code PrinterJob.printPage}).
     */
    private static VBox preview(String caption, PageLayout layout) {
        StackPane holder = new StackPane();
        holder.setPrefSize(220, 170);
        holder.setAlignment(Pos.CENTER);
        if (layout == null) {
            holder.getChildren().add(new Label("no layout"));
        } else {
            boolean landscape = layout.getPageOrientation() == PageOrientation.LANDSCAPE
                    || layout.getPageOrientation() == PageOrientation.REVERSE_LANDSCAPE;
            double paperWidth = landscape ? layout.getPaper().getHeight() : layout.getPaper().getWidth();
            double paperHeight = landscape ? layout.getPaper().getWidth() : layout.getPaper().getHeight();
            double scale = Math.min(210 / paperWidth, 160 / paperHeight);

            // whole pixels : a tiny variation of the reported margins must not move an antialiased edge
            Rectangle paper = new Rectangle(Math.round(paperWidth * scale), Math.round(paperHeight * scale), Color.WHITE);
            paper.setStroke(Color.web("#90a4ae"));
            paper.setStrokeType(StrokeType.INSIDE);
            Rectangle printable = new Rectangle(Math.round(layout.getLeftMargin() * scale),
                    Math.round(layout.getTopMargin() * scale), Math.round(layout.getPrintableWidth() * scale),
                    Math.round(layout.getPrintableHeight() * scale));
            printable.setFill(Color.web("#e3f2fd"));
            printable.setStroke(Color.web("#1e88e5"));
            // a solid stroke inside the pixel grid : a dashed stroke is rendered through a cached mask texture whose
            // right edge varied by one level between runs
            printable.setStrokeType(StrokeType.INSIDE);

            // a "document" designed at 400x300 points, scaled uniformly into the printable area
            Node document = sampleDocument();
            double fit = Math.min(layout.getPrintableWidth() / 400, layout.getPrintableHeight() / 300);
            document.getTransforms().setAll(new Scale(fit * scale, fit * scale));
            document.setLayoutX(Math.round(layout.getLeftMargin() * scale));
            document.setLayoutY(Math.round(layout.getTopMargin() * scale));

            Group page = new Group(paper, printable, document);
            page.setAutoSizeChildren(false);
            // positioned at whole pixels, not centered at a fractional offset
            javafx.scene.layout.Pane pane = new javafx.scene.layout.Pane(page);
            page.setLayoutX(Math.round((220 - paper.getWidth()) / 2));
            page.setLayoutY(Math.round((170 - paper.getHeight()) / 2));
            pane.setPrefSize(220, 170);
            holder.getChildren().add(pane);
        }
        Label label = new Label(caption);
        label.getStyleClass().add("swatch-label");
        VBox box = new VBox(3, holder, label);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static Node sampleDocument() {
        Text title = new Text(12, 40, "Invoice #2024-042");
        title.setFont(Font.font("System", FontWeight.BOLD, 30));
        title.setFill(Color.web("#263238"));
        Line rule = new Line(12, 56, 388, 56);
        rule.setStroke(Color.web("#1e88e5"));
        rule.setStrokeWidth(3);
        Group lines = new Group();
        for (int i = 0; i < 6; i++) {
            Rectangle bar = new Rectangle(12, 80 + i * 30, 240 - i * 25, 14);
            bar.setFill(Color.web("#b0bec5"));
            Rectangle amount = new Rectangle(320, 80 + i * 30, 68, 14);
            amount.setFill(Color.web("#78909c"));
            lines.getChildren().addAll(bar, amount);
        }
        Circle stamp = new Circle(330, 250, 34, Color.TRANSPARENT);
        stamp.setStroke(Color.web("#c62828"));
        stamp.setStrokeWidth(4);
        Text paid = new Text(304, 258, "PAID");
        paid.setFont(Font.font("System", FontWeight.BOLD, 20));
        paid.setFill(Color.web("#c62828"));
        Rectangle frame = new Rectangle(400, 300, Color.TRANSPARENT);
        return new Group(frame, title, rule, lines, stamp, paid);
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return PlatformUi.ready(content);
    }
}
