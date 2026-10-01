package io.quarkiverse.fx.showcase.pages.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Cell;
import javafx.scene.control.Label;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Layout and lifecycle helpers shared by the data pages.
 */
final class DataUi {

    static final String STYLESHEET = "/showcase/data/data.css";

    private static final String READY_KEY = "showcase.data.ready";

    private DataUi() {
    }

    /**
     * Applies the data pages stylesheet to a page root.
     */
    static <T extends Parent> T page(T root) {
        root.getStylesheets().add(Fx.resourceUrl(STYLESHEET));
        root.getStyleClass().add("data-page");
        return root;
    }

    static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("data-caption");
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    static Label note(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("data-note");
        label.setWrapText(true);
        return label;
    }

    /**
     * A captioned demo whose node takes the remaining height.
     */
    static VBox demo(String caption, Node node) {
        VBox box = new VBox(4, caption(caption), node);
        VBox.setVgrow(node, Priority.ALWAYS);
        box.getStyleClass().add("data-demo");
        return box;
    }

    /**
     * A captioned demo with a fixed size.
     */
    static VBox demo(String caption, Node node, double width, double height) {
        VBox box = demo(caption, node);
        box.setPrefSize(width, height);
        box.setMinSize(width, height);
        box.setMaxSize(width, height);
        return box;
    }

    static HBox row(Node... nodes) {
        HBox row = new HBox(12, nodes);
        row.setFillHeight(true);
        return row;
    }

    static <T extends Region> T size(T region, double width, double height) {
        region.setPrefSize(width, height);
        region.setMinSize(width, height);
        region.setMaxSize(width, height);
        return region;
    }

    /**
     * Records the completion stage returned by {@code ready()} for this content.
     */
    static void setReady(Node content, CompletionStage<?> stage) {
        content.getProperties().put(READY_KEY, stage);
    }

    static CompletionStage<?> ready(Node content) {
        Object stage = content.getProperties().get(READY_KEY);
        return stage instanceof CompletionStage<?> s ? s : CompletableFuture.completedFuture(null);
    }

    /**
     * Holds a checks view that is completed once the page is laid out.
     */
    static final class ChecksHolder extends StackPane {

        private final String title;
        private final List<Check> checks;

        ChecksHolder(String title, List<Check> checks) {
            this.title = title;
            this.checks = new ArrayList<>(checks);
            getChildren().setAll(Checks.view(title, this.checks));
            setMinWidth(0);
        }

        /**
         * Adds post-layout checks and refreshes the view.
         */
        void complete(List<Check> more) {
            checks.addAll(more);
            getChildren().setAll(Checks.view(title, checks));
        }
    }

    /**
     * The virtual flow of a ListView, TableView, TreeView or TreeTableView (once its skin exists).
     */
    static VirtualFlow<?> flow(Node virtualized) {
        Node node = virtualized.lookup(".virtual-flow");
        if (node instanceof VirtualFlow<?> flow) {
            return flow;
        }
        throw new IllegalStateException("No VirtualFlow in " + virtualized.getClass().getSimpleName());
    }

    /**
     * "first..last" indices of the visible cells of a virtualized control.
     */
    static String visibleRange(Node virtualized) {
        VirtualFlow<?> flow = flow(virtualized);
        Cell<?> first = (Cell<?>) flow.getFirstVisibleCell();
        Cell<?> last = (Cell<?>) flow.getLastVisibleCell();
        if (first == null || last == null) {
            return "none";
        }
        return index(first) + ".." + index(last);
    }

    static int index(Cell<?> cell) {
        if (cell instanceof javafx.scene.control.IndexedCell<?> indexed) {
            return indexed.getIndex();
        }
        return -1;
    }

    /**
     * The first non-empty cell of the given type inside {@code root} showing {@code index}.
     */
    static <C extends javafx.scene.control.IndexedCell<?>> C cell(Node root, String selector, int index) {
        for (Node node : root.lookupAll(selector)) {
            if (node instanceof javafx.scene.control.IndexedCell<?> cell && cell.getIndex() == index && !cell.isEmpty()
                    && cell.isVisible()) {
                @SuppressWarnings("unchecked")
                C c = (C) cell;
                return c;
            }
        }
        throw new IllegalStateException("No " + selector + " for index " + index);
    }

    static Check check(String name, Supplier<?> supplier) {
        return Checks.run(name, supplier::get);
    }

    static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
