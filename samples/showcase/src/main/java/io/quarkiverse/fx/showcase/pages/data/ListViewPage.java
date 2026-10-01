package io.quarkiverse.fx.showcase.pages.data;

import static io.quarkiverse.fx.showcase.pages.data.DataUi.check;
import static io.quarkiverse.fx.showcase.pages.data.DataUi.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.ChoiceBoxListCell;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

@Singleton
public class ListViewPage implements FeaturePage {

    private static final double COL = 196;

    record Swatch(String name, Color color) {
        String hex() {
            return String.format(Locale.ROOT, "#%02X%02X%02X", Math.round(color.getRed() * 255),
                    Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
        }
    }

    @Override
    public String id() {
        return "data-listview";
    }

    @Override
    public String title() {
        return "ListView";
    }

    @Override
    public String category() {
        return Categories.DATA;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        // 1. plain strings, one selected
        ListView<String> strings = new ListView<>(FXCollections.observableArrayList("Apple", "Apricot", "Banana",
                "Blueberry", "Cherry", "Date", "Fig", "Grape", "Kiwi", "Lemon", "Mango", "Orange", "Peach", "Pear", "Plum"));
        strings.getSelectionModel().select("Cherry");

        // 2. custom cells with graphics
        ListView<Swatch> swatches = new ListView<>();
        for (String name : List.of("crimson", "coral", "gold", "olivedrab", "teal", "steelblue", "indigo", "orchid",
                "slategray", "sienna")) {
            swatches.getItems().add(new Swatch(name, Color.web(name)));
        }
        swatches.setCellFactory(list -> new SwatchCell());
        swatches.getSelectionModel().select(5);

        // 3. multiple selection
        ListView<String> multiple = new ListView<>();
        for (int i = 1; i <= 12; i++) {
            multiple.getItems().add("Item " + i);
        }
        multiple.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        multiple.getSelectionModel().selectIndices(1, 3, 4, 7);
        Label selectedIndices = new Label();
        selectedIndices.textProperty().bind(Bindings.createStringBinding(
                () -> "indices " + multiple.getSelectionModel().getSelectedIndices(),
                multiple.getSelectionModel().getSelectedIndices()));
        VBox multipleBox = new VBox(4, multiple, selectedIndices);
        VBox.setVgrow(multiple, Priority.ALWAYS);

        // 4. empty with a placeholder
        ListView<String> empty = new ListView<>();
        Label placeholderText = new Label("No items to display");
        placeholderText.getStyleClass().add("placeholder-text");
        Circle placeholderIcon = new Circle(14, Color.web("#cfd8dc"));
        VBox placeholder = new VBox(8, placeholderIcon, placeholderText);
        placeholder.setAlignment(Pos.CENTER);
        empty.setPlaceholder(placeholder);

        // 5. CheckBoxListCell
        Map<String, BooleanProperty> done = new LinkedHashMap<>();
        for (String task : List.of("Write pages", "Build JVM", "Snapshot", "Build native", "Compare", "Report",
                "Fix config", "Celebrate")) {
            done.put(task, new SimpleBooleanProperty(done.size() < 3));
        }
        ListView<String> checkList = new ListView<>(FXCollections.observableArrayList(done.keySet()));
        checkList.setCellFactory(CheckBoxListCell.forListView(done::get));

        // 6. horizontal orientation
        ListView<Integer> horizontal = new ListView<>();
        for (int i = 1; i <= 24; i++) {
            horizontal.getItems().add(i);
        }
        horizontal.setOrientation(Orientation.HORIZONTAL);
        horizontal.setCellFactory(list -> new TileCell());
        horizontal.getSelectionModel().select(2);

        // 7. TextFieldListCell
        ListView<String> textFields = new ListView<>(
                FXCollections.observableArrayList("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf"));
        textFields.setEditable(true);
        textFields.setCellFactory(TextFieldListCell.forListView());

        // 8. ChoiceBoxListCell
        ObservableList<String> sizes = FXCollections.observableArrayList("Small", "Medium", "Large", "X-Large");
        ListView<String> choices = new ListView<>(
                FXCollections.observableArrayList("Small", "Large", "Medium", "Medium", "X-Large", "Small"));
        choices.setEditable(true);
        choices.setCellFactory(ChoiceBoxListCell.forListView(sizes));

        // 9. ComboBoxListCell
        ObservableList<String> priorities = FXCollections.observableArrayList("Low", "Normal", "High", "Critical");
        ListView<String> combos = new ListView<>(
                FXCollections.observableArrayList("High", "Low", "Normal", "Critical", "Normal", "Low"));
        combos.setEditable(true);
        combos.setCellFactory(ComboBoxListCell.forListView(priorities));

        // 10. styled through CSS only, including its skin (-fx-skin, loaded reflectively) and -fx-fixed-cell-size
        ListView<String> fancy = new ListView<>(FXCollections.observableArrayList("Mercury", "Venus", "Earth", "Mars",
                "Jupiter", "Saturn", "Uranus", "Neptune"));
        fancy.getStyleClass().add("fancy-list");
        fancy.getSelectionModel().select(2);

        // 11. disabled
        ListView<String> disabled = new ListView<>(
                FXCollections.observableArrayList("One", "Two", "Three", "Four", "Five", "Six"));
        disabled.getSelectionModel().select(1);
        disabled.setDisable(true);

        // edits, committed through edit events (the default commit handlers update the items)
        List<Check> selection = new ArrayList<>();
        selection.add(Checks.expect("single selection", "Cherry", () -> strings.getSelectionModel().getSelectedItem()));
        selection.add(Checks.expect("single selected index", 4, () -> strings.getSelectionModel().getSelectedIndex()));
        selection.add(Checks.expect("multiple selected indices", "[1, 3, 4, 7]",
                () -> multiple.getSelectionModel().getSelectedIndices().toString()));
        selection.add(Checks.expect("multiple selected items", "[Item 2, Item 4, Item 5, Item 8]",
                () -> multiple.getSelectionModel().getSelectedItems().toString()));
        selection.add(Checks.expect("TextFieldListCell commit", "Bravo (edited)", () -> {
            textFields.fireEvent(new ListView.EditEvent<>(textFields, ListView.editCommitEvent(), "Bravo (edited)", 1));
            return textFields.getItems().get(1);
        }));
        selection.add(Checks.expect("ChoiceBoxListCell commit", "X-Large", () -> {
            choices.fireEvent(new ListView.EditEvent<>(choices, ListView.editCommitEvent(), "X-Large", 0));
            return choices.getItems().get(0);
        }));
        selection.add(Checks.expect("CheckBoxListCell checked", 3,
                () -> (int) done.values().stream().filter(BooleanProperty::get).count()));
        selection.add(Checks.expect("horizontal orientation", Orientation.HORIZONTAL, horizontal::getOrientation));
        selection.add(Checks.expect("custom cell hex", "#4682B4", () -> swatches.getItems().get(5).hex()));

        DataUi.ChecksHolder selectionChecks = new DataUi.ChecksHolder("Selection & editing", selection);
        DataUi.ChecksHolder cellChecks = new DataUi.ChecksHolder("Cells & skins (after layout)", List.of());
        selectionChecks.setPrefWidth(508);
        cellChecks.setPrefWidth(508);

        HBox rowA = DataUi.row(
                demo("ListView<String> · selected", strings, COL, 192),
                demo("custom cells (graphics)", swatches, COL, 192),
                demo("SelectionMode.MULTIPLE", multipleBox, COL, 192),
                demo("empty · placeholder", empty, COL, 192),
                demo("CheckBoxListCell", checkList, COL, 192));
        VBox rowB = demo("Orientation.HORIZONTAL · custom tile cells", horizontal, 1028, 84);
        HBox rowC = DataUi.row(
                demo("TextFieldListCell · row 4 editing", textFields, COL, 160),
                demo("ChoiceBoxListCell · row 3 editing", choices, COL, 160),
                demo("ComboBoxListCell · row 2 editing", combos, COL, 160),
                demo("CSS only · -fx-skin · fixed cell size", fancy, COL, 160),
                demo("disabled", disabled, COL, 160));
        HBox rowD = DataUi.row(selectionChecks, cellChecks);

        VBox root = DataUi.page(new VBox(8, rowA, rowB, rowC, rowD));

        CompletionStage<?> ready = Fx.pulses(4).thenRun(() -> {
            List<Check> cells = new ArrayList<>();
            cells.add(check("ListView skin", () -> strings.getSkin().getClass().getName()));
            cells.add(check("visible rows (strings)", () -> DataUi.visibleRange(strings)));
            cells.add(check("visible rows (horizontal)", () -> DataUi.visibleRange(horizontal)));
            cells.add(check("TextField cell", () -> DataUi.cell(textFields, ".list-cell", 0).getClass().getSimpleName()));
            cells.add(check("ChoiceBox cell", () -> DataUi.cell(choices, ".list-cell", 0).getClass().getSimpleName()));
            cells.add(check("ComboBox cell", () -> DataUi.cell(combos, ".list-cell", 0).getClass().getSimpleName()));
            cells.add(check("CheckBox cell", () -> DataUi.cell(checkList, ".list-cell", 0).getClass().getSimpleName()));
            cells.add(check("custom cell graphic", () -> {
                ListCell<?> cell = DataUi.cell(swatches, ".list-cell", 0);
                return cell.getGraphic() == null ? "none" : cell.getGraphic().getClass().getSimpleName();
            }));
            cells.add(check("placeholder shown", () -> placeholder.getScene() != null && placeholder.isVisible()));
            cells.add(check("selected cell pseudo classes", () -> DataUi.cell(fancy, ".list-cell", 2)
                    .getPseudoClassStates().stream().map(Object::toString).filter(c -> !c.startsWith("nth-"))
                    .sorted().collect(java.util.stream.Collectors.joining(" "))));
            cells.add(Checks.expect("-fx-skin (CSS, reflective)", FooterListViewSkin.class.getSimpleName(),
                    () -> fancy.getSkin().getClass().getSimpleName()));
            cells.add(Checks.expect("-fx-fixed-cell-size (CSS)", "26 / 26",
                    () -> Math.round(fancy.getFixedCellSize()) + " / "
                            + Math.round(DataUi.cell(fancy, ".list-cell", 0).getHeight())));

            // editing state of the TextField / ChoiceBox / ComboBox cells: ListCell.startEdit() requests the focus
            // on the ListView (and TextFieldListCell on its TextField), which invisible nodes cannot receive, so the
            // focus owner is left unchanged (no caret, no focused colors)
            Scene scene = root.getScene();
            Node focusOwner = scene == null ? null : scene.getFocusOwner();
            editWithoutFocus(textFields, 3);
            editWithoutFocus(choices, 2);
            editWithoutFocus(combos, 1);
            List<Check> editing = new ArrayList<>();
            editing.add(Checks.expect("TextFieldListCell editing", "TextField = Delta", () -> editor(textFields, 3)));
            editing.add(Checks.expect("ChoiceBoxListCell editing", "ChoiceBox = Medium", () -> editor(choices, 2)));
            editing.add(Checks.expect("ComboBoxListCell editing", "ComboBox = Low", () -> editor(combos, 1)));
            editing.add(Checks.expect("focus owner unchanged by edit", true,
                    () -> scene != null && scene.getFocusOwner() == focusOwner));
            selectionChecks.complete(editing);
            cellChecks.complete(cells);
        });
        DataUi.setReady(root, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return DataUi.ready(content);
    }

    private static void editWithoutFocus(ListView<?> list, int index) {
        boolean visible = list.isVisible();
        list.setVisible(false);
        try {
            list.edit(index);
        } finally {
            list.setVisible(visible);
        }
    }

    /**
     * "EditorClass = value" of the editing cell at {@code index}.
     */
    private static String editor(ListView<?> list, int index) {
        ListCell<?> cell = DataUi.cell(list, ".list-cell", index);
        if (!cell.isEditing()) {
            return "not editing (editing index " + list.getEditingIndex() + ")";
        }
        Node graphic = cell.getGraphic();
        Object value = graphic instanceof javafx.scene.control.TextField textField ? textField.getText()
                : graphic instanceof javafx.scene.control.ChoiceBox<?> choiceBox ? choiceBox.getValue()
                : graphic instanceof javafx.scene.control.ComboBox<?> comboBox ? comboBox.getValue() : null;
        return (graphic == null ? "no graphic" : graphic.getClass().getSimpleName()) + " = " + value;
    }

    private static final class SwatchCell extends ListCell<Swatch> {
        private final Rectangle chip = new Rectangle(14, 14);
        private final Label name = new Label();
        private final Label hex = new Label();
        private final HBox box;

        SwatchCell() {
            chip.setArcWidth(4);
            chip.setArcHeight(4);
            chip.setStroke(Color.web("#00000040"));
            hex.getStyleClass().add("swatch-hex");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            box = new HBox(6, chip, name, spacer, hex);
            box.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Swatch item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                chip.setFill(item.color());
                name.setText(item.name());
                hex.setText(item.hex());
                setGraphic(box);
            }
        }
    }

    private static final class TileCell extends ListCell<Integer> {
        private final Rectangle tile = new Rectangle(34, 34);
        private final Text number = new Text();
        private final StackPane box = new StackPane(tile, number);

        TileCell() {
            tile.setArcWidth(10);
            tile.setArcHeight(10);
            number.getStyleClass().add("tile-text");
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                tile.setFill(Color.hsb((item * 15) % 360, 0.65, 0.85));
                number.setText(String.valueOf(item));
                setGraphic(box);
            }
        }
    }
}
