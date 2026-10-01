package io.quarkiverse.fx.showcase.pages.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.Platforms;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;

/**
 * HTMLEditor with preset rich content : its skin (created reflectively from -fx-skin), toolbars (resource bundle, icons)
 * and the HTML it serializes.
 */
@Singleton
public class HtmlEditorPage implements FeaturePage {

    private static final String STATE = HtmlEditorPage.class.getName();
    private static final String CONTENT = "/showcase/web/editor.html";
    /**
     * Script : the right edge of the page (x, y, width, height in page coordinates), where WebKit draws the vertical
     * scroll bar over the content when it does not fit (the page keeps its width : clientWidth is innerWidth), none when
     * it fits.
     */
    private static final String SCROLL_BAR_PROBE = "(function () { var viewport = document.scrollingElement;"
            + " return viewport.scrollHeight > viewport.clientHeight"
            + " ? [window.innerWidth - 3, 0, 3, window.innerHeight].join(',') : ''; })()";

    @Override
    public String id() {
        return "web-htmleditor";
    }

    @Override
    public String title() {
        return "HTMLEditor";
    }

    @Override
    public String category() {
        return Categories.WEB;
    }

    @Override
    public int order() {
        return 20;
    }

    private static final class State {
        final HTMLEditor editor = new HTMLEditor();
        final boolean skinAtConstruction = editor.getSkin() != null;
        final String skinClass = editor.getSkin() == null ? "null" : editor.getSkin().getClass().getName();
        final TextArea source = new TextArea();
        final VBox htmlChecks = new VBox();
        final VBox skinChecks = new VBox();
        String error;
        Throwable scrollBarError;
        CompletionStage<?> ready;
    }

    @Override
    public Node build() {
        State state = new State();
        HTMLEditor editor = state.editor;
        editor.setPrefSize(640, 410);
        editor.setMinSize(640, 410);
        editor.setMaxSize(640, 410);

        String html = Fx.resourceText(CONTENT);
        if (editor.getSkin() != null) {
            editor.setHtmlText(html);
        } else {
            // the skin was not created from -fx-skin : it will be created by the CSS pass
            editor.skinProperty().addListener((observable, oldSkin, newSkin) -> {
                if (oldSkin == null && newSkin != null) {
                    editor.setHtmlText(html);
                }
            });
        }

        state.source.setEditable(false);
        state.source.setWrapText(true);
        state.source.setFocusTraversable(false);
        // JavaFX CSS -fx-font-family takes a single family : the monospaced one of the platform ("Menlo" on macOS)
        state.source.setStyle("-fx-font-family: '" + Platforms.Families.mono() + "'; -fx-font-size: 10px;");
        state.source.setPrefSize(372, 410);
        state.source.setMinSize(372, 410);
        state.source.setMaxSize(372, 410);
        state.source.setText("Waiting for the editor content...");

        VBox left = new VBox(4, WebSupport.caption("HTMLEditor, setHtmlText(" + CONTENT.substring(1) + ")"), editor);
        VBox right = new VBox(4, WebSupport.caption("getHtmlText() after the load"), state.source);
        HBox top = new HBox(16, left, right);

        state.htmlChecks.setPrefWidth(506);
        state.skinChecks.setPrefWidth(506);
        HBox.setHgrow(state.htmlChecks, Priority.ALWAYS);
        HBox.setHgrow(state.skinChecks, Priority.ALWAYS);
        HBox checks = new HBox(16, state.htmlChecks, state.skinChecks);

        VBox root = new VBox(10, top, checks);
        root.getProperties().put(STATE, state);

        state.ready = WebSupport.until(() -> {
            WebView view = webView(editor);
            return view != null
                    && view.getEngine().getLoadWorker().getState() == Worker.State.SUCCEEDED
                    && view.getEngine().getDocument() != null
                    && view.getEngine().getDocument().getElementById("editor-marker") != null;
        }, 20_000, "HTMLEditor content")
                // the skin creates its toolbar buttons in its first layout pass (not in its constructor) : the load
                // can complete before it
                .thenCompose(v -> WebSupport.until(() -> editor.lookup(".html-editor-bold") != null, 10_000,
                        "HTMLEditor toolbars (first layout of the skin)"))
                // lets the skin apply its post-load and post-layout updates (content editable, toolbar state, font
                // family combo filled with Platform.runLater)
                .thenCompose(v -> Fx.pulses(5))
                .handle((v, error) -> {
                    state.error = error == null ? null : Checks.describe(WebSupport.unwrap(error));
                    return null;
                })
                // WebKit draws the scroll bar of the content with a JavaFX ScrollBar (see WebSnapshot.controlsPainted)
                .thenCompose(v -> state.error != null
                        ? CompletableFuture.completedFuture(null)
                        : WebSnapshot.controlsPainted(webView(editor), SCROLL_BAR_PROBE, "HTMLEditor scroll bar")
                                .handle((painted, error) -> {
                                    state.scrollBarError = error;
                                    return null;
                                }))
                .thenApply(v -> {
                    showChecks(state);
                    return null;
                })
                .thenCompose(v -> Fx.pulses(10))
                .thenCompose(v -> WebSupport.stable(editor, 10_000))
                .thenCompose(v -> Fx.delay(200));
        return root;
    }

    private static WebView webView(HTMLEditor editor) {
        return editor.lookup(".web-view") instanceof WebView view ? view : null;
    }

    private static void showChecks(State state) {
        HTMLEditor editor = state.editor;
        if (ShowcaseMode.snapshot()) {
            // the enabled state of cut / copy / paste follows the system clipboard and the selection, which are
            // outside of the page : rendered the same way whatever their state
            for (String styleClass : List.of("html-editor-cut", "html-editor-copy", "html-editor-paste")) {
                Node button = editor.lookup("." + styleClass);
                if (button != null) {
                    button.setStyle("-fx-opacity: 1;");
                }
            }
        }
        List<Check> html = new ArrayList<>();
        List<Check> skin = new ArrayList<>();

        html.add(state.error == null ? Check.pass("content loaded", "SUCCEEDED") : Check.fail("content loaded", state.error));
        String text;
        try {
            text = editor.getHtmlText();
        } catch (Throwable t) {
            text = null;
            html.add(Check.fail("getHtmlText()", Checks.describe(t)));
        }
        if (text != null) {
            state.source.setText(text);
            String lower = text.toLowerCase();
            html.add(Check.info("getHtmlText() length", text.length()));
            for (String[] expected : new String[][] {
                    { "contenteditable body", "contenteditable=\"true\"" },
                    { "heading", "<h2" },
                    { "bold / italic / underline", "<b>", "<i>", "<u>" },
                    { "unordered / ordered lists", "<ul>", "<ol>", "<li>" },
                    { "table", "<table", "<th>", "<td" },
                    { "colors (inline CSS)", "color: #c62828", "background-color: #fff59d" },
                    { "font face", "<font face=\"courier new\">" } }) {
                List<String> missing = new ArrayList<>();
                for (int i = 1; i < expected.length; i++) {
                    if (!lower.contains(expected[i].toLowerCase())) {
                        missing.add(expected[i]);
                    }
                }
                html.add(missing.isEmpty()
                        ? Check.pass(expected[0], String.join(" ", List.of(expected).subList(1, expected.length)))
                        : Check.fail(expected[0], "missing " + missing));
            }
        }

        skin.add(Check.of("skin created from -fx-skin", state.skinAtConstruction, state.skinClass));
        skin.add(Checks.run("toolbars", () -> editor.lookupAll(".tool-bar").size()));
        skin.add(Checks.expect("bold button tooltip (resource bundle)", "Bold", () -> {
            ButtonBase bold = (ButtonBase) editor.lookup(".html-editor-bold");
            return bold.getTooltip().getText();
        }));
        skin.add(Checks.run("bold button icon (resource image)", () -> {
            ButtonBase bold = (ButtonBase) editor.lookup(".html-editor-bold");
            ImageView icon = (ImageView) bold.getGraphic();
            if (icon.getImage().isError()) {
                throw new IllegalStateException("image error", icon.getImage().getException());
            }
            return (int) icon.getImage().getWidth() + "x" + (int) icon.getImage().getHeight();
        }));
        skin.add(Checks.run("toolbar buttons", () -> editor.lookupAll(".button").size() + " buttons, "
                + editor.lookupAll(".toggle-button").size() + " toggle buttons, "
                + editor.lookupAll(".color-picker").size() + " color pickers"));
        skin.add(Checks.run("combo values (format, family, size)", () -> {
            List<String> values = new ArrayList<>();
            for (Node node : editor.lookupAll(".font-menu-button")) {
                if (node instanceof ComboBox<?> combo) {
                    values.add("'" + combo.getValue() + "'");
                }
            }
            return String.join(", ", values);
        }));
        skin.add(Checks.expect("font family combo populated", true, () -> {
            // the installed families (and an empty entry) : more than 20, unless fewer are installed (minimal Linux)
            int expected = Math.min(20, Font.getFamilies().size());
            for (Node node : editor.lookupAll(".font-menu-button")) {
                if (node instanceof ComboBox<?> combo && combo.getItems().size() > expected) {
                    return true;
                }
            }
            return false;
        }));
        if (state.error == null) {
            skin.add(state.scrollBarError == null ? Check.pass("scroll bar in a WebView snapshot", true)
                    : Check.fail("scroll bar in a WebView snapshot",
                            Checks.describe(WebSupport.unwrap(state.scrollBarError))));
        }

        state.htmlChecks.getChildren().setAll(Checks.view("getHtmlText()", html));
        state.skinChecks.getChildren().setAll(Checks.view("Skin & toolbars", skin));
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ((State) content.getProperties().get(STATE)).ready;
    }

    @Override
    public void dispose(Node content) {
        State state = (State) content.getProperties().get(STATE);
        if (state != null) {
            WebView view = webView(state.editor);
            if (view != null) {
                view.getEngine().loadContent("");
            }
        }
    }
}
