package io.quarkiverse.fx.showcase.pages.data;

import java.util.Locale;

import io.quarkus.runtime.annotations.RegisterForReflection;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ListViewSkin;

/**
 * ListView skin installed from data.css through {@code -fx-skin}: JavaFX loads it by name (Class.forName) and
 * instantiates it through its public {@code (ListView)} constructor, found with Class.getConstructors().
 * It reserves a footer below the cells.
 */
@RegisterForReflection
public class FooterListViewSkin<T> extends ListViewSkin<T> {

    private final Label footer = new Label();

    public FooterListViewSkin(ListView<T> control) {
        super(control);
        footer.getStyleClass().add("list-footer");
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(Locale.ROOT, "-fx-skin: %s · %d items", getClass().getSimpleName(),
                        control.getItems().size()),
                control.getItems()));
        getChildren().add(footer);
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        double footerHeight = snapSizeY(footer.prefHeight(w));
        super.layoutChildren(x, y, w, h - footerHeight);
        footer.resizeRelocate(x, y + h - footerHeight, w, footerHeight);
    }

    @Override
    public void dispose() {
        if (getSkinnable() == null) {
            return;
        }
        footer.textProperty().unbind();
        getChildren().remove(footer);
        super.dispose();
    }
}
