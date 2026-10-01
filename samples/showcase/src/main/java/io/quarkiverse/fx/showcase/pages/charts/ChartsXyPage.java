package io.quarkiverse.fx.showcase.pages.charts;

import static io.quarkiverse.fx.showcase.pages.charts.ChartSupport.cell;
import static io.quarkiverse.fx.showcase.pages.charts.ChartSupport.num;
import static io.quarkiverse.fx.showcase.pages.charts.ChartSupport.range;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;

/**
 * XY charts : line, area, stacked area, scatter and bubble charts, styled from a stylesheet.
 */
@Singleton
public class ChartsXyPage implements FeaturePage {

    private static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun" };

    @Override
    public String id() {
        return "charts-xy";
    }

    @Override
    public String title() {
        return "XY charts";
    }

    @Override
    public String category() {
        return Categories.CHARTS;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        LineChart<Number, Number> line = lineChart();
        AreaChart<String, Number> area = areaChart();
        StackedAreaChart<Number, Number> stacked = stackedAreaChart();
        ScatterChart<Number, Number> scatter = scatterChart();
        BubbleChart<Number, Number> bubble = bubbleChart();

        HBox top = new HBox(10,
                cell(line, "LineChart · looked-up palette, dash array, square symbols", 336, 350),
                cell(area, "AreaChart · CategoryAxis · -fx-create-symbols: false, column fill", 336, 350),
                cell(stacked, "StackedAreaChart · legend on the right (CSS -fx-legend-side)", 336, 350));

        VBox checksHolder = new VBox();
        checksHolder.setPrefSize(508, 368);
        checksHolder.setMinSize(508, 368);
        checksHolder.setMaxSize(508, 368);
        HBox bottom = new HBox(10,
                cell(scatter, "ScatterChart · seeded data · SVG -fx-shape symbols", 250, 368),
                cell(bubble, "BubbleChart · CSS -fx-tick-unit · $ tick formatter", 250, 368),
                checksHolder);

        VBox root = new VBox(10, top, bottom);
        root.getStyleClass().add("showcase-charts");
        root.getStylesheets().add(Fx.resourceUrl(ChartSupport.CSS));
        ChartSupport.checksAfterLayout(root, checksHolder, "Chart checks (after CSS and layout)",
                () -> checks(line, area, stacked, scatter, bubble));
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ChartSupport.ready(content);
    }

    private static LineChart<Number, Number> lineChart() {
        NumberAxis x = new NumberAxis("Month", 1, 12, 1);
        NumberAxis y = new NumberAxis();
        y.setLabel("Units");
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false);
        chart.setTitle("Monthly units");
        chart.getStyleClass().addAll("xy-line", "palette-cool");
        chart.getData().add(numberSeries("Product A", 1, 12, 15, 14, 18, 22, 21, 25, 28, 26, 30, 33, 35));
        chart.getData().add(numberSeries("Product B", 1, 8, 9, 13, 12, 15, 19, 18, 22, 21, 24, 23, 27));
        chart.getData().add(numberSeries("Product C", 1, 20, 18, 17, 15, 16, 14, 13, 12, 14, 11, 10, 9));
        return chart;
    }

    private static AreaChart<String, Number> areaChart() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        AreaChart<String, Number> chart = new AreaChart<>(x, y);
        chart.setAnimated(false);
        chart.setTitle("Sales by region");
        chart.getStyleClass().addAll("xy-area", "palette-cool");
        chart.getData().add(categorySeries("North", 30, 42, 38, 51, 47, 58));
        chart.getData().add(categorySeries("South", 22, 25, 33, 29, 40, 44));
        return chart;
    }

    private static StackedAreaChart<Number, Number> stackedAreaChart() {
        NumberAxis x = new NumberAxis();
        x.setLabel("Hour");
        NumberAxis y = new NumberAxis();
        y.setLabel("GW");
        StackedAreaChart<Number, Number> chart = new StackedAreaChart<>(x, y);
        chart.setAnimated(false);
        chart.setTitle("Energy mix");
        chart.getStyleClass().addAll("xy-stacked", "palette-warm");
        chart.getData().add(numberSeries("Solar", 0, 2, 3, 5, 8, 11, 13, 14, 15, 15));
        chart.getData().add(numberSeries("Wind", 0, 6, 7, 7, 8, 9, 9, 10, 11, 12));
        chart.getData().add(numberSeries("Hydro", 0, 10, 10, 9, 9, 9, 8, 8, 8, 7));
        return chart;
    }

    private static ScatterChart<Number, Number> scatterChart() {
        NumberAxis x = new NumberAxis(0, 100, 20);
        NumberAxis y = new NumberAxis(0, 100, 20);
        ScatterChart<Number, Number> chart = new ScatterChart<>(x, y);
        chart.setAnimated(false);
        chart.setTitle("Clusters");
        chart.getStyleClass().addAll("xy-scatter", "palette-cool");
        // fixed seed : java.util.Random is fully specified, the same sequence on every run
        Random random = new Random(7);
        int[][] centers = { { 25, 30 }, { 60, 72 }, { 80, 25 } };
        String[] names = { "Alpha", "Beta", "Gamma" };
        for (int s = 0; s < centers.length; s++) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(names[s]);
            for (int i = 0; i < 12; i++) {
                series.getData().add(new XYChart.Data<>(centers[s][0] + random.nextInt(25) - 12,
                        centers[s][1] + random.nextInt(25) - 12));
            }
            chart.getData().add(series);
        }
        return chart;
    }

    private static BubbleChart<Number, Number> bubbleChart() {
        NumberAxis x = new NumberAxis();
        x.setAutoRanging(false);
        x.setLowerBound(0);
        x.setUpperBound(10);
        NumberAxis y = new NumberAxis();
        y.setAutoRanging(false);
        y.setLowerBound(0);
        y.setUpperBound(10);
        y.setTickLabelFormatter(new NumberAxis.DefaultFormatter(y, "$", null));
        BubbleChart<Number, Number> chart = new BubbleChart<>(x, y);
        chart.setAnimated(false);
        chart.setTitle("Market share");
        chart.getStyleClass().addAll("xy-bubble", "palette-cool");
        XYChart.Series<Number, Number> a = new XYChart.Series<>();
        a.setName("2024");
        a.getData().add(new XYChart.Data<>(2, 3, 0.8));
        a.getData().add(new XYChart.Data<>(4, 6.5, 1.2));
        a.getData().add(new XYChart.Data<>(7, 4, 0.6));
        a.getData().add(new XYChart.Data<>(8, 8, 1.0));
        XYChart.Series<Number, Number> b = new XYChart.Series<>();
        b.setName("2025");
        b.getData().add(new XYChart.Data<>(3, 7.5, 0.5));
        b.getData().add(new XYChart.Data<>(5, 2, 0.9));
        b.getData().add(new XYChart.Data<>(6, 6, 1.4));
        b.getData().add(new XYChart.Data<>(9, 3, 0.7));
        chart.getData().add(a);
        chart.getData().add(b);
        return chart;
    }

    private static XYChart.Series<Number, Number> numberSeries(String name, int firstX, int... values) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(name);
        for (int i = 0; i < values.length; i++) {
            series.getData().add(new XYChart.Data<>(firstX + i, values[i]));
        }
        return series;
    }

    private static XYChart.Series<String, Number> categorySeries(String name, int... values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        for (int i = 0; i < values.length; i++) {
            series.getData().add(new XYChart.Data<>(MONTHS[i], values[i]));
        }
        return series;
    }

    private static List<Check> checks(LineChart<Number, Number> line, AreaChart<String, Number> area,
            StackedAreaChart<Number, Number> stacked, ScatterChart<Number, Number> scatter,
            BubbleChart<Number, Number> bubble) {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("line series / points", "3 / 36",
                () -> line.getData().size() + " / " + line.getData().stream().mapToInt(s -> s.getData().size()).sum()));
        checks.add(Checks.run("line y axis (auto range)", () -> range((NumberAxis) line.getYAxis())));
        checks.add(Checks.expect("series0 stroke (CHART_COLOR_1)", "0x4c78a8ff",
                () -> ((Shape) line.getData().get(0).getNode()).getStroke().toString()));
        checks.add(Checks.expect("series1 -fx-stroke-dash-array", "[6.0, 4.0]",
                () -> ((Shape) line.getData().get(1).getNode()).getStrokeDashArray().toString()));
        checks.add(Checks.expect("series2 stroke / width", "0x222222ff / 1.5",
                () -> ((Shape) line.getData().get(2).getNode()).getStroke() + " / "
                        + ((Shape) line.getData().get(2).getNode()).getStrokeWidth()));
        checks.add(Checks.expect("area categories",
                "[Jan, Feb, Mar, Apr, May, Jun]", () -> ((CategoryAxis) area.getXAxis()).getCategories().toString()));
        checks.add(Checks.expect("area -fx-create-symbols", false, area::getCreateSymbols));
        checks.add(Checks.expect("area fill series0 (TRANS_20)", "0x4c78a833", () -> {
            Group group = (Group) area.getData().get(0).getNode();
            return ((Path) group.getChildren().get(0)).getFill().toString();
        }));
        checks.add(Checks.expect("stacked -fx-legend-side", "RIGHT", () -> stacked.getLegendSide().name()));
        checks.add(Checks.run("stacked y axis (auto range)", () -> range((NumberAxis) stacked.getYAxis())));
        checks.add(Checks.expect("scatter symbols / SVG shapes", "36 / 12", () -> {
            List<Node> symbols = ChartSupport.lookupAll(scatter, ".chart-symbol").stream()
                    .filter(n -> !n.getStyleClass().contains("chart-legend-item-symbol")).toList();
            long shaped = symbols.stream()
                    .filter(n -> n instanceof Region r && r.getShape() != null && r.getShape().getClass().getSimpleName()
                            .equals("SVGPath"))
                    .count();
            return symbols.size() + " / " + shaped;
        }));
        checks.add(Checks.expect("bubble -fx-tick-unit / minor", "2.5 / 2",
                () -> num(((NumberAxis) bubble.getYAxis()).getTickUnit()) + " / "
                        + ((NumberAxis) bubble.getYAxis()).getMinorTickCount()));
        checks.add(Checks.expect("bubble $ tick formatter", "$7.5",
                () -> ((NumberAxis) bubble.getYAxis()).getTickLabelFormatter().toString(7.5)));
        checks.add(Checks.run("own CSS props XY/Line/Area/StArea/NumAxis", () -> ChartSupport.ownCssProperties(
                XYChart.getClassCssMetaData(), Chart.getClassCssMetaData()) + " / "
                + ChartSupport.ownCssProperties(LineChart.getClassCssMetaData(), XYChart.getClassCssMetaData()) + " / "
                + ChartSupport.ownCssProperties(AreaChart.getClassCssMetaData(), XYChart.getClassCssMetaData()) + " / "
                + ChartSupport.ownCssProperties(StackedAreaChart.getClassCssMetaData(), XYChart.getClassCssMetaData())
                + " / "
                + ChartSupport.ownCssProperties(NumberAxis.getClassCssMetaData(), ValueAxis.getClassCssMetaData())));
        checks.add(Checks.expect("legend items per chart", "3,2,3,3,2",
                () -> List.<Chart> of(line, area, stacked, scatter, bubble).stream()
                        .map(c -> String.valueOf(c.lookupAll(".chart-legend-item").size()))
                        .collect(Collectors.joining(","))));
        return checks;
    }
}
