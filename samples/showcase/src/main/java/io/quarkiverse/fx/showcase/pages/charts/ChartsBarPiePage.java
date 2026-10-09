package io.quarkiverse.fx.showcase.pages.charts;

import static io.quarkiverse.fx.showcase.pages.charts.ChartSupport.backgroundFill;
import static io.quarkiverse.fx.showcase.pages.charts.ChartSupport.cell;
import static io.quarkiverse.fx.showcase.pages.charts.ChartSupport.num;
import static io.quarkiverse.fx.showcase.pages.charts.ChartSupport.range;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.collections.FXCollections;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.RadialGradient;
import javafx.scene.text.Text;

/**
 * Bar charts (vertical, horizontal, stacked) and pie charts, styled from a stylesheet.
 */
@Singleton
public class ChartsBarPiePage implements FeaturePage {

    private static final String[] QUARTERS = { "Q1", "Q2", "Q3" };

    @Override
    public String id() {
        return "charts-bar-pie";
    }

    @Override
    public String title() {
        return "Bar & pie charts";
    }

    @Override
    public String category() {
        return Categories.CHARTS;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        BarChart<String, Number> barV = barVertical();
        BarChart<Number, String> barH = barHorizontal();
        StackedBarChart<String, Number> stackedV = stackedVertical();
        StackedBarChart<Number, String> stackedH = stackedHorizontal();
        PieChart pieCw = pieClockwise();
        PieChart pieCcw = pieCounterClockwise();

        HBox top = new HBox(9,
                cell(barV, "BarChart · negative values · CSS bar/category gap", 250, 350),
                cell(barH, "BarChart horizontal · rounded bars", 250, 350),
                cell(stackedV, "StackedBarChart · CSS category gap", 250, 350),
                cell(stackedH, "StackedBarChart horizontal · pattern fill", 250, 350));

        VBox checksHolder = new VBox();
        checksHolder.setPrefSize(508, 368);
        checksHolder.setMinSize(508, 368);
        checksHolder.setMaxSize(508, 368);
        HBox bottom = new HBox(10,
                cell(pieCw, "PieChart · clockwise, labels, legend", 250, 368),
                cell(pieCcw, "PieChart · CSS start angle 90, counter-clockwise", 250, 368),
                checksHolder);

        VBox root = new VBox(10, top, bottom);
        root.getStyleClass().add("showcase-charts");
        root.getStylesheets().add(Fx.resourceUrl(ChartSupport.CSS));
        ChartSupport.checksAfterLayout(root, checksHolder, "Chart checks (after CSS and layout)",
                () -> checks(barV, barH, stackedV, stackedH, pieCw, pieCcw));
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ChartSupport.ready(content);
    }

    private static BarChart<String, Number> barVertical() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setAnimated(false);
        chart.setTitle("Profit by quarter");
        chart.getStyleClass().addAll("bar-v", "palette-cool");
        chart.getData().add(categorySeries("2023", 12, -4, 9));
        chart.getData().add(categorySeries("2024", 15, 6, -3));
        chart.getData().add(categorySeries("2025", 18, 11, 14));
        return chart;
    }

    private static BarChart<Number, String> barHorizontal() {
        NumberAxis x = new NumberAxis();
        CategoryAxis y = new CategoryAxis();
        BarChart<Number, String> chart = new BarChart<>(x, y);
        chart.setAnimated(false);
        chart.setTitle("Downloads (k)");
        chart.getStyleClass().addAll("bar-h", "palette-warm");
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Downloads");
        String[] names = { "macOS", "Windows", "Linux", "Other" };
        int[] values = { 42, 67, 31, 8 };
        for (int i = 0; i < names.length; i++) {
            series.getData().add(new XYChart.Data<>(values[i], names[i]));
        }
        chart.getData().add(series);
        return chart;
    }

    private static StackedBarChart<String, Number> stackedVertical() {
        CategoryAxis x = new CategoryAxis(FXCollections.observableArrayList(QUARTERS));
        NumberAxis y = new NumberAxis();
        StackedBarChart<String, Number> chart = new StackedBarChart<>(x, y);
        chart.setAnimated(false);
        chart.setTitle("Tickets");
        chart.getStyleClass().addAll("stacked-v", "palette-warm");
        chart.getData().add(categorySeries("Bugs", 14, 11, 7));
        chart.getData().add(categorySeries("Features", 9, 13, 16));
        chart.getData().add(categorySeries("Docs", 3, 5, 4));
        return chart;
    }

    private static StackedBarChart<Number, String> stackedHorizontal() {
        NumberAxis x = new NumberAxis();
        CategoryAxis y = new CategoryAxis(FXCollections.observableArrayList(QUARTERS));
        StackedBarChart<Number, String> chart = new StackedBarChart<>(x, y);
        chart.setAnimated(false);
        chart.setTitle("Hours");
        chart.setLegendSide(Side.TOP);
        chart.getStyleClass().addAll("stacked-h", "palette-warm");
        String[] names = { "Dev", "Test", "Ops" };
        int[][] values = { { 20, 24, 18 }, { 8, 10, 12 }, { 5, 4, 9 } };
        for (int s = 0; s < names.length; s++) {
            XYChart.Series<Number, String> series = new XYChart.Series<>();
            series.setName(names[s]);
            for (int q = 0; q < QUARTERS.length; q++) {
                series.getData().add(new XYChart.Data<>(values[s][q], QUARTERS[q]));
            }
            chart.getData().add(series);
        }
        return chart;
    }

    private static PieChart pieClockwise() {
        PieChart chart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Java", 38),
                new PieChart.Data("Kotlin", 17),
                new PieChart.Data("Scala", 9),
                new PieChart.Data("Groovy", 6),
                new PieChart.Data("Other", 30)));
        chart.setAnimated(false);
        chart.setTitle("JVM languages");
        chart.getStyleClass().addAll("pie-cw", "palette-cool");
        return chart;
    }

    private static PieChart pieCounterClockwise() {
        PieChart chart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Rent", 45),
                new PieChart.Data("Food", 20),
                new PieChart.Data("Travel", 15),
                new PieChart.Data("Misc", 20)));
        chart.setAnimated(false);
        chart.setTitle("Budget");
        chart.getStyleClass().addAll("pie-ccw", "palette-warm");
        return chart;
    }

    private static XYChart.Series<String, Number> categorySeries(String name, int... values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        for (int i = 0; i < values.length; i++) {
            series.getData().add(new XYChart.Data<>(QUARTERS[i], values[i]));
        }
        return series;
    }

    private static List<Check> checks(BarChart<String, Number> barV, BarChart<Number, String> barH,
            StackedBarChart<String, Number> stackedV, StackedBarChart<Number, String> stackedH, PieChart pieCw,
            PieChart pieCcw) {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("bar nodes / negative", "9 / 2", () -> barV.lookupAll(".chart-bar").stream()
                .filter(n -> !n.getStyleClass().contains("bar-legend-symbol")).count() + " / "
                + barV.lookupAll(".chart-bar.negative").size()));
        checks.add(Checks.expect("bar -fx-bar-gap / -fx-category-gap", "2 / 14",
                () -> num(barV.getBarGap()) + " / " + num(barV.getCategoryGap())));
        checks.add(Checks.expect("bar series0 fill (CSS)", "0x4c78a8ff",
                () -> backgroundFill(barV.getData().get(0).getData().get(0).getNode())));
        checks.add(Checks.run("bar y axis (auto range)", () -> range((NumberAxis) barV.getYAxis())));
        checks.add(Checks.expect("horizontal bar axis sides", "BOTTOM / LEFT",
                () -> barH.getXAxis().getSide() + " / " + barH.getYAxis().getSide()));
        checks.add(Checks.expect("horizontal bar categories", "4", () -> String.valueOf(
                ((CategoryAxis) barH.getYAxis()).getCategories().size())));
        checks.add(Checks.expect("horizontal bar -fx-legend-visible", false, barH::isLegendVisible));
        checks.add(Checks.expect("stacked -fx-category-gap", "24", () -> num(stackedV.getCategoryGap())));
        checks.add(Checks.run("stacked y axis (auto range)", () -> range((NumberAxis) stackedV.getYAxis())));
        checks.add(Checks.run("stacked horizontal x axis", () -> range((NumberAxis) stackedH.getXAxis())));
        checks.add(Checks.expect("pie slices / labels", "5 / 5",
                () -> pieCw.lookupAll(".chart-pie").stream().filter(n -> !n.getStyleClass().contains("pie-legend-symbol"))
                        .count() + " / " + pieCw.lookupAll(".chart-pie-label").size()));
        checks.add(Checks.expect("pie label texts (sorted)", "Groovy,Java,Kotlin,Other,Scala",
                () -> pieCw.lookupAll(".chart-pie-label").stream().map(n -> ((Text) n).getText()).sorted()
                        .collect(Collectors.joining(","))));
        checks.add(Checks.expect("pie CSS start angle / clockwise", "90 / false / 12",
                () -> num(pieCcw.getStartAngle()) + " / " + pieCcw.isClockwise() + " / "
                        + num(pieCcw.getLabelLineLength())));
        checks.add(Checks.run("pie data0 gradient from -fx-pie-color",
                () -> pieColor(pieCcw.getData().get(0).getNode())));
        checks.add(Checks.run("own CSS props Pie/Bar/StackedBar/CatAxis", () -> ChartSupport.ownCssProperties(
                PieChart.getClassCssMetaData(), Chart.getClassCssMetaData()) + " / "
                + ChartSupport.ownCssProperties(BarChart.getClassCssMetaData(), XYChart.getClassCssMetaData()) + " / "
                + ChartSupport.ownCssProperties(StackedBarChart.getClassCssMetaData(), XYChart.getClassCssMetaData())
                + " / "
                + ChartSupport.ownCssProperties(CategoryAxis.getClassCssMetaData(), Axis.getClassCssMetaData())));
        checks.add(Checks.expect("pie legend items / CSS legend visible", "5 / false",
                () -> pieCw.lookupAll(".chart-legend-item").size() + " / " + pieCcw.isLegendVisible()));
        return checks;
    }

    /**
     * The slice is filled by a radial gradient derived from -fx-pie-color : report its first stop.
     */
    private static String pieColor(Node slice) {
        if (slice instanceof Region region && region.getBackground() != null
                && region.getBackground().getFills().getFirst().getFill() instanceof RadialGradient gradient) {
            return gradient.getStops().getFirst().getColor() + ".." + gradient.getStops().getLast().getColor();
        }
        return backgroundFill(slice);
    }
}
