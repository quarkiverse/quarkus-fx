package io.quarkiverse.fx.showcase.pages.controls;

import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.caption;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.demo;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.grow;

import java.time.LocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.IsoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.layout.GridPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.PopupWindow;
import javafx.util.StringConverter;

@Singleton
public class PickersPage implements FeaturePage {

    record Fruit(String name, Color color, int calories) {
    }

    private static final List<Fruit> FRUITS = List.of(
            new Fruit("Apple", Color.web("#c62828"), 52),
            new Fruit("Banana", Color.web("#f9a825"), 89),
            new Fruit("Blueberry", Color.web("#3949ab"), 57),
            new Fruit("Kiwi", Color.web("#7cb342"), 61),
            new Fruit("Orange", Color.web("#ef6c00"), 47));

    private static final LocalDate ISO_DATE = LocalDate.of(2024, 2, 29);
    private static final LocalDate HIJRAH_DATE = LocalDate.of(2024, 3, 11);
    private static final LocalDate THAI_DATE = LocalDate.of(2019, 10, 13);

    @Override
    public String id() {
        return "controls-pickers";
    }

    @Override
    public String title() {
        return "Choices, Pickers & Progress";
    }

    @Override
    public String category() {
        return Categories.CONTROLS;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Node build() {
        ControlsUi.ChecksHolder checks = new ControlsUi.ChecksHolder();
        List<Check> early = checks.early;

        StringConverter<Fruit> fruitConverter = new StringConverter<>() {
            @Override
            public String toString(Fruit fruit) {
                return fruit == null ? "" : fruit.name() + " (" + fruit.calories() + " kcal)";
            }

            @Override
            public Fruit fromString(String string) {
                return FRUITS.stream().filter(f -> string != null && string.startsWith(f.name())).findFirst()
                        .orElse(null);
            }
        };

        // ChoiceBox
        ChoiceBox<Fruit> choice = new ChoiceBox<>();
        choice.getItems().setAll(FRUITS);
        choice.setConverter(fruitConverter);
        choice.getSelectionModel().select(2);
        ChoiceBox<String> plainChoice = new ChoiceBox<>();
        plainChoice.getItems().addAll("First", "Second", "Third");
        plainChoice.setValue("Second");
        ChoiceBox<String> emptyChoice = new ChoiceBox<>();
        emptyChoice.getItems().addAll("One", "Two");
        early.add(Checks.expect("ChoiceBox converter / selection", "Blueberry (57 kcal) / 2",
                () -> choice.getConverter().toString(choice.getValue()) + " / "
                        + choice.getSelectionModel().getSelectedIndex()));

        // ComboBox
        ComboBox<String> editable = new ComboBox<>();
        editable.getItems().addAll("Quarkus", "JavaFX", "GraalVM");
        editable.setEditable(true);
        editable.setValue("JavaFX");
        editable.setPromptText("Type or pick");
        ComboBox<String> promptCombo = new ComboBox<>();
        promptCombo.getItems().addAll("Quarkus", "JavaFX");
        promptCombo.setPromptText("Prompt text");
        ComboBox<Fruit> cells = new ComboBox<>();
        cells.getItems().setAll(FRUITS);
        cells.setCellFactory(list -> new FruitCell());
        cells.setButtonCell(new FruitCell());
        cells.getSelectionModel().select(3);
        cells.setPrefWidth(180);
        cells.setId("fruit-combo");
        early.add(Checks.expect("editable ComboBox commitValue", "GraalVM", () -> {
            ComboBox<String> combo = new ComboBox<>();
            combo.setEditable(true);
            combo.getEditor().setText("GraalVM");
            combo.commitValue();
            return combo.getValue();
        }));
        early.add(Checks.expect("ComboBox converter fromString", "Kiwi",
                () -> fruitConverter.fromString("Kiwi (61 kcal)").name()));

        // ColorPickers
        // none of the fixed colors is in the palette : no square is selected (hovered) when the palette shows
        ColorPicker colorPicker = new ColorPicker(Color.web("#1e88e5"));
        colorPicker.setId("color-picker");
        colorPicker.getCustomColors().addAll(Color.web("#ff8a65"), Color.web("#4db6ac"), Color.web("#9575cd"));
        ColorPicker buttonPicker = new ColorPicker(Color.web("#43a047"));
        buttonPicker.getStyleClass().add(ColorPicker.STYLE_CLASS_BUTTON);
        ColorPicker splitPicker = new ColorPicker(Color.web("#e53935"));
        splitPicker.getStyleClass().add(ColorPicker.STYLE_CLASS_SPLIT_BUTTON);
        early.add(Checks.expect("ColorPicker value", "0x1e88e5ff", () -> colorPicker.getValue().toString()));

        // DatePickers with their calendar rendered inline (DatePickerSkin.getPopupContent)
        VBox iso = datePicker("DatePicker ISO, week numbers", () -> IsoChronology.INSTANCE, ISO_DATE, true, early);
        VBox hijrah = datePicker("HijrahChronology, week numbers", () -> HijrahChronology.INSTANCE, HIJRAH_DATE, true,
                early);
        VBox thai = datePicker("ThaiBuddhistChronology", () -> ThaiBuddhistChronology.INSTANCE, THAI_DATE, false,
                early);
        // the DatePicker texts, the first day of the week and the week numbers follow the default FORMAT locale
        early.add(Check.info("default FORMAT locale / first day of week", Locale.getDefault(Locale.Category.FORMAT)
                .toLanguageTag() + " / " + WeekFields.of(Locale.getDefault(Locale.Category.FORMAT)).getFirstDayOfWeek()));
        early.add(Checks.expect("HijrahDate.from(2024-03-11)", "Hijrah-umalqura AH 1445-09-01",
                () -> HijrahDate.from(HIJRAH_DATE).toString()));
        early.add(Checks.expect("ThaiBuddhistDate.from(2019-10-13)", "ThaiBuddhist BE 2562-10-13",
                () -> ThaiBuddhistDate.from(THAI_DATE).toString()));
        early.add(Checks.run("DatePicker converters (ISO / Hijrah / Thai)", () -> List.of(iso, hijrah, thai).stream()
                .map(box -> {
                    DatePicker picker = (DatePicker) box.getProperties().get("picker");
                    return picker == null ? "(failed)" : picker.getConverter().toString(picker.getValue());
                }).collect(Collectors.joining(" | "))));
        early.add(Checks.expect("DatePicker converter fromString (ISO)", ISO_DATE, () -> {
            DatePicker picker = new DatePicker(ISO_DATE);
            return picker.getConverter().fromString(picker.getConverter().toString(ISO_DATE));
        }));

        // Pagination
        Pagination pagination = new Pagination(10, 3);
        pagination.setPageFactory(index -> {
            Label page = new Label("Page " + (index + 1));
            page.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #455a64;");
            StackPane pane = new StackPane(page);
            pane.setStyle("-fx-background-color: #eceff1;");
            pane.setPrefSize(180, 40);
            return pane;
        });
        pagination.setPrefSize(330, 110);
        Pagination bullets = new Pagination(6, 1);
        bullets.getStyleClass().add(Pagination.STYLE_CLASS_BULLET);
        bullets.setPageFactory(index -> new Label("Bullet page " + (index + 1)));
        bullets.setPrefSize(330, 80);
        early.add(Checks.expect("Pagination count / index", "10 / 3",
                () -> pagination.getPageCount() + " / " + pagination.getCurrentPageIndex()));

        // Progress
        GridPane progress = new GridPane(10, 6);
        progress.setAlignment(Pos.CENTER_LEFT);
        double[] values = { 0, 0.35, 1.0 };
        for (int i = 0; i < values.length; i++) {
            ProgressBar bar = new ProgressBar(values[i]);
            bar.setPrefWidth(120);
            ProgressIndicator indicator = new ProgressIndicator(values[i]);
            indicator.setId("indicator-" + i);
            indicator.setPrefSize(46, 46);
            progress.addRow(i, caption(String.valueOf(values[i])), bar, indicator);
        }
        if (!ShowcaseMode.snapshot()) {
            ProgressBar bar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
            bar.setPrefWidth(120);
            ProgressIndicator indicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
            indicator.setPrefSize(40, 40);
            progress.addRow(3, caption("indeterminate"), bar, indicator);
        }
        early.add(Checks.expect("ProgressIndicator.isIndeterminate (-1 / 0.35)", "true / false",
                () -> new ProgressIndicator(-1).isIndeterminate() + " / " + new ProgressIndicator(0.35)
                        .isIndeterminate()));

        VBox choices = demo("ChoiceBox : converter, plain, empty", new HBox(8, choice, plainChoice, emptyChoice));
        VBox combos = demo("ComboBox : editable, prompt, cell factory + button cell",
                new HBox(8, editable, promptCombo, cells));
        HBox row1 = new HBox(8, choices, grow(combos));

        VBox colors = demo("ColorPicker : combo, button, split-button", colorPicker, buttonPicker, splitPicker);
        VBox progresses = demo("ProgressBar / ProgressIndicator", progress);
        VBox side = new VBox(8, colors, progresses);
        HBox.setHgrow(side, Priority.ALWAYS);
        HBox row2 = new HBox(8, iso, hijrah, thai, side);

        VBox paginations = demo("Pagination : numeric (page 4 of 10), bullet style", pagination, bullets);
        paginations.setMinWidth(Region.USE_PREF_SIZE);
        HBox row3 = new HBox(10, paginations, grow(checks));

        VBox root = ControlsUi.page(8, row1, row2, row3, ControlsUi.hiddenChecks());
        root.setPrefWidth(1028);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Fx.pulses(2).thenRunAsync(() -> {
            ControlsUi.ChecksHolder holder = ControlsUi.find(content, ControlsUi.ChecksHolder.class);
            List<Check> late = new ArrayList<>();
            late.add(Checks.run("ProgressIndicator texts (0 / 0.35 / 1.0)", () -> {
                List<String> texts = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    Node text = content.lookup("#indicator-" + i).lookup(".percentage");
                    texts.add(text instanceof javafx.scene.text.Text t ? t.getText() : ControlsUi.describe(text));
                }
                return String.join(" / ", texts);
            }));
            late.add(Checks.run("DatePicker week numbers (ISO calendar)", () -> content.lookupAll(".week-number-cell")
                    .stream()
                    .filter(Node::isVisible)
                    .map(n -> ((javafx.scene.control.Labeled) n).getText())
                    .limit(6)
                    .collect(Collectors.joining(" "))));
            holder.show("Checks", late);
        }, Fx.FX_THREAD);
    }

    /**
     * A DatePicker and its calendar rendered inline. A chronology that cannot be used (e.g. missing calendar data) is
     * reported as a failed check instead of failing the whole page.
     */
    private static VBox datePicker(String caption, Supplier<Chronology> chronology, LocalDate date,
            boolean weekNumbers, List<Check> checks) {
        try {
            DatePicker picker = new DatePicker(date);
            picker.setChronology(chronology.get());
            picker.setShowWeekNumbers(weekNumbers);
            picker.setPrefWidth(120);
            picker.setMaxWidth(Double.MAX_VALUE);
            DatePickerSkin skin = new DatePickerSkin(picker);
            picker.setSkin(skin);
            Region calendar = (Region) skin.getPopupContent();
            calendar.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            VBox box = demo(caption + " : " + date, picker, calendar);
            box.setMinWidth(Region.USE_PREF_SIZE);
            box.getProperties().put("picker", picker);
            return box;
        } catch (Throwable t) {
            checks.add(Check.fail(caption, Checks.describe(t)));
            Label error = new Label(Checks.describe(t));
            error.getStyleClass().add("check-fail");
            error.setWrapText(true);
            error.setPrefWidth(220);
            VBox box = demo(caption + " : " + date, error);
            box.setMinWidth(Region.USE_PREF_SIZE);
            return box;
        }
    }

    @Override
    public CompletionStage<Map<String, Image>> extraSnapshots(Node content) {
        ComboBox<?> combo = (ComboBox<?>) content.lookup("#fruit-combo");
        ColorPicker picker = (ColorPicker) content.lookup("#color-picker");
        Scene scene = content.getScene();
        Node previousFocus = scene.getFocusOwner();
        Map<String, Image> images = new LinkedHashMap<>();
        List<Check> popupChecks = new ArrayList<>();
        ControlsUi.PopupGuard guard = new ControlsUi.PopupGuard().install();

        CompletionStage<Map<String, Image>> result = CompletableFuture.completedFuture((Void) null)
                .thenRunAsync(() -> show(combo::show, guard), Fx.FX_THREAD)
                .thenCompose(v -> Fx.pulses(3))
                .thenRunAsync(() -> {
                    PopupWindow popup = guard.lastShowing();
                    popupChecks.add(Checks.expect("ComboBox popup shown", true, () -> popup != null));
                    if (popup != null) {
                        Parent root = popup.getScene().getRoot();
                        images.put("combo-popup", root.snapshot(null, null));
                        popupChecks.add(Checks.run("ComboBox popup cells (cell factory)", () -> cellTexts(root)));
                    }
                    combo.hide();
                    show(picker::show, guard);
                }, Fx.FX_THREAD)
                .thenCompose(v -> Fx.pulses(3))
                .thenApplyAsync(v -> {
                    PopupWindow popup = guard.lastShowing();
                    popupChecks.add(Checks.expect("ColorPicker palette shown", true, () -> popup != null));
                    if (popup != null) {
                        Parent root = popup.getScene().getRoot();
                        images.put("color-palette", root.snapshot(null, null));
                        popupChecks.add(Checks.run("ColorPalette squares", () -> root.lookupAll(".color-square").size()));
                        // "Custom Color..." comes from the JavaFX controls resource bundle
                        popupChecks.add(Checks.run("ColorPalette texts", () -> labeledTexts(root, new ArrayList<>())));
                    }
                    return images;
                }, Fx.FX_THREAD);

        return result.whenCompleteAsync((ignored, error) -> {
            combo.hide();
            picker.hide();
            guard.uninstall();
            if (previousFocus != null && previousFocus.getScene() == scene) {
                previousFocus.requestFocus();
            }
            Pane hidden = ControlsUi.find(content, Pane.class, "hidden-checks");
            if (hidden != null) {
                Checks.attach(hidden, popupChecks);
            }
        }, Fx.FX_THREAD);
    }

    @Override
    public void dispose(Node content) {
        if (content.lookup("#fruit-combo") instanceof ComboBox<?> combo) {
            combo.hide();
        }
        if (content.lookup("#color-picker") instanceof ColorPicker picker) {
            picker.hide();
        }
    }

    /**
     * Shows a popup, then takes the focus away from its content (requested by the skin once shown).
     */
    private static void show(Runnable show, ControlsUi.PopupGuard guard) {
        show.run();
        PopupWindow popup = guard.lastShowing();
        if (popup != null) {
            ControlsUi.PopupGuard.calm(popup);
        }
    }

    private static String cellTexts(Parent root) {
        Map<Integer, String> texts = new TreeMap<>();
        for (Node node : root.lookupAll(".list-cell")) {
            if (node instanceof IndexedCell<?> cell && cell.getIndex() >= 0 && !cell.isEmpty()) {
                texts.putIfAbsent(cell.getIndex(), cell.getText());
            }
        }
        return String.join(", ", texts.values());
    }

    private static String labeledTexts(Node node, List<String> texts) {
        if (node instanceof Labeled labeled && labeled.getText() != null && !labeled.getText().isBlank()) {
            texts.add(labeled.getText());
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                labeledTexts(child, texts);
            }
        }
        return String.join(", ", texts);
    }


    private static final class FruitCell extends ListCell<Fruit> {
        @Override
        protected void updateItem(Fruit item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.name() + " · " + String.format(Locale.ROOT, "%d kcal", item.calories()));
                setGraphic(new Circle(6, item.color()));
            }
        }
    }
}
