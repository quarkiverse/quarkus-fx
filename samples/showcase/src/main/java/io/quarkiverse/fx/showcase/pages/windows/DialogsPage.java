package io.quarkiverse.fx.showcase.pages.windows;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Dialog panes of every alert type embedded in the page, real dialog windows (alert, text input, choice, custom) opened
 * with show() and captured, file and directory choosers (configured, only shown in interactive mode).
 */
@Singleton
public class DialogsPage implements FeaturePage {

    private static final double PANE_WIDTH = 334;
    private static final String LIVE_TITLE = "Dialog windows (show, capture, answer)";

    @Override
    public String id() {
        return "windows-dialogs";
    }

    @Override
    public String title() {
        return "Dialogs & File Choosers";
    }

    @Override
    public String category() {
        return Categories.WINDOWS;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() {
        VBox root = WindowSupport.page(8);

        GridPane panes = new GridPane();
        panes.setHgap(13);
        panes.setVgap(10);
        for (int i = 0; i < 3; i++) {
            panes.getColumnConstraints().add(new ColumnConstraints(PANE_WIDTH));
        }

        panes.add(WindowSupport.demo("DialogPane · AlertType.INFORMATION",
                alertPane(AlertType.INFORMATION, "Export complete", "12 snapshots were written to disk.")), 0, 0);
        panes.add(WindowSupport.demo("DialogPane · AlertType.WARNING",
                alertPane(AlertType.WARNING, "Unsaved changes", "Closing now discards the edits of 3 pages.")), 1, 0);

        DialogPane error = alertPane(AlertType.ERROR, "Native build failed", "A class was initialized at build time.");
        TextArea trace = new TextArea("com.oracle.graal.pointsto.constraints.UnsupportedFeatureException\n"
                + "    at io.quarkiverse.fx.showcase.Example.<clinit>(Example.java:42)");
        trace.setEditable(false);
        trace.setPrefRowCount(3);
        error.setExpandableContent(trace);
        panes.add(WindowSupport.demo("DialogPane · AlertType.ERROR (expandable content, collapsed)", error), 2, 0);

        panes.add(WindowSupport.demo("DialogPane · AlertType.CONFIRMATION (default header)",
                alertPane(AlertType.CONFIRMATION, null, "Delete the 3 selected items?")), 0, 1);

        DialogPane none = alertPane(AlertType.NONE, "No type, custom graphic", "Image graphic and custom buttons.");
        Image icon = new Image(Fx.resourceUrl("/showcase/images/icon.png"));
        ImageView graphic = new ImageView(icon);
        graphic.setFitWidth(40);
        graphic.setFitHeight(40);
        none.setGraphic(graphic);
        none.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, new ButtonType("Later", ButtonData.OTHER));
        panes.add(WindowSupport.demo("DialogPane · AlertType.NONE", none), 1, 1);

        FileChooser fileChooser = fileChooser();
        DirectoryChooser directoryChooser = directoryChooser();
        panes.add(WindowSupport.demo("Real windows (interactive) · FileChooser · DirectoryChooser",
                launchers(fileChooser, directoryChooser)), 2, 1);

        List<Check> checks = new ArrayList<>();
        checks.add(Checks.expect("ButtonType texts (bundle)", "OK Cancel Yes No Apply Close",
                () -> List.of(ButtonType.OK, ButtonType.CANCEL, ButtonType.YES, ButtonType.NO, ButtonType.APPLY,
                        ButtonType.CLOSE).stream().map(ButtonType::getText).collect(Collectors.joining(" "))));
        checks.add(Checks.run("Alert titles / headers", () -> alertDefaults(alert -> java.util.Objects
                .equals(alert.getTitle(), alert.getHeaderText()) ? quote(alert.getTitle())
                        : quote(alert.getTitle()) + "/" + quote(alert.getHeaderText()))));
        checks.add(Checks.run("Alert buttons / style class", () -> alertDefaults(alert -> alert.getButtonTypes().stream()
                .map(ButtonType::getText).toList() + " " + alert.getDialogPane().getStyleClass().stream()
                        .filter(c -> !c.equals("root") && !c.equals("dialog-pane") && !c.equals("alert"))
                        .collect(Collectors.joining()))));
        checks.add(Checks.expect("Alert.close() without answer", "Cancel (CANCEL_CLOSE)", () -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.close();
            return describe(alert.getResult());
        }));
        checks.add(Checks.run("modena.css -fx-graphic", DialogsPage::cssGraphics));
        checks.add(Checks.expect("NONE pane graphic (icon.png)", "64x64", () -> icon.isError()
                ? "ERROR " + Checks.describe(icon.getException())
                : WindowSupport.size(icon.getWidth(), icon.getHeight())));
        checks.add(Checks.run("ButtonBar platform defaults", () -> {
            ButtonBar bar = new ButtonBar();
            return "order " + bar.getButtonOrder() + ", min width " + WindowSupport.fmt(bar.getButtonMinWidth());
        }));
        checks.add(Checks.expect("Dialog<String> result converter", "Apply (APPLY) → converted Apply", () -> {
            // the buttons of a dialog that was never shown still convert and set its result
            Dialog<String> dialog = new Dialog<>();
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.APPLY, ButtonType.CANCEL);
            dialog.setResultConverter(type -> "converted " + type.getText());
            ((Button) dialog.getDialogPane().lookupButton(ButtonType.APPLY)).fire();
            return describe(ButtonType.APPLY) + " → " + dialog.getResult();
        }));
        checks.add(Checks.run("ExtensionFilters", () -> fileChooser.getExtensionFilters().stream()
                .map(f -> f.getDescription() + " " + f.getExtensions().size()).collect(Collectors.joining(", "))
                + ", selected " + fileChooser.getSelectedExtensionFilter().getDescription()));
        checks.add(Checks.expect("File / directory choosers", "showcase.png, no initial directory",
                () -> fileChooser.getInitialFileName() + ", "
                        + (directoryChooser.getInitialDirectory() == null ? "no initial directory" : "initial set")));

        VBox staticChecks = Checks.view("Dialog defaults, resources and CSS", checks);
        VBox live = WindowSupport.liveSection(root, LIVE_TITLE, List.of(),
                "Opens an Alert, a TextInputDialog, a ChoiceDialog and a custom Dialog<ButtonType>.",
                this::runLive);

        HBox bottom = new HBox(16, staticChecks, live);
        staticChecks.setPrefWidth(506);
        staticChecks.setMinWidth(506);
        live.setPrefWidth(506);
        HBox.setHgrow(live, Priority.ALWAYS);

        root.getChildren().addAll(panes, bottom);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return runLive(content);
    }

    @Override
    public CompletionStage<Map<String, Image>> extraSnapshots(Node content) {
        return WindowSupport.images(content);
    }

    @Override
    public void dispose(Node content) {
        WindowSupport.dispose(content);
    }

    // ------------------------------------------------------------------------------------------------ embedded panes

    /**
     * A dialog pane configured like the one of an {@link Alert} of the given type (style classes, buttons, header).
     */
    private static DialogPane alertPane(AlertType type, String header, String content) {
        DialogPane source = new Alert(type).getDialogPane();
        DialogPane pane = new EmbeddedDialogPane();
        pane.getStyleClass().setAll(source.getStyleClass().filtered(c -> !c.equals("root")));
        pane.getStyleClass().add("embedded-dialog");
        pane.getButtonTypes().setAll(source.getButtonTypes());
        pane.setHeaderText(header == null ? source.getHeaderText() : header);
        pane.setContentText(content);
        pane.setPrefWidth(PANE_WIDTH);
        pane.setMaxWidth(PANE_WIDTH);
        // set by Dialog on its own pane, selects the header styles of modena.css
        pane.pseudoClassStateChanged(PseudoClass.getPseudoClass("header"), true);
        pane.pseudoClassStateChanged(PseudoClass.getPseudoClass("no-header"), false);
        return pane;
    }

    private static String quote(String text) {
        return text == null ? "null" : text.isEmpty() ? "''" : text;
    }

    /**
     * A dialog pane shown inside the page : its buttons never take the focus of the main scene (ButtonBarSkin requests
     * the focus on the default button whenever it lays the buttons out, and a focused button looks different whether
     * the main window is focused or not).
     */
    static final class EmbeddedDialogPane extends DialogPane {

        @Override
        protected Node createButton(ButtonType buttonType) {
            Button button = new Button(buttonType.getText()) {
                @Override
                public void requestFocus() {
                    // display only
                }
            };
            ButtonData data = buttonType.getButtonData();
            ButtonBar.setButtonData(button, data);
            button.setDefaultButton(data.isDefaultButton());
            button.setCancelButton(data.isCancelButton());
            return button;
        }
    }

    private static String alertDefaults(Function<Alert, String> describe) {
        List<String> values = new ArrayList<>();
        for (AlertType type : List.of(AlertType.INFORMATION, AlertType.WARNING, AlertType.ERROR, AlertType.CONFIRMATION,
                AlertType.NONE)) {
            values.add(describe.apply(new Alert(type)));
        }
        return String.join(", ", values);
    }

    /**
     * Resolves the -fx-graphic of the alert style classes of modena.css (images loaded from the javafx-controls jar).
     */
    private static String cssGraphics() {
        List<String> result = new ArrayList<>();
        for (String styleClass : List.of("information", "warning", "error", "confirmation")) {
            DialogPane probe = new DialogPane();
            probe.getStyleClass().addAll("alert", styleClass);
            new Scene(new Group(probe));
            probe.applyCss();
            Node graphic = probe.getGraphic();
            if (graphic instanceof ImageView view && view.getImage() != null) {
                Image image = view.getImage();
                String url = image.getUrl() == null ? "?" : image.getUrl().substring(image.getUrl().lastIndexOf('/') + 1);
                result.add(url.replace("dialog-", "").replace(".png", "") + " "
                        + WindowSupport.size(image.getWidth(), image.getHeight()) + (image.isError() ? " ERROR" : ""));
            } else {
                throw new IllegalStateException(styleClass + ": no image graphic but " + graphic);
            }
        }
        // compact form when all images have the same size
        List<String> sizes = result.stream().map(r -> r.substring(r.indexOf(' ') + 1)).distinct().toList();
        return sizes.size() == 1 && !sizes.get(0).contains("ERROR")
                ? result.stream().map(r -> r.substring(0, r.indexOf(' '))).collect(Collectors.joining(", ")) + ": "
                        + sizes.get(0)
                : String.join(", ", result);
    }

    // --------------------------------------------------------------------------------------------- choosers & buttons

    private static FileChooser fileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open a showcase asset");
        chooser.setInitialFileName("showcase.png");
        FileChooser.ExtensionFilter images = new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.gif", "*.bmp");
        chooser.getExtensionFilters().addAll(images,
                new FileChooser.ExtensionFilter("Media", "*.mp4", "*.m4a", "*.wav", "*.aiff"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        chooser.setSelectedExtensionFilter(images);
        return chooser;
    }

    private static DirectoryChooser directoryChooser() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose an output folder");
        return chooser;
    }

    private Node launchers(FileChooser fileChooser, DirectoryChooser directoryChooser) {
        Label result = new Label("Result: none yet");
        result.getStyleClass().add("demo-note");

        Button alert = new Button("Alert");
        alert.setOnAction(e -> showInteractive(result, alertDialog(owner(alert))));
        Button text = new Button("TextInputDialog");
        text.setOnAction(e -> showInteractive(result, textInputDialog(owner(text))));
        Button choice = new Button("ChoiceDialog");
        choice.setOnAction(e -> showInteractive(result, choiceDialog(owner(choice))));
        Button custom = new Button("Custom Dialog");
        custom.setOnAction(e -> showInteractive(result, customDialog(owner(custom))));
        FlowPane dialogs = new FlowPane(6, 6, alert, text, choice, custom);

        Button open = new Button("Open file…");
        open.setOnAction(e -> {
            if (!ShowcaseMode.snapshot()) {
                File file = fileChooser.showOpenDialog(owner(open));
                result.setText("Result: " + (file == null ? "cancelled" : file.getName()));
            }
        });
        Button save = new Button("Save as…");
        save.setOnAction(e -> {
            if (!ShowcaseMode.snapshot()) {
                File file = fileChooser.showSaveDialog(owner(save));
                result.setText("Result: " + (file == null ? "cancelled" : file.getName()));
            }
        });
        Button folder = new Button("Choose folder…");
        folder.setOnAction(e -> {
            if (!ShowcaseMode.snapshot()) {
                File dir = directoryChooser.showDialog(owner(folder));
                result.setText("Result: " + (dir == null ? "cancelled" : dir.getName()));
            }
        });
        FlowPane choosers = new FlowPane(6, 6, open, save, folder);

        Label filters = WindowSupport.note("Filters: Images (*.png *.jpg *.gif *.bmp), Media (*.mp4 *.m4a *.wav"
                + " *.aiff), All files (*.*). Choosers are never shown in snapshot mode.");

        VBox box = new VBox(6, dialogs, choosers, filters, result);
        box.getStyleClass().add("launcher-box");
        box.setPadding(new Insets(8));
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private static Window owner(Node node) {
        return node.getScene() == null ? null : node.getScene().getWindow();
    }

    private static void showInteractive(Label result, Dialog<?> dialog) {
        if (ShowcaseMode.snapshot()) {
            return;
        }
        dialog.setOnHidden(e -> result.setText("Result: " + describe(dialog.getResult())));
        dialog.show();
    }

    private static String describe(Object result) {
        if (result instanceof ButtonType type) {
            return type.getText() + " (" + type.getButtonData() + ")";
        }
        return String.valueOf(result);
    }

    // ------------------------------------------------------------------------------------------------------ dialogs

    private static Alert alertDialog(Window owner) {
        Alert alert = new Alert(AlertType.CONFIRMATION, "The reference snapshot of this page will be overwritten.",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Alert window");
        alert.setHeaderText("Replace the existing snapshot?");
        alert.initOwner(owner);
        return alert;
    }

    private static TextInputDialog textInputDialog(Window owner) {
        TextInputDialog dialog = new TextInputDialog("quarkus-fx");
        dialog.setTitle("TextInputDialog");
        dialog.setHeaderText("Name the native executable");
        dialog.setContentText("Executable name:");
        dialog.initOwner(owner);
        return dialog;
    }

    private static ChoiceDialog<String> choiceDialog(Window owner) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("JVM", "JVM", "Native", "Both");
        dialog.setTitle("ChoiceDialog");
        dialog.setHeaderText("Which runtime should be compared?");
        dialog.setContentText("Runtime:");
        dialog.initOwner(owner);
        return dialog;
    }

    static final ButtonType CONNECT = new ButtonType("Connect", ButtonData.OK_DONE);
    static final ButtonType TEST = new ButtonType("Test", ButtonData.OTHER);

    private static Dialog<ButtonType> customDialog(Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Custom Dialog<ButtonType>");
        dialog.setHeaderText("Connection settings");
        dialog.setGraphic(WindowSupport.icon('\uf013', 30, "#2f6fb5"));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Host"), new TextField("localhost"));
        form.addRow(1, new Label("Port"), new TextField("8080"));
        CheckBox tls = new CheckBox("Use TLS");
        tls.setSelected(true);
        form.addRow(2, new Label("Security"), tls);
        Slider timeout = new Slider(0, 60, 30);
        timeout.setShowTickMarks(true);
        timeout.setMajorTickUnit(10);
        Label seconds = new Label("30 s");
        form.addRow(3, new Label("Timeout"), new HBox(8, timeout, seconds));

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(form);
        pane.getButtonTypes().setAll(CONNECT, TEST);
        Label details = new Label("Expandable content: proxy and certificate settings would go here.");
        details.setWrapText(true);
        pane.setExpandableContent(details);
        pane.setExpanded(true);
        dialog.initOwner(owner);
        return dialog;
    }

    // -------------------------------------------------------------------------------------------------- live checks

    private CompletionStage<Void> runLive(Node content) {
        Stage main = WindowSupport.mainStage(content);
        WindowSupport.Live live = WindowSupport.startLive(content);
        if (main == null) {
            live.checks.add(Check.fail("main window", "page not showing"));
            WindowSupport.publish(content, live);
            return CompletableFuture.completedFuture(null);
        }
        boolean mainFocused = main.isFocused();
        double x = main.getX() + 320;
        double y = main.getY() + 170;

        // ready() is invoked right after build(): let the page get its skins and layout first
        CompletionStage<Void> chain = Fx.pulses(3);
        chain = live.step(chain, "Alert window", v -> alertScenario(live, main, x, y));
        chain = live.step(chain, "TextInputDialog window", v -> textInputScenario(live, main, x, y));
        chain = live.step(chain, "ChoiceDialog window", v -> choiceScenario(live, main, x, y));
        chain = live.step(chain, "Custom dialog window", v -> customScenario(live, main, x, y));
        return chain.thenRun(() -> {
            // setX / setY before show() : the dialogs are not centered on their owner
            List<String> positions = live.images.keySet().stream()
                    .map(key -> key + " " + live.notes.getOrDefault(key, "?")).toList();
            String requested = "+" + WindowSupport.fmt(x - main.getX()) + ",+" + WindowSupport.fmt(y - main.getY());
            boolean complete = positions.size() == 4 && positions.stream().noneMatch(p -> p.endsWith(" no owner"));
            boolean all = complete && positions.stream().allMatch(p -> p.endsWith(" " + requested));
            // within a device pixel (fractional scales of Windows); informational on Linux (window manager placement)
            boolean placed = complete && live.images.keySet().stream()
                    .allMatch(key -> Boolean.TRUE.equals(live.placed.get(key)));
            live.checks.add(new Check("Dialog positions (from owner)",
                    all ? "all 4 at " + requested : String.join(", ", positions),
                    complete ? WindowSupport.placement(all || placed) : Boolean.FALSE));
            live.closeAll();
            if (mainFocused && !main.isFocused()) {
                main.requestFocus();
            }
            WindowSupport.publish(content, live);
        });
    }

    /**
     * Shows {@code dialog}, waits for it to be rendered, then captures it under {@code key}.
     */
    private static CompletionStage<Void> showAndCapture(WindowSupport.Live live, Dialog<?> dialog, String key,
            double x, double y, Runnable afterShow) {
        WindowSupport.styled(dialog.getDialogPane());
        WindowSupport.freezeAxes(dialog.getDialogPane());
        dialog.setX(x);
        dialog.setY(y);
        dialog.show();
        Scene scene = dialog.getDialogPane().getScene();
        WindowSupport.shield(scene.getWindow());
        live.opened.add(scene.getWindow());
        afterShow.run();
        return Fx.pulses(3).thenCompose(v -> {
            WindowSupport.stabilize(scene);
            afterShow.run();
            return Fx.pulses(2);
        }).thenAccept(v -> {
            live.images.put(key, WindowSupport.capture(scene));
            Window window = scene.getWindow();
            Window owner = dialog.getOwner();
            live.notes.put(key, owner == null ? "no owner"
                    : "+" + WindowSupport.fmt(window.getX() - owner.getX()) + ",+"
                            + WindowSupport.fmt(window.getY() - owner.getY()));
            live.placed.put(key, owner != null
                    && WindowSupport.near(window.getX() - owner.getX(), x - owner.getX(), window)
                    && WindowSupport.near(window.getY() - owner.getY(), y - owner.getY(), window));
        });
    }

    private static String windowInfo(Dialog<?> dialog, Stage main) {
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        return (stage.getOwner() == main ? "owned" : "not owned") + " " + stage.getModality() + " "
                + WindowSupport.size(stage.getScene().getWidth(), stage.getScene().getHeight());
    }

    private static CompletionStage<Void> alertScenario(WindowSupport.Live live, Stage main, double x, double y) {
        Alert alert = alertDialog(main);
        List<String> events = new ArrayList<>();
        alert.setOnShowing(e -> events.add("SHOWING"));
        alert.setOnShown(e -> events.add("SHOWN"));
        alert.setOnHiding(e -> events.add("HIDING"));
        alert.setOnCloseRequest(e -> events.add("CLOSE_REQUEST"));
        alert.setOnHidden(e -> events.add("HIDDEN"));
        return showAndCapture(live, alert, "alert", x, y, () -> {
        }).thenRun(() -> {
            String info = windowInfo(alert, main);
            boolean shown = alert.isShowing();
            ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).fire();
            live.checks.add(Check.of("Alert, OK fired", shown && !alert.isShowing() && alert.getResult() == ButtonType.OK,
                    info + " → " + describe(alert.getResult())));
            live.checks.add(Checks.expect("Alert DialogEvents", "SHOWING SHOWN HIDING CLOSE_REQUEST HIDDEN",
                    () -> String.join(" ", events)));
        });
    }

    private static CompletionStage<Void> textInputScenario(WindowSupport.Live live, Stage main, double x, double y) {
        TextInputDialog dialog = textInputDialog(main);
        String defaultValue = dialog.getDefaultValue();
        TextField editor = dialog.getEditor();
        return showAndCapture(live, dialog, "text-input-dialog", x, y, () -> {
            editor.setText("showcase-runner");
            editor.deselect();
        }).thenRun(() -> {
            String info = windowInfo(dialog, main);
            boolean shown = dialog.isShowing();
            ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).fire();
            live.checks.add(Check.of("TextInputDialog, OK fired",
                    shown && !dialog.isShowing() && "showcase-runner".equals(dialog.getResult()),
                    info + ", default " + defaultValue + " → " + dialog.getResult()));
        });
    }

    private static CompletionStage<Void> choiceScenario(WindowSupport.Live live, Stage main, double x, double y) {
        ChoiceDialog<String> dialog = choiceDialog(main);
        dialog.setSelectedItem("Native");
        return showAndCapture(live, dialog, "choice-dialog", x, y, () -> {
        }).thenRun(() -> {
            String info = windowInfo(dialog, main);
            boolean shown = dialog.isShowing();
            ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).fire();
            live.checks.add(Check.of("ChoiceDialog, OK fired",
                    shown && !dialog.isShowing() && "Native".equals(dialog.getResult()),
                    info + ", " + dialog.getItems().size() + " items → " + dialog.getResult()));
        });
    }

    private static CompletionStage<Void> customScenario(WindowSupport.Live live, Stage main, double x, double y) {
        Dialog<ButtonType> dialog = customDialog(main);
        return showAndCapture(live, dialog, "custom-dialog", x, y, () -> {
        }).thenRun(() -> {
            String info = windowInfo(dialog, main);
            boolean shown = dialog.isShowing();
            // without a cancel button, close() is refused while there is no result
            dialog.close();
            boolean refused = dialog.isShowing();
            live.checks.add(Check.of("Custom dialog close()", shown && refused,
                    info + ", no cancel button → close " + (refused ? "refused" : "accepted")));
            // validation pattern : an ACTION event filter consuming the event keeps the dialog open
            Button test = (Button) dialog.getDialogPane().lookupButton(TEST);
            List<String> vetoed = new ArrayList<>();
            test.addEventFilter(ActionEvent.ACTION, e -> {
                vetoed.add(((Button) e.getSource()).getText());
                e.consume();
            });
            test.fire();
            live.checks.add(Check.of("Custom dialog, Test vetoed", dialog.isShowing() && dialog.getResult() == null
                    && vetoed.equals(List.of("Test")),
                    (dialog.isShowing() ? "vetoed" : "closed") + " by " + vetoed + ", result " + dialog.getResult()));
            Button connect = (Button) dialog.getDialogPane().lookupButton(CONNECT);
            boolean isDefault = connect.isDefaultButton();
            connect.fire();
            live.checks.add(Check.of("Custom dialog, Connect fired",
                    isDefault && !dialog.isShowing() && dialog.getResult() == CONNECT,
                    (isDefault ? "default" : "not default") + " → " + describe(dialog.getResult())));
        });
    }
}
