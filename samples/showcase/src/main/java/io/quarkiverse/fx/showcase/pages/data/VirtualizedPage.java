package io.quarkiverse.fx.showcase.pages.data;

import static io.quarkiverse.fx.showcase.pages.data.DataUi.check;
import static io.quarkiverse.fx.showcase.pages.data.DataUi.demo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

@Singleton
public class VirtualizedPage implements FeaturePage {

    static final int COUNT = 10_000;
    private static final int MAX_VALUE = 10_007;
    private static final int LIST_INDEX = 5000;
    private static final int TABLE_INDEX = 7500;
    private static final PseudoClass HIGH_VALUE = PseudoClass.getPseudoClass("high-value");

    record Item(int id, String name, int value, String category) {
    }

    @Override
    public String id() {
        return "data-virtualized";
    }

    @Override
    public String title() {
        return "Virtualized & transformed lists";
    }

    @Override
    public String category() {
        return Categories.DATA;
    }

    @Override
    public int order() {
        return 50;
    }

    private static String pad(int i) {
        String s = Integer.toString(i);
        return "00000".substring(s.length()) + s;
    }

    static ObservableList<Item> items() {
        List<Item> list = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            list.add(new Item(i, "Item " + pad(i), (int) ((i * 7919L) % MAX_VALUE),
                    String.valueOf("ABCD".charAt((i * 13 + i / 7) % 4))));
        }
        return FXCollections.observableArrayList(list);
    }

    private static Predicate<Item> predicate(String text, String category) {
        String needle = text == null ? "" : text.trim();
        return item -> (category == null || category.equals("All") || item.category().equals(category))
                && item.name().contains(needle);
    }

    private static TableView<Item> itemTable(ObservableList<Item> items, boolean bars) {
        TableView<Item> table = new TableView<>(items);
        TableColumn<Item, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().id()));
        idCol.getStyleClass().add("numeric-cell");
        idCol.setPrefWidth(56);
        TableColumn<Item, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().name()));
        nameCol.setPrefWidth(100);
        TableColumn<Item, Integer> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().value()));
        valueCol.setPrefWidth(bars ? 130 : 70);
        if (bars) {
            valueCol.setCellFactory(column -> new ValueCell());
        } else {
            valueCol.getStyleClass().add("numeric-cell");
        }
        TableColumn<Item, String> categoryCol = new TableColumn<>("Cat.");
        categoryCol.setCellValueFactory(f -> new ReadOnlyObjectWrapper<>(f.getValue().category()));
        categoryCol.getStyleClass().add("center-cell");
        categoryCol.setPrefWidth(44);
        table.getColumns().setAll(List.of(idCol, nameCol, valueCol, categoryCol));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(HIGH_VALUE, !empty && item != null && item.value() >= 9000);
            }
        });
        return table;
    }

    private static String visibleItems(Node virtualized) {
        javafx.scene.control.skin.VirtualFlow<?> flow = DataUi.flow(virtualized);
        return label(flow.getFirstVisibleCell()) + ".." + label(flow.getLastVisibleCell());
    }

    private static String label(IndexedCell<?> cell) {
        Object item = cell == null ? null : cell.getItem();
        return item instanceof Item i ? i.name() : String.valueOf(item);
    }

    private static String cellText(Node virtualized, int index) {
        IndexedCell<?> cell = DataUi.flow(virtualized).getCell(index);
        return cell == null ? "none" : String.valueOf(cell.getItem());
    }

    @Override
    public Node build() {
        ObservableList<Item> items = items();

        // --- 10 000 strings
        List<String> strings = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            strings.add("Row " + pad(i));
        }
        ListView<String> bigList = new ListView<>(FXCollections.observableArrayList(strings));
        bigList.getSelectionModel().select(LIST_INDEX + 2);
        bigList.scrollTo(LIST_INDEX);

        // --- 10 000 rows, fixed cell size
        TableView<Item> bigTable = itemTable(items, true);
        bigTable.setFixedCellSize(22);
        bigTable.getSelectionModel().select(TABLE_INDEX + 3);
        bigTable.scrollTo(TABLE_INDEX);

        // --- FilteredList + SortedList bound to the table comparator
        TextField filterField = new TextField("7");
        filterField.setPrefColumnCount(5);
        ChoiceBox<String> categoryBox = new ChoiceBox<>(FXCollections.observableArrayList("All", "A", "B", "C", "D"));
        categoryBox.setValue("B");
        FilteredList<Item> filtered = new FilteredList<>(items);
        filtered.predicateProperty().bind(Bindings.createObjectBinding(
                () -> predicate(filterField.getText(), categoryBox.getValue()),
                filterField.textProperty(), categoryBox.valueProperty()));
        SortedList<Item> sorted = new SortedList<>(filtered);
        TableView<Item> filterTable = itemTable(sorted, false);
        sorted.comparatorProperty().bind(filterTable.comparatorProperty());
        TableColumn<Item, ?> valueCol = filterTable.getColumns().get(2);
        valueCol.setSortType(TableColumn.SortType.DESCENDING);
        filterTable.getSortOrder().add(valueCol);
        filterTable.getSelectionModel().select(0);

        Label count = new Label();
        count.textProperty().bind(Bindings.createStringBinding(
                () -> Bindings.size(sorted).get() + " of " + items.size(), sorted, items));
        count.getStyleClass().add("data-note");
        HBox filterBar = new HBox(8, new Label("name contains"), filterField, new Label("category"), categoryBox, count);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        VBox filterBox = new VBox(6, filterBar, filterTable);
        VBox.setVgrow(filterTable, Priority.ALWAYS);

        // --- checks
        long expectedFiltered = items.stream().filter(predicate("7", "B")).count();
        Item expectedFirst = items.stream().filter(predicate("7", "B"))
                .max(Comparator.comparingInt(Item::value)).orElseThrow();

        List<Check> sizes = new ArrayList<>();
        sizes.add(Checks.expect("ListView items", COUNT, () -> bigList.getItems().size()));
        sizes.add(Checks.expect("TableView items", COUNT, () -> bigTable.getItems().size()));
        sizes.add(Checks.expect("FilteredList size", (int) expectedFiltered, filtered::size));
        sizes.add(Checks.expect("SortedList size", (int) expectedFiltered, sorted::size));
        sizes.add(Checks.expect("comparator bound to table", true,
                () -> sorted.getComparator() != null && sorted.getComparator() == filterTable.getComparator()));
        sizes.add(Checks.expect("first sorted row", expectedFirst.name() + " = " + expectedFirst.value(),
                () -> filterTable.getItems().getFirst().name() + " = " + filterTable.getItems().getFirst().value()));
        sizes.add(Checks.expect("source index of first row", expectedFirst.id(),
                () -> sorted.getSourceIndexFor(items, 0)));
        sizes.add(Checks.run("sizes by category", () -> {
            StringBuilder sb = new StringBuilder();
            for (String category : List.of("A", "B", "C", "D")) {
                sb.append(category).append('=').append(new FilteredList<>(items, predicate("", category)).size())
                        .append(' ');
            }
            return sb.toString().trim();
        }));
        sizes.add(Checks.expect("source change propagates", "4 -> 5, first=zeta", () -> {
            ObservableList<String> source = FXCollections.observableArrayList("alpha", "beta", "gamma", "delta", "x");
            FilteredList<String> f = new FilteredList<>(source, s -> s.length() > 1);
            SortedList<String> s = new SortedList<>(f, Comparator.reverseOrder());
            int before = s.size();
            source.add("zeta");
            return before + " -> " + s.size() + ", first=" + s.getFirst();
        }));

        DataUi.ChecksHolder left = new DataUi.ChecksHolder("Transformed lists", sizes);
        DataUi.ChecksHolder right = new DataUi.ChecksHolder("VirtualFlow (after scrollTo)", List.of());
        left.setPrefWidth(508);
        right.setPrefWidth(508);

        HBox top = DataUi.row(
                demo("ListView · 10 000 items · scrollTo(" + LIST_INDEX + ")", bigList, 196, 468),
                demo("TableView · 10 000 rows · fixedCellSize 22 · scrollTo(" + TABLE_INDEX + ")", bigTable, 420, 468),
                demo("FilteredList + SortedList bound to the table comparator", filterBox, 388, 468));
        VBox root = DataUi.page(new VBox(12, top, DataUi.row(left, right)));

        CompletionStage<?> ready = Fx.pulses(3)
                .thenRun(() -> {
                    // again, now that the controls are laid out
                    bigList.scrollTo(LIST_INDEX);
                    bigTable.scrollTo(TABLE_INDEX);
                })
                .thenCompose(v -> Fx.pulses(3))
                .thenRun(() -> right.complete(List.of(
                        Checks.expect("ListView first visible", LIST_INDEX,
                                () -> DataUi.index(DataUi.flow(bigList).getFirstVisibleCell())),
                        check("ListView visible range", () -> DataUi.visibleRange(bigList)),
                        Checks.expect("ListView selected cell", "Row 0" + (LIST_INDEX + 2),
                                () -> cellText(bigList, LIST_INDEX + 2)),
                        Checks.expect("TableView first visible", TABLE_INDEX,
                                () -> DataUi.index(DataUi.flow(bigTable).getFirstVisibleCell())),
                        check("TableView visible range", () -> DataUi.visibleRange(bigTable)),
                        Checks.expect("TableView selected", "Item 0" + (TABLE_INDEX + 3),
                                () -> bigTable.getSelectionModel().getSelectedItem().name()),
                        check("filtered visible range", () -> DataUi.visibleRange(filterTable)),
                        check("first / last visible items", () -> visibleItems(bigList) + " · "
                                + visibleItems(bigTable)),
                        check("cell counts", () -> DataUi.flow(bigList).getCellCount() + " / "
                                + DataUi.flow(bigTable).getCellCount() + " / " + DataUi.flow(filterTable).getCellCount()),
                        check("list cell height", () -> String.valueOf(
                                Math.round(DataUi.flow(bigList).getFirstVisibleCell().getHeight()))))));
        DataUi.setReady(root, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return DataUi.ready(content);
    }

    private static final class ValueCell extends TableCell<Item, Integer> {
        private final Rectangle bar = new Rectangle(0, 8);

        ValueCell() {
            bar.setArcWidth(3);
            bar.setArcHeight(3);
            getStyleClass().add("numeric-cell");
            setGraphicTextGap(6);
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(String.valueOf(item));
                bar.setWidth(Math.max(1, 60.0 * item / MAX_VALUE));
                bar.setFill(item >= 9000 ? Color.web("#ef6c00") : Color.web("#42a5f5"));
                setGraphic(bar);
            }
        }
    }
}
