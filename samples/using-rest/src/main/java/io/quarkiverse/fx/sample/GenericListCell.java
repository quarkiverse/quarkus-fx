package io.quarkiverse.fx.sample;

import javafx.scene.control.ListCell;

import java.util.function.Function;

public class GenericListCell<T> extends ListCell<T> {

    private final Function<T, String> toString;

    private GenericListCell(Function<T, String> toString) {
        this.toString = toString;
    }

    public static <T> ListCell<T> of(Function<T, String> toString) {
        return new GenericListCell<>(toString);
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            this.setText(null);
        } else {
            this.setText(this.toString.apply(item));
        }
    }
}
