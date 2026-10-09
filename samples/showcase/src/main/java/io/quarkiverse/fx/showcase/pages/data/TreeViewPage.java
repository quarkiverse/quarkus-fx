package io.quarkiverse.fx.showcase.pages.data;

import static io.quarkiverse.fx.showcase.pages.data.DataUi.check;
import static io.quarkiverse.fx.showcase.pages.data.DataUi.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

@Singleton
public class TreeViewPage implements FeaturePage {

    private static final String DATE = "2025-03-14";

    @Override
    public String id() {
        return "data-treeview";
    }

    @Override
    public String title() {
        return "TreeView & TreeTableView";
    }

    @Override
    public String category() {
        return Categories.DATA;
    }

    @Override
    public int order() {
        return 30;
    }

    static Node folderIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M0,1.5 L5.5,1.5 L7,3.5 L15,3.5 L15,13 L0,13 Z");
        path.setFill(Color.web("#ffb300"));
        path.setStroke(Color.web("#c68400"));
        path.setStrokeWidth(0.8);
        return path;
    }

    static Node fileIcon(String type) {
        Color color = switch (type) {
            case "Java" -> Color.web("#e65100");
            case "CSS" -> Color.web("#1565c0");
            case "Image" -> Color.web("#2e7d32");
            case "Archive" -> Color.web("#6a1b9a");
            default -> Color.web("#78909c");
        };
        Rectangle page = new Rectangle(11, 14, color.deriveColor(0, 0.35, 1.25, 1));
        page.setStroke(color);
        page.setStrokeWidth(1);
        page.setArcWidth(3);
        page.setArcHeight(3);
        return page;
    }

    private static TreeItem<String> folder(String name, boolean expanded, List<TreeItem<String>> children) {
        TreeItem<String> item = new TreeItem<>(name, folderIcon());
        item.getChildren().setAll(children);
        item.setExpanded(expanded);
        return item;
    }

    private static TreeItem<String> leaf(String name, String type) {
        return new TreeItem<>(name, fileIcon(type));
    }

    private static TreeItem<FileEntry> entry(String name, String type, long size) {
        FileEntry entry = new FileEntry(name, type, size, DATE);
        return new TreeItem<>(entry, "Folder".equals(type) ? folderIcon() : fileIcon(type));
    }

    @SafeVarargs
    private static TreeItem<FileEntry> dir(String name, boolean expanded, TreeItem<FileEntry>... children) {
        long size = 0;
        for (TreeItem<FileEntry> child : children) {
            size += child.getValue().getSize();
        }
        TreeItem<FileEntry> item = entry(name, "Folder", size);
        item.getChildren().setAll(children);
        item.setExpanded(expanded);
        return item;
    }

    static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String path(TreeItem<?> item) {
        List<String> names = new ArrayList<>();
        for (TreeItem<?> i = item; i != null; i = i.getParent()) {
            names.addFirst(String.valueOf(i.getValue()));
        }
        return String.join("/", names);
    }

    private static String state(CheckBoxTreeItem<?> item) {
        return item.isIndeterminate() ? "indeterminate" : item.isSelected() ? "selected" : "unselected";
    }

    @Override
    public Node build() {
        // --- TreeView with graphics
        TreeItem<String> treeSelected = leaf("TreeView", "Java");
        TreeItem<String> treeRoot = new TreeItem<>("Showcase",
                new ImageView(new Image(Fx.resourceUrl("/showcase/images/icon-16.png"))));
        treeRoot.setExpanded(true);
        treeRoot.getChildren().setAll(List.of(
                folder("Controls", true, List.of(leaf("Buttons", "Java"), leaf("Inputs", "Java"))),
                folder("Data & Containers", true, List.of(leaf("ListView", "Java"), leaf("TableView", "Java"),
                        treeSelected, leaf("Containers", "Java"), leaf("data.css", "CSS"))),
                folder("Charts", false, List.of(leaf("Pie", "Java"), leaf("Line", "Java"))),
                folder("Images", true, List.of(leaf("pattern.png", "Image"), leaf("photo.jpg", "Image"))),
                folder("Media", false, List.of(leaf("clip.mp4", "Other"))),
                leaf("README.md", "Other")));
        TreeView<String> tree = new TreeView<>(treeRoot);
        tree.getSelectionModel().select(treeSelected);

        // --- CheckBoxTreeItem + CheckBoxTreeCell
        CheckBoxTreeItem<String> groceries = new CheckBoxTreeItem<>("Groceries");
        CheckBoxTreeItem<String> fruits = new CheckBoxTreeItem<>("Fruits");
        CheckBoxTreeItem<String> vegetables = new CheckBoxTreeItem<>("Vegetables");
        CheckBoxTreeItem<String> dairy = new CheckBoxTreeItem<>("Dairy");
        CheckBoxTreeItem<String> bakery = new CheckBoxTreeItem<>("Bakery");
        CheckBoxTreeItem<String> independent = new CheckBoxTreeItem<>("Independent");
        independent.setIndependent(true);
        groceries.getChildren().setAll(List.of(fruits, vegetables, dairy, bakery, independent));
        List<CheckBoxTreeItem<String>> toSelect = new ArrayList<>();
        for (String name : List.of("Apple", "Banana", "Cherry")) {
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>(name);
            fruits.getChildren().add(item);
            if (!name.equals("Cherry")) {
                toSelect.add(item);
            }
        }
        for (String name : List.of("Carrot", "Leek")) {
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>(name);
            vegetables.getChildren().add(item);
            toSelect.add(item);
        }
        for (String name : List.of("Milk", "Cheese")) {
            dairy.getChildren().add(new CheckBoxTreeItem<>(name));
        }
        for (String name : List.of("Bread", "Croissant")) {
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>(name);
            bakery.getChildren().add(item);
            toSelect.add(item);
        }
        CheckBoxTreeItem<String> independentChild = new CheckBoxTreeItem<>("Child");
        independent.getChildren().add(independentChild);
        // state propagates on changes only : select the leaves once the tree is assembled
        toSelect.forEach(item -> item.setSelected(true));
        independent.setSelected(true);
        groceries.setExpanded(true);
        fruits.setExpanded(true);
        vegetables.setExpanded(true);
        dairy.setExpanded(true);
        independent.setExpanded(true);
        TreeView<String> checkTree = new TreeView<>(groceries);
        checkTree.setCellFactory(CheckBoxTreeCell.forTreeView());

        // --- TreeTableView with TreeItemPropertyValueFactory, sorted by size (descending)
        TreeItem<FileEntry> resources = dir("resources", true,
                entry("application.properties", "Text", 612),
                entry("app.css", "CSS", 1034),
                entry("pattern.png", "Image", 48213));
        TreeItem<FileEntry> runner = entry("SnapshotRunner.java", "Java", 11240);
        TreeItem<FileEntry> src = dir("src", true,
                dir("main", true,
                        dir("java", true,
                                entry("ShowcaseApp.java", "Java", 4210),
                                entry("MainView.java", "Java", 7832),
                                runner),
                        resources));
        TreeItem<FileEntry> fileRoot = dir("showcase", true,
                entry("README.md", "Text", 2890),
                src,
                dir("target", false,
                        entry("showcase-runner", "Archive", 104857600),
                        entry("quarkus-run.jar", "Archive", 712)),
                entry("pom.xml", "Text", 5120));

        TreeTableView<FileEntry> treeTable = new TreeTableView<>(fileRoot);
        TreeTableColumn<FileEntry, String> nameCol = new TreeTableColumn<>("Name");
        nameCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);
        TreeTableColumn<FileEntry, String> typeCol = new TreeTableColumn<>("Type");
        typeCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("type"));
        typeCol.setPrefWidth(70);
        TreeTableColumn<FileEntry, Number> sizeCol = new TreeTableColumn<>("Size");
        sizeCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("size"));
        sizeCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : humanSize(item.longValue()));
            }
        });
        sizeCol.getStyleClass().add("numeric-cell");
        sizeCol.setPrefWidth(90);
        sizeCol.setSortType(TreeTableColumn.SortType.DESCENDING);
        TreeTableColumn<FileEntry, String> modifiedCol = new TreeTableColumn<>("Modified");
        modifiedCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("modified"));
        modifiedCol.setPrefWidth(90);
        // no directoryProperty() nor getDirectory() : TreeItemPropertyValueFactory falls back to isDirectory()
        TreeTableColumn<FileEntry, Boolean> directoryCol = new TreeTableColumn<>("Directory");
        directoryCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("directory"));
        directoryCol.setVisible(false);
        treeTable.getColumns().setAll(List.of(nameCol, typeCol, sizeCol, modifiedCol, directoryCol));
        treeTable.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // selected before sorting : the sort keeps the selected item
        treeTable.getSelectionModel().select(runner);
        treeTable.getSortOrder().add(sizeCol);

        // --- checks
        List<Check> treeChecks = new ArrayList<>();
        treeChecks.add(Checks.expect("expanded item count", 16, tree::getExpandedItemCount));
        treeChecks.add(Checks.expect("selected path", "Showcase/Data & Containers/TreeView",
                () -> path(tree.getSelectionModel().getSelectedItem())));
        treeChecks.add(Checks.expect("selected row / level", "7 / 2",
                () -> tree.getRow(treeSelected) + " / " + tree.getTreeItemLevel(treeSelected)));
        treeChecks.add(Checks.expect("Fruits (2 of 3)", "indeterminate", () -> state(fruits)));
        treeChecks.add(Checks.expect("Vegetables (2 of 2)", "selected", () -> state(vegetables)));
        treeChecks.add(Checks.expect("Dairy (0 of 2)", "unselected", () -> state(dairy)));
        treeChecks.add(Checks.expect("Groceries (root)", "indeterminate", () -> state(groceries)));
        treeChecks.add(Checks.expect("Bakery (collapsed, 2 of 2)", "selected", () -> state(bakery)));
        treeChecks.add(Checks.expect("independent item / child", "selected / unselected",
                () -> state(independent) + " / " + state(independentChild)));
        treeChecks.add(Checks.run("selection events (detached)", () -> {
            CheckBoxTreeItem<String> parent = new CheckBoxTreeItem<>("parent");
            CheckBoxTreeItem<String> a = new CheckBoxTreeItem<>("a");
            CheckBoxTreeItem<String> b = new CheckBoxTreeItem<>("b");
            parent.getChildren().setAll(List.of(a, b));
            AtomicInteger events = new AtomicInteger();
            parent.addEventHandler(CheckBoxTreeItem.checkBoxSelectionChangedEvent(), e -> events.incrementAndGet());
            a.setSelected(true);
            b.setSelected(true);
            return "events=" + events.get() + " parent=" + state(parent);
        }));

        List<Check> tableChecks = new ArrayList<>();
        tableChecks.add(Checks.expect("TreeItemPropertyValueFactory", "src", () -> nameCol.getCellData(src)));
        tableChecks.add(Checks.expect("TreeItem xxxProperty() used", true,
                () -> (Object) sizeCol.getCellObservableValue(runner) == runner.getValue().sizeProperty()));
        tableChecks.add(Checks.expect("is-getter fallback (hidden col)", "src=true, SnapshotRunner.java=false",
                () -> "src=" + directoryCol.getCellData(src) + ", SnapshotRunner.java=" + directoryCol.getCellData(runner)));
        tableChecks.add(Checks.expect("folder size (sum)", "48.7 KB", () -> humanSize(sizeCol.getCellData(resources)
                .longValue())));
        tableChecks.add(Checks.expect("TreeTableView sort order", "Size DESCENDING", () -> treeTable.getSortOrder().stream()
                .map(c -> c.getText() + " " + c.getSortType()).collect(Collectors.joining(", "))));
        tableChecks.add(Checks.expect("sorted root children", "[target, src, pom.xml, README.md]",
                () -> fileRoot.getChildren().stream().map(i -> i.getValue().getName()).toList().toString()));
        tableChecks.add(Checks.expect("sorted descendants", "[pattern.png, app.css, application.properties]",
                () -> resources.getChildren().stream().map(i -> i.getValue().getName()).toList().toString()));
        tableChecks.add(Checks.expect("TreeTableView selected row", "SnapshotRunner.java",
                () -> treeTable.getSelectionModel().getSelectedItem().getValue().getName()));

        DataUi.ChecksHolder left = new DataUi.ChecksHolder("TreeView & CheckBoxTreeItem", treeChecks);
        DataUi.ChecksHolder right = new DataUi.ChecksHolder("TreeTableView", tableChecks);
        left.setPrefWidth(508);
        right.setPrefWidth(508);

        HBox top = DataUi.row(
                demo("TreeView · graphics · expanded · selected", tree, 250, 420),
                demo("CheckBoxTreeItem · CheckBoxTreeCell", checkTree, 250, 420),
                demo("TreeTableView · TreeItemPropertyValueFactory · sorted by Size ↓", treeTable, 504, 420));
        VBox root = DataUi.page(new VBox(12, top, DataUi.row(left, right)));

        CompletionStage<?> ready = Fx.pulses(4).thenRun(() -> {
            left.complete(List.of(
                    check("TreeView skin", () -> tree.getSkin().getClass().getSimpleName()),
                    check("TreeView visible rows", () -> DataUi.visibleRange(tree)),
                    check("cell graphic (row 0)", () -> {
                        TreeCell<?> cell = DataUi.cell(tree, ".tree-cell", 0);
                        return cell.getGraphic() == null ? "none" : cell.getGraphic().getClass().getSimpleName();
                    }),
                    check("CheckBoxTreeCell", () -> DataUi.cell(checkTree, ".tree-cell", 1).getClass().getSimpleName())));
            right.complete(List.of(
                    check("TreeTableView skin", () -> treeTable.getSkin().getClass().getSimpleName()),
                    check("TreeTableView visible rows", () -> DataUi.visibleRange(treeTable)),
                    check("TreeTableView column widths", () -> treeTable.getVisibleLeafColumns().stream()
                            .map(c -> String.valueOf(Math.round(c.getWidth()))).collect(Collectors.joining(" ")))));
        });
        DataUi.setReady(root, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return DataUi.ready(content);
    }
}
