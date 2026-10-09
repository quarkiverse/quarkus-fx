package io.quarkiverse.fx.showcase.pages.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

@Singleton
public class PanesPage implements FeaturePage {

    private static final double W = 333;
    private static final double H = 165;

    private static final String RED = "#e53935";
    private static final String BLUE = "#1e88e5";
    private static final String GREEN = "#43a047";
    private static final String ORANGE = "#fb8c00";
    private static final String PURPLE = "#8e24aa";
    private static final String TEAL = "#00897b";
    private static final String GREY = "#546e7a";
    private static final String PINK = "#d81b60";

    @Override
    public String id() {
        return "layout-panes";
    }

    @Override
    public String title() {
        return "Layout panes";
    }

    @Override
    public String category() {
        return Categories.LAYOUT_CSS;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        VBox root = Kit.page("panes-page");

        // HBox
        Label h1 = Kit.block("A", RED, 40, 40);
        Label h2 = Kit.block("B", BLUE, 60, 56);
        Label h3 = Kit.growing("C grow", GREEN, 50, 40);
        h3.setMaxHeight(40);
        Label h4 = Kit.block("D", ORANGE, 50, 30);
        HBox hbox1 = new HBox(6, h1, h2, h3, h4);
        HBox.setHgrow(h3, Priority.ALWAYS);
        HBox.setMargin(h4, new Insets(0, 0, 0, 12));
        hbox1.setAlignment(Pos.CENTER_LEFT);
        hbox1.setPadding(new Insets(6));
        hbox1.getStyleClass().add("pane-bg");
        Label h5 = Kit.block("fill", PURPLE, 40, 30);
        h5.setMaxHeight(Double.MAX_VALUE);
        HBox hbox2 = new HBox(4, h5, Kit.block("E", TEAL, 50, 30), Kit.block("F", GREY, 30, 50));
        hbox2.setAlignment(Pos.BOTTOM_RIGHT);
        hbox2.setPadding(new Insets(6));
        hbox2.setPrefHeight(66);
        hbox2.getStyleClass().add("pane-bg");
        VBox hboxes = new VBox(8, hbox1, hbox2);
        hboxes.setPadding(new Insets(8));
        Node hboxDemo = Kit.demo("HBox · spacing, hgrow on C, margin on D · BOTTOM_RIGHT, fillHeight", W, H, hboxes);

        // VBox
        Label v1 = Kit.growing("1", RED, 60, 24);
        v1.setMaxHeight(24);
        Label v2 = Kit.growing("2 vgrow", BLUE, 60, 30);
        Label v3 = Kit.growing("3", GREEN, 60, 20);
        v3.setMaxHeight(20);
        VBox vbox1 = new VBox(4, v1, v2, v3);
        VBox.setVgrow(v2, Priority.ALWAYS);
        vbox1.setPadding(new Insets(6));
        vbox1.setPrefWidth(100);
        vbox1.getStyleClass().add("pane-bg");
        VBox vbox2 = new VBox(4, Kit.block("30", ORANGE, 30, 26), Kit.block("60", PURPLE, 60, 26), Kit.block("45", TEAL, 45, 26));
        vbox2.setFillWidth(false);
        vbox2.setAlignment(Pos.TOP_CENTER);
        vbox2.setPadding(new Insets(6));
        vbox2.setPrefWidth(90);
        vbox2.getStyleClass().add("pane-bg");
        Label v4 = Kit.block("m", PINK, 40, 26);
        VBox vbox3 = new VBox(4, Kit.block("x", GREY, 40, 26), v4);
        VBox.setMargin(v4, new Insets(10, 0, 0, 20));
        vbox3.setFillWidth(false);
        vbox3.setAlignment(Pos.BOTTOM_RIGHT);
        vbox3.setPadding(new Insets(6));
        vbox3.setPrefWidth(100);
        vbox3.getStyleClass().add("pane-bg");
        HBox vboxes = new HBox(8, vbox1, vbox2, vbox3);
        vboxes.setPadding(new Insets(8));
        Node vboxDemo = Kit.demo("VBox · fillWidth + vgrow · TOP_CENTER · BOTTOM_RIGHT + margin", W, H, vboxes);

        // FlowPane
        FlowPane flow = new FlowPane(5, 5);
        flow.setPadding(new Insets(6));
        flow.setPrefWrapLength(190);
        flow.setRowValignment(VPos.CENTER);
        flow.getStyleClass().add("pane-bg");
        String[] colors = { RED, BLUE, GREEN, ORANGE, PURPLE, TEAL, GREY, PINK };
        int[] widths = { 30, 54, 40, 70, 36, 58, 44, 62, 30, 50, 40, 66 };
        for (int i = 0; i < widths.length; i++) {
            flow.getChildren().add(Kit.block(String.valueOf(i + 1), colors[i % colors.length], widths[i], i % 2 == 0 ? 22 : 30));
        }
        FlowPane vflow = new FlowPane(Orientation.VERTICAL, 4, 4);
        vflow.setPadding(new Insets(6));
        vflow.setPrefWrapLength(140);
        vflow.setColumnHalignment(HPos.CENTER);
        vflow.getStyleClass().add("pane-bg");
        for (int i = 0; i < 9; i++) {
            vflow.getChildren().add(Kit.block("v" + (i + 1), colors[(i + 3) % colors.length], i % 3 == 0 ? 40 : 28, 22));
        }
        HBox flows = new HBox(8, flow, vflow);
        flows.setPadding(new Insets(6));
        flows.setFillHeight(false);
        Node flowDemo = Kit.demo("FlowPane · horizontal wrap 190 · vertical wrap 140", W, H, flows);

        // TilePane
        TilePane tiles = new TilePane(4, 4);
        tiles.setPrefColumns(4);
        tiles.setPadding(new Insets(6));
        tiles.setTileAlignment(Pos.BOTTOM_CENTER);
        tiles.getStyleClass().add("pane-bg");
        double[][] sizes = { { 30, 20 }, { 50, 30 }, { 40, 40 }, { 20, 20 }, { 60, 24 }, { 35, 35 }, { 45, 20 }, { 25, 40 } };
        for (int i = 0; i < sizes.length; i++) {
            tiles.getChildren().add(Kit.block(String.valueOf(i + 1), colors[i], sizes[i][0], sizes[i][1]));
        }
        TilePane smallTiles = new TilePane(2, 2);
        smallTiles.setPrefColumns(8);
        smallTiles.setPrefTileWidth(34);
        smallTiles.setPrefTileHeight(28);
        smallTiles.setTileAlignment(Pos.TOP_LEFT);
        smallTiles.setPadding(new Insets(6));
        smallTiles.getStyleClass().add("pane-bg");
        for (int i = 0; i < 8; i++) {
            smallTiles.getChildren().add(Kit.block("", colors[(i + 5) % colors.length], 20, 20));
        }
        VBox tileBox = new VBox(6, tiles, smallTiles);
        tileBox.setFillWidth(false);
        tileBox.setPadding(new Insets(6));
        Node tileDemo = Kit.demo("TilePane · 4 columns, BOTTOM_CENTER · fixed 34x28 tiles TOP_LEFT", W, H, tileBox);

        // GridPane
        GridPane grid = new GridPane();
        grid.setGridLinesVisible(true);
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(6));
        ColumnConstraints c0 = new ColumnConstraints(60);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(40);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setHalignment(HPos.CENTER);
        grid.getColumnConstraints().addAll(c0, c1, c2);
        RowConstraints r0 = new RowConstraints(32);
        RowConstraints r1 = new RowConstraints(44);
        r1.setValignment(VPos.CENTER);
        RowConstraints r2 = new RowConstraints();
        r2.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(r0, r1, r2);
        Label ga = Kit.block("A", RED, 44, 24);
        GridPane.setHalignment(ga, HPos.CENTER);
        grid.add(ga, 0, 0);
        grid.add(Kit.growing("B colspan 2", BLUE, 80, 24), 1, 0, 2, 1);
        grid.add(Kit.growing("C rowspan", GREEN, 44, 40), 0, 1, 1, 2);
        Label gd = Kit.block("D", ORANGE, 40, 24);
        GridPane.setHalignment(gd, HPos.RIGHT);
        GridPane.setValignment(gd, VPos.BOTTOM);
        grid.add(gd, 1, 1);
        grid.add(Kit.block("E", PURPLE, 40, 24), 2, 1);
        Label gf = Kit.block("F", TEAL, 40, 24);
        GridPane.setValignment(gf, VPos.TOP);
        grid.add(gf, 1, 2);
        Label gg = Kit.block("G", GREY, 40, 24);
        GridPane.setHalignment(gg, HPos.LEFT);
        GridPane.setValignment(gg, VPos.BOTTOM);
        GridPane.setMargin(gg, new Insets(0, 0, 4, 6));
        grid.add(gg, 2, 2);
        Node gridDemo = Kit.demo("GridPane · grid lines, 60px / 40% / hgrow, spans, alignment", W, H, grid);

        // BorderPane
        BorderPane border = new BorderPane();
        Label top = Kit.growing("top", RED, 100, 24);
        top.setMaxHeight(24);
        Label bottom = Kit.block("bottom (BOTTOM_RIGHT)", BLUE, 150, 22);
        BorderPane.setAlignment(bottom, Pos.BOTTOM_RIGHT);
        Label left = Kit.growing("left", GREEN, 54, 40);
        left.setMaxWidth(54);
        Label right = Kit.block("right", ORANGE, 54, 40);
        BorderPane.setAlignment(right, Pos.CENTER);
        Label center = Kit.growing("center", PURPLE, 60, 40);
        BorderPane.setMargin(center, new Insets(6));
        border.setTop(top);
        border.setBottom(bottom);
        border.setLeft(left);
        border.setRight(right);
        border.setCenter(center);
        border.setPadding(new Insets(6));
        border.getStyleClass().add("pane-bg");
        Node borderDemo = Kit.demo("BorderPane · top/bottom/left/right/center, margin on center", W, H, border);

        // StackPane
        StackPane stack = new StackPane();
        stack.setMaxSize(250, 130);
        stack.getStyleClass().add("pane-bg");
        Rectangle base = new Rectangle(180, 100, Color.web("#b0bec5"));
        Label tl = Kit.block("TL", RED, 40, 28);
        StackPane.setAlignment(tl, Pos.TOP_LEFT);
        Label tr = Kit.block("TR", BLUE, 40, 28);
        StackPane.setAlignment(tr, Pos.TOP_RIGHT);
        StackPane.setMargin(tr, new Insets(6, 6, 0, 0));
        Label bl = Kit.block("BL", GREEN, 40, 28);
        StackPane.setAlignment(bl, Pos.BOTTOM_LEFT);
        Label br = Kit.block("BR", ORANGE, 40, 28);
        StackPane.setAlignment(br, Pos.BOTTOM_RIGHT);
        Label overlay = Kit.block("CENTER 70%", PURPLE, 110, 44);
        overlay.setOpacity(0.7);
        Label baseline = Kit.block("BASELINE_CENTER", TEAL, 110, 20);
        StackPane.setAlignment(baseline, Pos.BASELINE_CENTER);
        stack.getChildren().addAll(base, overlay, baseline, tl, tr, bl, br);
        Node stackDemo = Kit.demo("StackPane · child alignment, margin, overlap order", W, H, stack);

        // AnchorPane
        AnchorPane anchor = new AnchorPane();
        anchor.getStyleClass().add("pane-bg");
        Label a1 = Kit.block("top-left", RED, 60, 26);
        AnchorPane.setTopAnchor(a1, 8.0);
        AnchorPane.setLeftAnchor(a1, 8.0);
        Label a2 = Kit.block("top-right", BLUE, 64, 26);
        AnchorPane.setTopAnchor(a2, 8.0);
        AnchorPane.setRightAnchor(a2, 8.0);
        Label a3 = Kit.growing("left + right anchors (stretched)", GREEN, 60, 26);
        a3.setMaxHeight(26);
        AnchorPane.setBottomAnchor(a3, 8.0);
        AnchorPane.setLeftAnchor(a3, 8.0);
        AnchorPane.setRightAnchor(a3, 8.0);
        Label a4 = Kit.growing("top+bottom", ORANGE, 70, 30);
        a4.setMaxWidth(70);
        AnchorPane.setTopAnchor(a4, 42.0);
        AnchorPane.setBottomAnchor(a4, 42.0);
        AnchorPane.setLeftAnchor(a4, 96.0);
        Label a5 = Kit.block("bottom-right", PURPLE, 80, 26);
        AnchorPane.setBottomAnchor(a5, 42.0);
        AnchorPane.setRightAnchor(a5, 8.0);
        anchor.getChildren().addAll(a1, a2, a3, a4, a5);
        Node anchorDemo = Kit.demo("AnchorPane · corner anchors, stretched by opposite anchors", W, H, anchor);

        // Pane with absolute positions
        Pane pane = new Pane();
        pane.getStyleClass().add("pane-bg");
        Label p1 = Kit.block("(10,10)", RED, 60, 26);
        p1.relocate(10, 10);
        Label p2 = Kit.block("layoutX 90", BLUE, 76, 26);
        p2.setLayoutX(90);
        p2.setLayoutY(50);
        Label p3 = Kit.block("rotate 30", GREEN, 70, 26);
        p3.relocate(200, 20);
        p3.setRotate(30);
        Label p4 = Kit.block("translate", ORANGE, 70, 26);
        p4.relocate(10, 100);
        p4.setTranslateX(20);
        p4.setTranslateY(10);
        Circle circle = new Circle(250, 115, 22, Color.web(PURPLE));
        Line line = new Line(10, 150, 320, 150);
        line.setStroke(Color.web(GREY));
        line.getStrokeDashArray().addAll(6.0, 4.0);
        Polygon triangle = new Polygon(150, 95, 180, 145, 120, 145);
        triangle.setFill(Color.web(TEAL));
        Group scaled = new Group(new Rectangle(30, 20, Color.web(PINK)));
        scaled.getTransforms().addAll(new Scale(1.5, 1.5), new Rotate(-10));
        scaled.relocate(262, 56);
        pane.getChildren().addAll(line, p1, p2, p3, p4, circle, triangle, scaled);
        Node paneDemo = Kit.demo("Pane · relocate, layoutX/Y, rotate, translate, shapes, Group transforms", W, H, pane);

        GridPane demos = Kit.grid(3, W, 14, 8);
        Kit.addAll(demos, 3, hboxDemo, vboxDemo, flowDemo, tileDemo, gridDemo, borderDemo, stackDemo, anchorDemo, paneDemo);

        HBox checks = Kit.checksRow(1027);
        root.getChildren().addAll(demos, checks);

        Kit.whenShown(root, 3, () -> {
            List<Check> leftChecks = new ArrayList<>();
            leftChecks.add(Check.info("HBox child x (A B C D)",
                    List.of(h1, h2, h3, h4).stream().map(n -> Kit.num(n.getLayoutX())).collect(Collectors.joining(", "))
                            + " · C width " + Kit.num(h3.getWidth())));
            leftChecks.add(Check.info("VBox vgrow height · margin x",
                    Kit.num(v2.getHeight()) + " · " + Kit.num(v4.getLayoutX())));
            leftChecks.add(Check.info("FlowPane rows · vertical columns",
                    distinct(flow.getChildren(), true) + " · " + distinct(vflow.getChildren(), false)));
            leftChecks.add(Check.info("TilePane tile size",
                    Kit.num(tiles.getTileWidth()) + "x" + Kit.num(tiles.getTileHeight()) + " · "
                            + Kit.num(smallTiles.getTileWidth()) + "x" + Kit.num(smallTiles.getTileHeight())));
            List<Check> rightChecks = new ArrayList<>();
            rightChecks.add(Check.info("GridPane column widths",
                    Kit.num(grid.getCellBounds(0, 0).getWidth()) + ", " + Kit.num(grid.getCellBounds(1, 0).getWidth()) + ", "
                            + Kit.num(grid.getCellBounds(2, 0).getWidth()) + " · " + grid.getColumnCount() + "x"
                            + grid.getRowCount()));
            rightChecks.add(Check.info("BorderPane center bounds", Kit.bounds(center.getBoundsInParent())));
            rightChecks.add(Check.info("StackPane TR · AnchorPane stretched",
                    Kit.num(tr.getLayoutX()) + ", " + Kit.num(tr.getLayoutY()) + " · " + Kit.num(a3.getWidth()) + " / "
                            + Kit.num(a4.getHeight())));
            rightChecks.add(Check.info("Pane rotated bounds", Kit.bounds(p3.getBoundsInParent())));
            Kit.fillChecks(checks, "Layout results", leftChecks, "Layout results (2)", rightChecks);
        });
        return root;
    }

    /**
     * Number of rows (or columns) : the children of a row are vertically centered (rowValignment CENTER), those of a
     * column horizontally centered (columnHalignment CENTER), so they share the same center.
     */
    private static int distinct(List<Node> nodes, boolean byY) {
        TreeSet<Double> values = new TreeSet<>();
        for (Node node : nodes) {
            Bounds bounds = node.getBoundsInParent();
            values.add(byY ? bounds.getCenterY() : bounds.getCenterX());
        }
        return values.size();
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Kit.ready(content);
    }
}
