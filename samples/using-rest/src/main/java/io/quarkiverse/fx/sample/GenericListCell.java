package io.quarkiverse.fx.sample;

import java.util.function.Function;

import javafx.scene.control.ListCell;

public class GenericListCell<T> extends ListCell<T> {

    private final Function<T, String> toString;

    private GenericListCell(final Function<T, String> toString) {
        this.toString = toString;
    }

    public static <T> ListCell<T> of(final Function<T, String> toString) {
        return new GenericListCell<>(toString);
    }

    @Override
    protected void updateItem(final T item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            this.setText(null);
        } else {
            this.setText(this.toString.apply(item));
        }
    }
}
