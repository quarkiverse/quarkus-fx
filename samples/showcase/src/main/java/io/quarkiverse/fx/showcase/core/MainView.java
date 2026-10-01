package io.quarkiverse.fx.showcase.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import io.quarkus.runtime.Quarkus;

/**
 * Main window : page navigation on the left, the selected page in the center.
 */
public final class MainView {

    public static final double PAGE_WIDTH = 1060;
    public static final double PAGE_HEIGHT = 760;

    private final List<FeaturePage> pages;
    private final BorderPane root = new BorderPane();
    private final TreeView<Object> nav = new TreeView<>();
    private final Label pageTitle = new Label();
    private final StackPane pageFrame = new StackPane();
    private final Map<FeaturePage, TreeItem<Object>> items = new LinkedHashMap<>();
    // owns the focus in snapshot mode, so that pages never show focus decorations, whether the window is active or not
    private final Region focusSink = new Region();

    private FeaturePage currentPage;
    private Node currentContent;
    private Throwable currentError;

    public MainView(List<FeaturePage> pages) {
        this.pages = pages;

        TreeItem<Object> navRoot = new TreeItem<>("Pages");
        Map<String, TreeItem<Object>> categories = new LinkedHashMap<>();
        for (FeaturePage page : pages) {
            TreeItem<Object> category = categories.computeIfAbsent(page.category(), name -> {
                TreeItem<Object> item = new TreeItem<>(name);
                item.setExpanded(true);
                navRoot.getChildren().add(item);
                return item;
            });
            TreeItem<Object> item = new TreeItem<>(page);
            category.getChildren().add(item);
            items.put(page, item);
        }
        nav.setRoot(navRoot);
        nav.setShowRoot(false);
        // a focused tree paints a focus ring and another selection color, depending on the window being active
        nav.setFocusTraversable(!ShowcaseMode.snapshot());
        nav.setPrefWidth(270);
        nav.setCellFactory(tree -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item instanceof FeaturePage page ? page.title() : item.toString());
            }
        });
        nav.getSelectionModel().selectedItemProperty().addListener((observable, oldItem, newItem) -> {
            if (newItem != null && newItem.getValue() instanceof FeaturePage page && page != currentPage) {
                show(page);
            }
        });

        pageTitle.getStyleClass().add("page-title");
        pageFrame.getStyleClass().add("page-frame");
        pageFrame.setAlignment(Pos.TOP_LEFT);
        pageFrame.setMinSize(PAGE_WIDTH, PAGE_HEIGHT);
        ScrollPane scroll = new ScrollPane(pageFrame);
        scroll.getStyleClass().add("page-scroll");

        VBox center = new VBox(8, pageTitle, scroll);
        center.setPadding(new Insets(10));
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        Label status = new Label(statusText());
        status.getStyleClass().add("status-text");
        focusSink.setManaged(false);
        HBox statusBar = new HBox(status, focusSink);
        statusBar.getStyleClass().add("status-bar");

        root.setTop(menuBar());
        root.setLeft(nav);
        root.setCenter(center);
        root.setBottom(statusBar);
    }

    private String statusText() {
        String text = "JavaFX " + System.getProperty("javafx.runtime.version") + " · " + pages.size() + " pages";
        // the runtime differs between the compared runs, keep it out of snapshots
        return ShowcaseMode.snapshot() ? text : ShowcaseMode.runtime() + " · " + text;
    }

    private MenuBar menuBar() {
        MenuItem previous = new MenuItem("Previous page");
        previous.setAccelerator(KeyCombination.keyCombination("Shortcut+OPEN_BRACKET"));
        previous.setOnAction(e -> step(-1));
        MenuItem next = new MenuItem("Next page");
        next.setAccelerator(KeyCombination.keyCombination("Shortcut+CLOSE_BRACKET"));
        next.setOnAction(e -> step(1));
        MenuItem quit = new MenuItem("Quit");
        quit.setAccelerator(KeyCombination.keyCombination("Shortcut+Q"));
        quit.setOnAction(e -> {
            Platform.exit();
            Quarkus.asyncExit();
        });
        Menu showcase = new Menu("Showcase", null, previous, next, new SeparatorMenuItem(), quit);

        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Quarkus FX Showcase");
            alert.setContentText(statusText() + "\nJava " + System.getProperty("java.version"));
            alert.show();
        });
        Menu help = new Menu("Help", null, about);

        MenuBar menuBar = new MenuBar(showcase, help);
        menuBar.setUseSystemMenuBar(true);
        return menuBar;
    }

    private void step(int delta) {
        int index = currentPage == null ? 0 : pages.indexOf(currentPage) + delta;
        if (index >= 0 && index < pages.size()) {
            select(pages.get(index));
        }
    }

    public Parent root() {
        return root;
    }

    public StackPane pageFrame() {
        return pageFrame;
    }

    public TreeView<Object> nav() {
        return nav;
    }

    public void select(FeaturePage page) {
        TreeItem<Object> item = items.get(page);
        nav.getSelectionModel().select(item);
        int row = nav.getRow(item);
        if (row >= 0) {
            nav.scrollTo(Math.max(0, row - 3));
        }
        if (currentPage != page) {
            show(page);
        }
    }

    public Node currentContent() {
        return currentContent;
    }

    public Throwable currentError() {
        return currentError;
    }

    /**
     * Disposes the current page, if any.
     */
    public void clear() {
        if (currentPage != null && currentContent != null && currentError == null) {
            try {
                currentPage.dispose(currentContent);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        currentPage = null;
        currentContent = null;
        currentError = null;
        pageFrame.getChildren().clear();
    }

    private void show(FeaturePage page) {
        clear();
        currentPage = page;
        pageTitle.setText(page.category() + " › " + page.title());
        Node content;
        try {
            content = page.build();
        } catch (Throwable t) {
            currentError = t;
            content = errorNode(t);
        }
        currentContent = content;
        pageFrame.getChildren().setAll(content);
        if (ShowcaseMode.snapshot()) {
            releaseFocus();
        }
    }

    /**
     * Moves the focus out of the page (snapshot mode).
     */
    public void releaseFocus() {
        focusSink.requestFocus();
    }

    private static Node errorNode(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        Label label = new Label("This page failed to build: " + Checks.describe(t));
        label.getStyleClass().add("check-fail");
        label.setWrapText(true);
        TextArea trace = new TextArea(sw.toString());
        trace.setEditable(false);
        trace.setPrefRowCount(30);
        return new VBox(8, label, trace);
    }
}
