package io.quarkiverse.fx.showcase.pages.data;

import static io.quarkiverse.fx.showcase.pages.data.DataUi.check;
import static io.quarkiverse.fx.showcase.pages.data.DataUi.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.adapter.JavaBeanIntegerProperty;
import javafx.beans.property.adapter.JavaBeanIntegerPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.converter.DefaultStringConverter;

@Singleton
public class TableViewPage implements FeaturePage {

    private static final PseudoClass INACTIVE = PseudoClass.getPseudoClass("inactive");
    private static final String MENU_KEY = "showcase.data.table-menu";
    private static final String MENU_IMAGE_KEY = "showcase.data.table-menu-image";

    @Override
    public String id() {
        return "data-tableview";
    }

    @Override
    public String title() {
        return "TableView";
    }

    @Override
    public String category() {
        return Categories.DATA;
    }

    @Override
    public int order() {
        return 20;
    }

    static ObservableList<Person> loadPeople() {
        return Fx.resourceText("/showcase/data/people.csv").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(Person::parse)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Node build() {
        ObservableList<Person> people = loadPeople();
        Person ada = people.stream().filter(p -> p.getFirstName().equals("Ada")).findFirst().orElseThrow();

        // --- main table
        TableView<Person> table = new TableView<>(people);
        table.setEditable(true);
        table.setTableMenuButtonVisible(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Person, Person> indexCol = new TableColumn<>("#");
        indexCol.setCellValueFactory(features -> new ReadOnlyObjectWrapper<>(features.getValue()));
        indexCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Person item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(getIndex() + 1));
            }
        });
        indexCol.setSortable(false);
        indexCol.setPrefWidth(34);
        indexCol.getStyleClass().add("numeric-cell");

        TableColumn<Person, String> firstCol = new TableColumn<>("First");
        firstCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        firstCol.setCellFactory(TextFieldTableCell.forTableColumn());
        firstCol.setPrefWidth(90);
        TableColumn<Person, String> lastCol = new TableColumn<>("Last");
        lastCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        lastCol.setPrefWidth(100);
        TableColumn<Person, ?> nameCol = new TableColumn<>("Name");
        nameCol.getColumns().addAll(firstCol, lastCol);

        TableColumn<Person, String> fullNameCol = new TableColumn<>("Full name");
        fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        fullNameCol.setVisible(false);

        TableColumn<Person, Integer> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(new PropertyValueFactory<>("age"));
        ageCol.setPrefWidth(56);
        ageCol.getStyleClass().add("numeric-cell");
        ageCol.setSortType(TableColumn.SortType.DESCENDING);

        TableColumn<Person, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(new PropertyValueFactory<>("department"));
        deptCol.setCellFactory(ChoiceBoxTableCell.forTableColumn("Design", "Engineering", "Operations", "Research"));
        deptCol.setPrefWidth(120);

        TableColumn<Person, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        emailCol.setPrefWidth(190);

        TableColumn<Person, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeCol.setCellFactory(CheckBoxTableCell.forTableColumn(activeCol));
        activeCol.setPrefWidth(60);

        TableColumn<Person, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setCellValueFactory(new PropertyValueFactory<>("progress"));
        progressCol.setCellFactory(ProgressBarTableCell.forTableColumn());
        progressCol.setPrefWidth(160);

        table.getColumns().addAll(indexCol, nameCol, fullNameCol, ageCol, deptCol, emailCol, activeCol, progressCol);
        table.setRowFactory(tv -> new TableRow<>() {
            {
                itemProperty().flatMap(Person::activeProperty).map(active -> !active).orElse(false)
                        .subscribe(inactive -> pseudoClassStateChanged(INACTIVE, inactive));
            }
        });

        // sorted by department then age (descending), programmatically
        table.getSortOrder().setAll(deptCol, ageCol);
        table.getSelectionModel().select(1);

        Label selected = new Label();
        selected.textProperty().bind(Bindings.concat("Bindings.selectString(selectedItem, \"lastName\") = ",
                Bindings.selectString(table.getSelectionModel().selectedItemProperty(), "lastName"),
                "   ·   inactive rows use a custom :inactive pseudo class (row factory)"));
        selected.getStyleClass().add("data-note");

        // --- MapValueFactory
        TableView<Map> planets = new TableView<>();
        TableColumn<Map, String> planetCol = new TableColumn<>("Planet");
        planetCol.setCellValueFactory(new MapValueFactory<>("name"));
        TableColumn<Map, Integer> diameterCol = new TableColumn<>("Diameter (km)");
        diameterCol.setCellValueFactory(new MapValueFactory<>("diameter"));
        diameterCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format(Locale.US, "%,d", item));
            }
        });
        diameterCol.getStyleClass().add("numeric-cell");
        TableColumn<Map, Integer> moonsCol = new TableColumn<>("Moons");
        moonsCol.setCellValueFactory(new MapValueFactory<>("moons"));
        moonsCol.getStyleClass().add("numeric-cell");
        planets.getColumns().addAll(planetCol, diameterCol, moonsCol);
        planets.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Object[][] data = { { "Mercury", 4879, 0 }, { "Venus", 12104, 0 }, { "Earth", 12742, 1 }, { "Mars", 6779, 2 },
                { "Jupiter", 139820, 95 }, { "Saturn", 116460, 146 }, { "Uranus", 50724, 28 },
                { "Neptune", 49244, 16 } };
        for (Object[] row : data) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", row[0]);
            map.put("diameter", row[1]);
            map.put("moons", row[2]);
            planets.getItems().add(map);
        }
        planets.getSortOrder().add(diameterCol);

        // --- default placeholders (texts from the JavaFX controls resource bundle)
        TableView<Person> emptyTable = new TableView<>();
        TableColumn<Person, String> e1 = new TableColumn<>("Name");
        TableColumn<Person, String> e2 = new TableColumn<>("Email");
        emptyTable.getColumns().addAll(e1, e2);
        emptyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableView<Person> noColumns = new TableView<>(loadPeople());

        // --- checks
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("rows from people.csv", 14, people::size));
        checks.add(Checks.expect("PropertyValueFactory value", "Ada", () -> firstCol.getCellData(ada)));
        checks.add(Checks.expect("xxxProperty() method used", true,
                () -> (Object) ageCol.getCellObservableValue(ada) == ada.ageProperty()));
        checks.add(Checks.expect("getter fallback (hidden col)", "Ada Lovelace", () -> fullNameCol.getCellData(ada)));
        checks.add(Checks.expect("sort order", "Department ASC, Age DESC",
                () -> table.getSortOrder().stream().map(c -> c.getText() + " "
                        + (c.getSortType() == TableColumn.SortType.ASCENDING ? "ASC" : "DESC"))
                        .collect(Collectors.joining(", "))));
        checks.add(Checks.expect("first row after sort", "Tim Berners-Lee", () -> table.getItems().get(0).getFullName()));
        checks.add(Checks.expect("selected row", "Radia Perlman",
                () -> table.getSelectionModel().getSelectedItem().getFullName()));
        checks.add(Checks.expect("Bindings.selectString", "Perlman",
                () -> Bindings.selectString(table.getSelectionModel().selectedItemProperty(), "lastName").get()));
        checks.add(Checks.expect("CellEditEvent commit", "ada@lovelace.dev", () -> {
            TablePosition<Person, String> position = new TablePosition<>(table, table.getItems().indexOf(ada), emailCol);
            Event.fireEvent(emailCol,
                    new TableColumn.CellEditEvent<>(table, position, TableColumn.editCommitEvent(), "ada@lovelace.dev"));
            return ada.getEmail();
        }));
        checks.add(Checks.expect("JavaBeanStringProperty", "Charles/Babbage", () -> {
            Contact contact = new Contact("Ada", 5);
            JavaBeanStringProperty name = JavaBeanStringPropertyBuilder.create().bean(contact).name("name").build();
            name.set("Charles");
            String first = contact.getName();
            contact.setName("Babbage");
            return first + "/" + name.get();
        }));
        checks.add(Checks.expect("JavaBeanIntegerProperty", 4, () -> {
            Contact contact = new Contact("Ada", 5);
            JavaBeanIntegerProperty rating = JavaBeanIntegerPropertyBuilder.create().bean(contact).name("rating")
                    .build();
            ObservableValue<Number> twice = rating.multiply(2);
            contact.setRating(2);
            return twice.getValue();
        }));
        checks.add(Checks.expect("MapValueFactory value", 1, () -> moonsCol.getCellData(planets.getItems().stream()
                .filter(m -> "Earth".equals(m.get("name"))).findFirst().orElseThrow())));
        DataUi.ChecksHolder holder = new DataUi.ChecksHolder("Checks", checks);
        holder.setPrefWidth(474);

        HBox bottom = DataUi.row(
                demo("MapValueFactory · TableView<Map> sorted by diameter", planets, 330, 300),
                new VBox(10, demo("default placeholder", emptyTable, 200, 145), demo("no columns", noColumns, 200, 145)),
                holder);
        VBox root = DataUi.page(new VBox(8,
                demo("PropertyValueFactory · nested columns · sort Department ↑ Age ↓ · TextField / ChoiceBox (editing) / "
                        + "CheckBox / ProgressBar cells · FLEX_LAST_COLUMN · menu button", table, 1028, 280),
                selected, bottom));

        CompletionStage<?> ready = Fx.pulses(4).thenRun(() -> {
            // TableCell.startEdit() does not request the focus : the ChoiceBoxTableCell of the selected row is
            // shown in its editing state
            table.edit(1, deptCol);
            holder.complete(List.of(
                    check("TableView skin", () -> table.getSkin().getClass().getSimpleName()),
                    check("visible rows", () -> DataUi.visibleRange(table)),
                    check("empty placeholder text", () -> ((Label) emptyTable.lookup(".placeholder .label")).getText()),
                    check("no columns placeholder text",
                            () -> ((Label) noColumns.lookup(".placeholder .label")).getText()),
                    check("resize policy / leaf columns", () -> table.getColumnResizePolicy() + " / "
                            + table.getVisibleLeafColumns().size()),
                    check("column widths", () -> table.getVisibleLeafColumns().stream()
                            .map(c -> String.valueOf(Math.round(c.getWidth()))).collect(Collectors.joining(" "))),
                    Checks.expect("ChoiceBoxTableCell editing", "ChoiceBox = Design", () -> {
                        TableCell<?, ?> cell = tableCell(table, 1, deptCol);
                        return cell.isEditing() && cell.getGraphic() instanceof ChoiceBox<?> choiceBox
                                ? "ChoiceBox = " + choiceBox.getValue()
                                : "not editing, graphic " + cell.getGraphic();
                    })));
        }).thenCompose(v -> ShowcaseMode.snapshot() ? columnMenu(table, root, holder)
                : java.util.concurrent.CompletableFuture.completedFuture(null));
        DataUi.setReady(root, ready);
        return root;
    }

    /**
     * Opens the table menu (the "+" button) as a click would, snapshots the popup content, then closes it.
     */
    private static CompletionStage<?> columnMenu(TableView<?> table, Node root, DataUi.ChecksHolder holder) {
        ContextMenu menu;
        try {
            Node button = table.lookup(".show-hide-columns-button");
            Event.fireEvent(button, new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                    false, false, false, false, true, false, false, false, false, false, null));
            menu = Window.getWindows().stream()
                    .filter(w -> w instanceof ContextMenu m && m.getOwnerNode() == button && m.isShowing())
                    .map(ContextMenu.class::cast).findFirst()
                    .orElseThrow(() -> new IllegalStateException("table menu not showing"));
            // whatever the mouse position, no hover in the snapshot
            menu.getScene().getRoot().setMouseTransparent(true);
            // otherwise, the popup scene gives the initial focus to the first item on the next pulse, which is
            // highlighted only when the application is active (the popup mirrors the focus of its owner window)
            menu.getSkin().getNode().requestFocus();
            root.getProperties().put(MENU_KEY, menu);
        } catch (Throwable t) {
            holder.complete(List.of(Check.fail("table menu items", Checks.describe(t))));
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        return Fx.pulses(3).thenRun(() -> {
            try {
                root.getProperties().put(MENU_IMAGE_KEY, menu.getScene().getRoot().snapshot(null, null));
            } finally {
                // nested column items are named "parent<separator>child", the separator coming from the
                // ControlResources bundle
                holder.complete(List.of(check("table menu items", () -> menu.getItems().size() + " items, 2nd \""
                        + menu.getItems().get(1).getText() + "\", unchecked " + menu.getItems().stream()
                                .filter(item -> item instanceof CheckMenuItem c && !c.isSelected())
                                .map(MenuItem::getText).toList())));
                menu.hide();
            }
        });
    }

    @Override
    public CompletionStage<Map<String, Image>> extraSnapshots(Node content) {
        Object image = content.getProperties().get(MENU_IMAGE_KEY);
        return java.util.concurrent.CompletableFuture
                .completedFuture(image instanceof Image i ? Map.of("table-menu", i) : Map.of());
    }

    @Override
    public void dispose(Node content) {
        if (content.getProperties().get(MENU_KEY) instanceof ContextMenu menu) {
            menu.hide();
        }
    }

    private static TableCell<?, ?> tableCell(TableView<?> table, int row, TableColumn<?, ?> column) {
        for (Node node : table.lookupAll(".table-cell")) {
            if (node instanceof TableCell<?, ?> cell && cell.getIndex() == row && cell.getTableColumn() == column
                    && cell.isVisible()) {
                return cell;
            }
        }
        throw new IllegalStateException("No cell at " + row + " / " + column.getText());
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return DataUi.ready(content);
    }
}
