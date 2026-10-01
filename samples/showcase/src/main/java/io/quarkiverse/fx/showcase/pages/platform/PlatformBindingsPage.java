package io.quarkiverse.fx.showcase.pages.platform;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.beans.property.adapter.ReadOnlyJavaBeanIntegerProperty;
import javafx.beans.property.adapter.ReadOnlyJavaBeanIntegerPropertyBuilder;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableIntegerArray;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;
import javafx.util.Subscription;

/**
 * javafx.beans and javafx.collections : select bindings and JavaBean adapters on application classes (reflection),
 * transformation lists, change listeners, fluent bindings and subscriptions, weak listeners.
 */
@Singleton
public class PlatformBindingsPage implements FeaturePage {

    /** List element with an observable name, for the extractor based "updated" changes. */
    private static final class Item {
        final StringProperty name;

        Item(String name) {
            this.name = new SimpleStringProperty(name);
        }

        @Override
        public String toString() {
            return name.get();
        }
    }

    @Override
    public String id() {
        return "platform-bindings";
    }

    @Override
    public String title() {
        return "Bindings, Collections & Listeners";
    }

    @Override
    public String category() {
        return Categories.PLATFORM;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Node build() {
        double half = PlatformUi.HALF_WIDTH;
        List<Check> checks = new ArrayList<>();
        VBox left = new VBox(8, select(checks), adapters(checks), collections(checks), listeners(checks));
        VBox right = new VBox(8, boundControls(checks), fluent(checks));
        right.getChildren().add(PlatformUi.checks("Checks", checks, 190, half));
        PlatformUi.width(left, half);
        PlatformUi.width(right, half);
        return PlatformUi.page(0, new HBox(12, left, right));
    }

    // ------------------------------------------------------------------ Bindings.select

    private VBox select(List<Check> checks) {
        Customer customer = new Customer("Ada", new Address("London", 1000));
        // an observable root : the steps are resolved through the xxxProperty() methods of the JavaFX beans
        ObjectProperty<Customer> root = new SimpleObjectProperty<>(customer);
        StringBinding city = Bindings.selectString(root, "address", "city");
        IntegerBinding zip = Bindings.selectInteger(root, "address", "zip");
        // a plain object root : the first step goes through a JavaBean adapter on getName(), created by
        // com.sun.javafx.property.JavaBeanAccessHelper which loads JavaBeanQuickAccessor reflectively. It throws
        // "Java beans are not supported" when that fails : record it as a check instead of breaking the page.
        ObjectBinding<Object> name;
        try {
            name = Bindings.select(customer, "name");
        } catch (RuntimeException e) {
            checks.add(Check.fail("Bindings.select(POJO root, name)", Checks.describe(e)));
            name = null;
        }
        ObjectBinding<Object> pojoName = name;
        // a PropertyChangeSupport bean as root : the select binding follows its bound property events
        LegacyAccount legacy = new LegacyAccount("Duke", 7);
        List<String> legacyOwners = new ArrayList<>();
        try {
            StringBinding owner = Bindings.selectString(legacy, "owner");
            legacyOwners.add(owner.get());
            legacy.setOwner("Tux");
            legacyOwners.add(owner.get());
            checks.add(Checks.expect("selectString(JavaBean root, owner)", "Duke > Tux",
                    () -> String.join(" > ", legacyOwners)));
        } catch (RuntimeException e) {
            checks.add(Check.fail("selectString(JavaBean root, owner)", Checks.describe(e)));
        }
        ObservableValue<String> fluent = customer.addressProperty().flatMap(Address::cityProperty).orElse("(no city)");
        List<String> selected = new ArrayList<>();
        List<String> flat = new ArrayList<>();
        // keep the fluent binding observed : it is lazy
        fluent.addListener((observable, oldValue, newValue) -> {
        });
        Runnable record = () -> {
            selected.add("'" + city.get() + "'");
            flat.add(fluent.getValue());
        };
        record.run();
        Address paris = new Address("Paris", 75000);
        customer.setAddress(paris);
        record.run();
        paris.setCity("Lyon");
        record.run();
        customer.setAddress(null);
        record.run();
        customer.setAddress(new Address("Oslo", 150));
        record.run();

        checks.add(Checks.expect("Bindings.selectString(address, city)", "'London' > 'Paris' > 'Lyon' > 'null' > 'Oslo'",
                () -> String.join(" > ", selected)));
        checks.add(Checks.expect("selectInteger(zip) / select(name)", "150 / Ada",
                () -> zip.get() + " / " + (pojoName == null ? "unsupported" : pojoName.get())));
        checks.add(Checks.expect("address.flatMap(city).orElse", "London > Paris > Lyon > (no city) > Oslo",
                () -> String.join(" > ", flat)));

        Label bound = new Label();
        bound.textProperty().bind(Bindings.concat("selectString → ", city, "   flatMap → ", fluent, "   zip → ", zip));
        bound.getStyleClass().add("kv-value");
        return PlatformUi.demo("Bindings.select on a JavaFX bean (reflection) vs ObservableValue.flatMap",
                PlatformUi.line("selectString: " + String.join(" > ", selected)),
                PlatformUi.line("flatMap:      " + String.join(" > ", flat)),
                PlatformUi.line("JavaBean root selectString(owner): " + String.join(" > ", legacyOwners)), bound);
    }

    // ------------------------------------------------------------------ JavaBean adapters

    private VBox adapters(List<Check> checks) {
        LegacyAccount account = new LegacyAccount("Grace", 100);
        List<String> events = new ArrayList<>();
        Label ownerLabel = new Label();
        Label balanceLabel = new Label();
        try {
            JavaBeanStringProperty owner = JavaBeanStringPropertyBuilder.create().bean(account).name("owner").build();
            ReadOnlyJavaBeanIntegerProperty balance = ReadOnlyJavaBeanIntegerPropertyBuilder.create().bean(account)
                    .name("balance").build();
            owner.addListener((observable, oldValue, newValue) -> events.add("owner " + oldValue + " → " + newValue));
            balance.addListener((observable, oldValue, newValue) -> events.add("balance " + oldValue + " → " + newValue));
            ownerLabel.textProperty().bind(Bindings.concat("owner: ", owner));
            balanceLabel.textProperty().bind(Bindings.concat("balance: ", balance.asString()));

            owner.set("Hopper");
            String written = account.getOwner();
            account.setOwner("Grace Hopper");
            account.deposit(50);

            checks.add(Checks.expect("JavaBeanStringProperty write-through", "Hopper", () -> written));
            checks.add(Checks.expect("adapter events (bean → property)",
                    "owner Grace → Hopper, owner Hopper → Grace Hopper, balance 100 → 150", () -> String.join(", ", events)));
            checks.add(Checks.expect("adapter name / bean", "owner / balance / same bean",
                    () -> owner.getName() + " / " + balance.getName()
                            + (owner.getBean() == account && balance.getBean() == account ? " / same bean" : " / other")));
        } catch (Throwable t) {
            checks.add(Check.fail("JavaBean property adapters", Checks.describe(t)));
            ownerLabel.setText(Checks.describe(t));
            ownerLabel.setWrapText(true);
        }
        HBox values = new HBox(20, ownerLabel, balanceLabel);
        return PlatformUi.demo("JavaBean*PropertyBuilder adapters on a plain Java bean (PropertyChangeSupport)",
                values, PlatformUi.line(String.join("   ", events)));
    }

    // ------------------------------------------------------------------ collections

    private VBox collections(List<Check> checks) {
        ObservableList<String> source = FXCollections.observableArrayList("Kiwi", "apple", "Banana", "cherry", "Date",
                "fig", "Grape", "elderberry");
        FilteredList<String> filtered = new FilteredList<>(source, s -> s.length() > 4);
        SortedList<String> sorted = new SortedList<>(filtered, String.CASE_INSENSITIVE_ORDER);
        source.addAll("Mango", "lime");
        source.remove("fig");
        checks.add(Checks.expect("SortedList(FilteredList(length > 4))", "[apple, Banana, cherry, elderberry, Grape, Mango]",
                sorted::toString));
        filtered.setPredicate(s -> Character.isUpperCase(s.charAt(0)));
        checks.add(Checks.expect("setPredicate(upper case) + getSourceIndex(0)", "[Banana, Date, Grape, Kiwi, Mango] / 1",
                () -> sorted + " / " + sorted.getSourceIndex(0)));

        ObservableList<Integer> numbers = FXCollections.observableArrayList(1, 2, 3, 4, 5, 6);
        List<String> operations = new ArrayList<>();
        FXCollections.rotate(numbers, 2);
        operations.add("rotate(2) " + numbers);
        FXCollections.reverse(numbers);
        operations.add("reverse " + numbers);
        FXCollections.shuffle(numbers, new Random(42));
        operations.add("shuffle(Random(42)) " + numbers);
        operations.add("concat " + FXCollections.concat(FXCollections.observableArrayList(1, 2),
                FXCollections.observableArrayList(3)));
        ObservableIntegerArray array = FXCollections.observableIntegerArray(3, 1, 4);
        array.addAll(1, 5);
        operations.add("IntegerArray " + array);
        checks.add(Checks.expect("FXCollections rotate / reverse / shuffle(seed)",
                "rotate(2) [5, 6, 1, 2, 3, 4] | reverse [4, 3, 2, 1, 6, 5] | shuffle(Random(42)) [3, 4, 6, 5, 1, 2]",
                () -> String.join(" | ", operations.subList(0, 3))));
        checks.add(Checks.run("unmodifiableObservableList.add", () -> {
            try {
                FXCollections.unmodifiableObservableList(source).add("x");
                return "no exception";
            } catch (UnsupportedOperationException e) {
                return "UnsupportedOperationException";
            }
        }));
        checks.add(Checks.expect("list.sorted(reverse).filtered", "[lime, elderberry, cherry, apple]",
                () -> source.sorted(Comparator.reverseOrder()).filtered(s -> Character.isLowerCase(s.charAt(0))).toString()));

        ListView<String> list = new ListView<>(sorted);
        list.setPrefSize(150, 128);
        list.setFocusTraversable(false);
        VBox lines = new VBox(2);
        for (String operation : operations) {
            lines.getChildren().add(PlatformUi.line(operation));
        }
        lines.getChildren().add(PlatformUi.line("source " + source));
        HBox content = new HBox(10, list, lines);
        return PlatformUi.demo("SortedList(FilteredList) in a ListView, FXCollections utilities", content);
    }

    // ------------------------------------------------------------------ change listeners

    private VBox listeners(List<Check> checks) {
        List<String> events = new ArrayList<>();
        ObservableList<Item> items = FXCollections.observableArrayList(item -> new Observable[] { item.name });
        ListChangeListener<Item> listListener = change -> {
            while (change.next()) {
                events.add("list " + describe(change));
            }
        };
        items.addListener(listListener);
        items.addAll(new Item("one"), new Item("two"), new Item("three"));
        items.add(1, new Item("four"));
        items.remove(0);
        items.set(0, new Item("FOUR"));
        FXCollections.sort(items, Comparator.comparing(Item::toString));
        items.get(1).name.set("deux");

        ObservableMap<String, Integer> map = FXCollections.observableHashMap();
        map.addListener((MapChangeListener<String, Integer>) change -> {
            if (change.wasAdded() && change.wasRemoved()) {
                events.add("map replaced " + change.getKey() + "=" + change.getValueRemoved() + " by "
                        + change.getValueAdded());
            } else if (change.wasAdded()) {
                events.add("map added " + change.getKey() + "=" + change.getValueAdded());
            } else {
                events.add("map removed " + change.getKey() + "=" + change.getValueRemoved());
            }
        });
        map.put("a", 1);
        map.put("a", 2);
        map.put("b", 3);
        map.remove("a");

        ObservableSet<String> set = FXCollections.observableSet();
        set.addListener((SetChangeListener<String>) change -> events.add("set " + (change.wasAdded()
                ? "added " + change.getElementAdded() : "removed " + change.getElementRemoved())));
        set.add("x");
        set.add("x");
        set.add("y");
        set.remove("x");

        // weak listeners, kept strongly reachable while used
        List<String> weak = new ArrayList<>();
        StringProperty text = new SimpleStringProperty("a");
        ChangeListener<String> change = (observable, oldValue, newValue) -> weak.add("change " + newValue);
        InvalidationListener invalidation = observable -> weak.add("invalidated");
        ListChangeListener<Item> listChange = c -> weak.add("list change");
        WeakChangeListener<String> weakChange = new WeakChangeListener<>(change);
        WeakInvalidationListener weakInvalidation = new WeakInvalidationListener(invalidation);
        text.addListener(weakChange);
        text.addListener(weakInvalidation);
        items.addListener(new WeakListChangeListener<>(listChange));
        text.set("b");
        items.add(new Item("five"));

        checks.add(Checks.expect("change events (list / map / set)", "7 / 4 / 3", () -> events.stream()
                .collect(Collectors.groupingBy(e -> e.substring(0, e.indexOf(' ')), Collectors.counting()))
                .entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(e -> String.valueOf(e.getValue()))
                .collect(Collectors.joining(" / "))));
        checks.add(Checks.expect("weak listeners (strongly held)", "invalidated, change b, list change / collected: false",
                () -> String.join(", ", weak) + " / collected: " + (weakChange.wasGarbageCollected()
                        || weakInvalidation.wasGarbageCollected())));
        Reference.reachabilityFence(change);
        Reference.reachabilityFence(invalidation);
        Reference.reachabilityFence(listChange);
        Reference.reachabilityFence(listListener);

        VBox log = new VBox(1);
        // events in two columns
        HBox columns = new HBox(16);
        int split = (events.size() + 1) / 2;
        for (List<String> part : List.of(events.subList(0, split), events.subList(split, events.size()))) {
            VBox column = new VBox(1);
            part.forEach(e -> column.getChildren().add(PlatformUi.line(e)));
            columns.getChildren().add(column);
        }
        log.getChildren().add(columns);
        return PlatformUi.demo("ListChangeListener (extractor) / MapChangeListener / SetChangeListener events", log);
    }

    private static String describe(ListChangeListener.Change<? extends Item> change) {
        if (change.wasPermutated()) {
            return "permutated " + IntStream.range(change.getFrom(), change.getTo()).mapToObj(change::getPermutation)
                    .map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        } else if (change.wasUpdated()) {
            return "updated " + change.getFrom() + ".." + change.getTo() + " " + change.getList()
                    .subList(change.getFrom(), change.getTo());
        } else if (change.wasReplaced()) {
            return "replaced " + change.getRemoved() + " by " + change.getAddedSubList() + " at " + change.getFrom();
        } else if (change.wasAdded()) {
            return "added " + change.getAddedSubList() + " at " + change.getFrom();
        } else {
            return "removed " + change.getRemoved() + " at " + change.getFrom();
        }
    }

    // ------------------------------------------------------------------ bound controls

    private VBox boundControls(List<Check> checks) {
        Slider slider = new Slider(0, 100, 64);
        slider.setPrefWidth(170);
        TextField field = new TextField();
        field.setPrefColumnCount(5);
        StringConverter<Number> converter = new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return value == null ? "" : PlatformUi.round(value.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.valueOf(string);
            }
        };
        Bindings.bindBidirectional(field.textProperty(), slider.valueProperty(), converter);
        String initial = field.getText();
        field.setText("42.5");
        double fromText = slider.getValue();
        slider.setValue(72);
        checks.add(Checks.expect("bindBidirectional(text, value, converter)", "64.0 / 42.5 / 72.0",
                () -> initial + " / " + fromText + " / " + field.getText()));

        ProgressBar bar = new ProgressBar();
        bar.progressProperty().bind(slider.valueProperty().divide(100));
        bar.setPrefWidth(120);
        Rectangle swatch = new Rectangle(40, 20);
        swatch.fillProperty().bind(Bindings.createObjectBinding(
                () -> Color.hsb(slider.getValue() * 3.6, 0.7, 0.9), slider.valueProperty()));
        Label formatted = new Label();
        formatted.textProperty().bind(Bindings.format(Locale.ROOT, "%.1f %% → hue %.0f°", slider.valueProperty(),
                slider.valueProperty().multiply(3.6)));
        checks.add(Checks.expect("Bindings.format(ROOT) / createObjectBinding", "72.0 % → hue 259° / 0x7845e5ff",
                () -> formatted.getText() + " / " + swatch.getFill()));

        HBox row1 = new HBox(8, slider, field, bar);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, swatch, formatted);
        row2.setAlignment(Pos.CENTER_LEFT);
        return PlatformUi.width(PlatformUi.demo("Bound controls : bidirectional + converter, format, object binding",
                row1, row2), PlatformUi.HALF_WIDTH);
    }

    // ------------------------------------------------------------------ fluent bindings and subscriptions

    private VBox fluent(List<Check> checks) {
        StringProperty name = new SimpleStringProperty("ada");
        ObservableValue<String> upper = name.map(s -> s.toUpperCase(Locale.ROOT)).orElse("<none>");
        List<String> upperValues = new ArrayList<>();
        upper.subscribe((String value) -> upperValues.add(value));
        name.set(null);
        name.set("grace");
        checks.add(Checks.expect("map(toUpperCase).orElse(<none>)", "ADA, <none>, GRACE", () -> String.join(", ", upperValues)));

        IntegerProperty counter = new SimpleIntegerProperty(1);
        BooleanProperty active = new SimpleBooleanProperty(true);
        ObservableValue<Number> gated = counter.when(active);
        List<String> gatedValues = new ArrayList<>();
        gated.subscribe((Number value) -> gatedValues.add(String.valueOf(value)));
        counter.set(2);
        active.set(false);
        counter.set(3);
        counter.set(4);
        active.set(true);
        checks.add(Checks.expect("counter.when(active)", "1, 2, 4", () -> String.join(", ", gatedValues)));

        List<String> subscriptions = new ArrayList<>();
        StringProperty text = new SimpleStringProperty("start");
        Subscription values = text.subscribe((String value) -> subscriptions.add("value " + value));
        Subscription changes = text.subscribe((oldValue, newValue) -> subscriptions.add("change " + oldValue + ">" + newValue));
        Subscription invalidations = text.subscribe(() -> subscriptions.add("invalid"));
        text.set("x");
        Subscription all = values.and(changes).and(invalidations);
        all.unsubscribe();
        text.set("y");
        checks.add(Checks.expect("subscribe(value / change / invalidation) + unsubscribe",
                "value start, invalid, value x, change start>x", () -> String.join(", ", subscriptions)));

        StringProperty first = new SimpleStringProperty("Duke");
        StringProperty last = new SimpleStringProperty("Java");
        StringBinding full = Bindings.createStringBinding(() -> first.get() + " " + last.get(), first, last);
        IntegerProperty a = new SimpleIntegerProperty(6);
        IntegerProperty b = new SimpleIntegerProperty(7);
        NumberBinding arithmetic = a.multiply(b).add(0.5);
        ObservableList<String> list = FXCollections.observableArrayList("zero", "one");
        ObservableMap<String, String> map = FXCollections.observableHashMap();
        map.put("key", "mapped");
        ReadOnlyIntegerProperty size = new SimpleIntegerProperty(0);
        IntegerBinding listSize = Bindings.size(list);
        StringBinding many = Bindings.when(listSize.greaterThan(2)).then("many").otherwise("few");
        ObservableList<String> copy = FXCollections.observableArrayList();
        Bindings.bindContent(copy, list);
        list.add("two");
        last.set("FX");
        checks.add(Checks.expect("createStringBinding / arithmetic / when / size",
                "Duke FX / 42.5 / many / 3", () -> full.get() + " / " + arithmetic.getValue() + " / " + many.get() + " / "
                        + listSize.get()));
        checks.add(Checks.expect("valueAt / stringValueAt / bindContent / convert",
                "one / mapped / [zero, one, two] / 0",
                () -> Bindings.valueAt(list, 1).get() + " / " + Bindings.stringValueAt(map, "key").get() + " / " + copy
                        + " / " + Bindings.convert(size).get()));

        VBox lines = new VBox(1,
                PlatformUi.line("map/orElse   " + String.join(", ", upperValues)),
                PlatformUi.line("when(active) " + String.join(", ", gatedValues)),
                PlatformUi.line("subscribe    " + String.join(", ", subscriptions)),
                PlatformUi.line("bindings     " + full.get() + " | " + arithmetic.getValue() + " | " + many.get()));
        return PlatformUi.width(PlatformUi.demo("ObservableValue map / flatMap / orElse / when / subscribe", lines),
                PlatformUi.HALF_WIDTH);
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return PlatformUi.ready(content);
    }
}
