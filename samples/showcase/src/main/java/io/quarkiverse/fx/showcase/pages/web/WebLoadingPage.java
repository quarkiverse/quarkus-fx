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
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * WebEngine.load of a classpath URL (with relative sub-resources) and of a data: URL, a user style sheet given as a
 * data: URL, WebView zoom and font scale, JavaScript UI callbacks (alert, confirm, prompt) and a WebEngine without
 * WebView with JavaScript disabled.
 * <p>
 * A user style sheet given as a classpath URL is only accepted in native executables with quarkus-fx : see
 * {@link io.quarkiverse.fx.showcase.pages.platform.PlatformNativeLimitsPage}.
 */
@Singleton
public class WebLoadingPage implements FeaturePage {

    private static final String STATE = WebLoadingPage.class.getName();
    private static final String CLASSPATH_PAGE = "/showcase/web/loaded.html";
    private static final String STYLESHEET = "/showcase/web/web-loading.css";

    private static final String DATA_PAGE = """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8"><title>Loaded from a data: URL</title>
            <style>
            body { margin: 8px; font-family: Helvetica, Arial, 'Liberation Sans', sans-serif; font-size: 12px; background: #e3f2fd; color: #0d47a1; }
            h1 { margin: 0 0 4px 0; font-size: 18px; border-bottom: 2px solid #42a5f5; }
            p { margin: 6px 0; }
            .box { display: inline-block; width: 32px; height: 32px; border-radius: 6px; margin: 2px; }
            </style></head>
            <body>
            <h1>data: URL</h1>
            <p id="unicode">text/html, base64, UTF-8: é ü ß ✓</p>
            <svg width="200" height="60" xmlns="http://www.w3.org/2000/svg">
              <rect x="2" y="2" width="90" height="56" rx="10" fill="#1e88e5"/>
              <circle cx="140" cy="30" r="26" fill="#ffb300" stroke="#e65100" stroke-width="3"/>
              <text x="12" y="36" font-size="16" fill="white">SVG</text>
            </svg>
            <div><span class="box" style="background:#ef5350"></span><span class="box" style="background:#66bb6a"></span><span class="box" style="background:#ab47bc"></span><span class="box" style="background:#26c6da"></span></div>
            <p id="measure">The quick brown fox jumps over the lazy dog.</p>
            </body></html>
            """;

    private static final String USER_STYLESHEET = """
            body { background: #fff3e0 !important; color: #e65100 !important; border: 3px dashed #fb8c00; padding: 4px; }
            h1 { font-style: italic; color: #bf360c !important; border-bottom-color: #fb8c00 !important; }
            .box { border-radius: 50% !important; }
            """;

    private static final String CALLBACKS_PAGE = """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8"><title>Initial title</title>
            <style>
            body { margin: 0; padding: 9px 12px; font-family: Helvetica, Arial, 'Liberation Sans', sans-serif; font-size: 12px; color: #311b92;
                   background: linear-gradient(90deg, #ede7f6, #e0f7fa); white-space: nowrap; overflow: hidden; }
            span { display: inline-block; margin-left: 14px; padding: 3px 9px; border-radius: 10px; background: white;
                   border: 1px solid #9575cd; }
            </style></head>
            <body><b>JavaScript UI callbacks handled in Java:</b><span id="alert"></span><span id="confirm"></span><span id="prompt"></span><span id="title"></span>
            <script>
            alert('Hello from alert()');
            document.getElementById('alert').textContent = 'alert() → onAlert';
            document.getElementById('confirm').textContent = 'confirm() → ' + confirm('Proceed?');
            document.getElementById('prompt').textContent = 'prompt() → ' + prompt('Name?', 'default');
            document.title = 'Title set by script';
            document.getElementById('title').textContent = 'document.title → titleProperty';
            </script>
            </body></html>
            """;

    static final double ZOOM = 0.75;
    static final double FONT_SCALE = 1.4;

    @Override
    public String id() {
        return "web-loading";
    }

    @Override
    public String title() {
        return "Loading, style sheets & callbacks";
    }

    @Override
    public String category() {
        return Categories.WEB;
    }

    @Override
    public int order() {
        return 30;
    }

    private static final class Loaded {
        final WebView view;
        final WebEngine engine;
        Worker.State state;
        String outcome;
        final CompletionStage<Void> done;

        Loaded(double width, double height) {
            this(new WebView(), width, height);
        }

        /** A WebEngine without WebView. */
        Loaded() {
            this(null, 0, 0);
        }

        private Loaded(WebView view, double width, double height) {
            this.view = view;
            this.engine = view == null ? new WebEngine() : view.getEngine();
            if (view != null) {
                view.setContextMenuEnabled(false);
                view.setPrefSize(width, height);
                view.setMinSize(width, height);
                view.setMaxSize(width, height);
            }
            done = WebSupport.loaded(engine, 20_000).handle((loadState, error) -> {
                state = loadState;
                outcome = WebSupport.outcome(engine, loadState, error);
                return null;
            });
        }

        boolean ok() {
            return state == Worker.State.SUCCEEDED;
        }
    }

    private static final class State {
        final Loaded classpath = new Loaded(245, 280);
        final Loaded data = new Loaded(245, 280);
        final Loaded styled = new Loaded(245, 280);
        final Loaded zoomed = new Loaded(245, 280);
        final Loaded callbacks = new Loaded(1028, 40);
        final Loaded headless = new Loaded();
        final List<String> alerts = new ArrayList<>();
        final List<String> confirms = new ArrayList<>();
        final List<String> prompts = new ArrayList<>();
        final VBox loadChecks = new VBox();
        final VBox styleChecks = new VBox();
        String styleSheetError;
        CompletionStage<?> ready;

        List<Loaded> all() {
            return List.of(classpath, data, styled, zoomed, callbacks, headless);
        }
    }

    @Override
    public Node build() {
        State state = new State();
        String dataUrl = "data:text/html;charset=utf-8;base64," + WebSupport.base64(DATA_PAGE);
        String styleUrl = "data:text/css;charset=utf-8;base64," + WebSupport.base64(USER_STYLESHEET);

        try {
            state.styled.engine.setUserStyleSheetLocation(styleUrl);
        } catch (Throwable t) {
            state.styleSheetError = Checks.describe(t);
        }
        // zoom, font scale and font smoothing set by the page style sheet (WebView CSS properties)
        state.zoomed.view.getStyleClass().add("zoomed-view");

        WebEngine callbacks = state.callbacks.engine;
        callbacks.setOnAlert(event -> state.alerts.add(event.getData()));
        callbacks.setConfirmHandler(message -> {
            state.confirms.add(message);
            return true;
        });
        callbacks.setPromptHandler(prompt -> {
            state.prompts.add(prompt.getMessage() + " [" + prompt.getDefaultValue() + "]");
            return "Quarkus FX";
        });
        state.headless.engine.setJavaScriptEnabled(false);

        state.classpath.engine.load(Fx.resourceUrl(CLASSPATH_PAGE));
        state.data.engine.load(dataUrl);
        state.styled.engine.load(dataUrl);
        state.zoomed.engine.load(dataUrl);
        callbacks.loadContent(CALLBACKS_PAGE);
        state.headless.engine.load(Fx.resourceUrl(CLASSPATH_PAGE));

        HBox views = new HBox(16,
                column("load(classpath URL of loaded.html)", state.classpath.view),
                column("load(data:text/html;base64,...)", state.data.view),
                column("+ setUserStyleSheetLocation(data:)", state.styled.view),
                column("CSS -fx-zoom: " + ZOOM + ", -fx-font-scale: " + FONT_SCALE, state.zoomed.view));
        VBox callbacksBox = new VBox(4,
                WebSupport.caption("loadContent + onAlert, confirmHandler, promptHandler, title property"),
                state.callbacks.view);

        state.loadChecks.getChildren().add(new Label("Waiting for the pages to load..."));
        state.loadChecks.setPrefWidth(506);
        state.styleChecks.setPrefWidth(506);
        HBox.setHgrow(state.loadChecks, Priority.ALWAYS);
        HBox.setHgrow(state.styleChecks, Priority.ALWAYS);
        HBox checks = new HBox(16, state.loadChecks, state.styleChecks);

        VBox root = new VBox(10, views, callbacksBox, checks);
        root.getStylesheets().add(Fx.resourceUrl(STYLESHEET));
        root.getProperties().put(STATE, state);

        state.ready = CompletableFuture.allOf(state.all().stream().map(l -> l.done.toCompletableFuture())
                .toArray(CompletableFuture[]::new))
                .thenComposeAsync(v -> Fx.pulses(5), Fx.FX_THREAD)
                .thenApply(v -> {
                    showChecks(state);
                    return null;
                })
                .thenCompose(v -> Fx.pulses(10))
                // images are decoded and painted asynchronously by WebKit
                .thenCompose(v -> WebSupport.stable(root, 10_000))
                .thenCompose(v -> Fx.delay(200));
        return root;
    }

    private static VBox column(String caption, WebView view) {
        Label label = WebSupport.caption(caption);
        label.setMaxWidth(245);
        return new VBox(4, label, view);
    }

    private static Object script(Loaded loaded, String script) {
        if (!loaded.ok()) {
            throw new IllegalStateException("page not loaded : " + loaded.outcome);
        }
        return loaded.engine.executeScript(script);
    }

    private static void showChecks(State state) {
        List<Check> load = new ArrayList<>();
        Loaded cp = state.classpath;
        load.add(Check.of("classpath URL: load state", cp.ok(), cp.outcome));
        load.add(Checks.expect("classpath URL: location, title", "loaded.html, Loaded from the classpath",
                () -> cp.engine.getLocation().substring(cp.engine.getLocation().lastIndexOf('/') + 1) + ", "
                        + script(cp, "document.title")));
        load.add(Checks.expect("classpath URL: linked loaded.css", "rgb(46, 125, 50)",
                () -> script(cp, "getComputedStyle(document.querySelector('h1')).color")));
        load.add(Checks.expect("classpath URL: script loaded.js", "written by loaded.js",
                () -> script(cp, "document.getElementById('script-out').textContent")));
        load.add(Checks.expect("classpath URL: img ../images/pattern.png", "256x256",
                () -> script(cp, "var i = document.getElementById('img'); i.naturalWidth + 'x' + i.naturalHeight")));
        load.add(Checks.expect("classpath URL: WebHistory entries", 1, () -> cp.engine.getHistory().getEntries().size()));

        Loaded data = state.data;
        load.add(Check.of("data: URL: load state", data.ok(), data.outcome));
        load.add(Checks.expect("data: URL: document.title", "Loaded from a data: URL",
                () -> script(data, "document.title")));
        load.add(Checks.expect("data: URL: UTF-8 text", "text/html, base64, UTF-8: é ü ß ✓",
                () -> script(data, "document.getElementById('unicode').textContent")));

        Loaded headless = state.headless;
        load.add(Check.of("WebEngine without WebView: load", headless.ok(), headless.outcome));
        load.add(Checks.expect("JavaScript disabled: DOM text", "loaded.js did not run",
                () -> headless.engine.getDocument().getElementById("script-out").getTextContent()));

        List<Check> style = new ArrayList<>();
        Loaded styled = state.styled;
        Loaded zoomed = state.zoomed;
        style.add(Check.of("styled / zoomed: load state", styled.ok() && zoomed.ok(),
                styled.outcome + " / " + zoomed.outcome));
        style.add(state.styleSheetError != null
                ? Check.fail("setUserStyleSheetLocation(data:)", state.styleSheetError)
                : Checks.expect("setUserStyleSheetLocation(data:)", true,
                        () -> styled.engine.getUserStyleSheetLocation().startsWith("data:text/css")));
        style.add(Checks.expect("user style: body background", "rgb(255, 243, 224)",
                () -> script(styled, "getComputedStyle(document.body).backgroundColor")));
        style.add(Checks.expect("user style: h1 font-style", "italic",
                () -> script(styled, "getComputedStyle(document.querySelector('h1')).fontStyle")));
        style.add(Checks.expect("CSS zoom / fontScale / smoothing", ZOOM + " / " + FONT_SCALE + " / GRAY",
                () -> zoomed.view.getZoom() + " / " + zoomed.view.getFontScale() + " / "
                        + zoomed.view.getFontSmoothingType()));
        style.add(Checks.run("innerWidth at zoom 1 / " + ZOOM,
                () -> script(data, "window.innerWidth") + " / " + script(zoomed, "window.innerWidth")));
        style.add(Checks.run("text height at fontScale 1 / " + FONT_SCALE,
                () -> script(data, "document.getElementById('measure').offsetHeight") + " / "
                        + script(zoomed, "document.getElementById('measure').offsetHeight")));

        Loaded callbacks = state.callbacks;
        style.add(Check.of("callbacks page: load state", callbacks.ok(), callbacks.outcome));
        style.add(Checks.expect("onAlert", "[Hello from alert()]", state.alerts::toString));
        style.add(Checks.expect("confirmHandler", "[Proceed?] → true",
                () -> state.confirms + " → " + script(callbacks, "document.getElementById('confirm').textContent")
                        .toString().substring("confirm() → ".length())));
        style.add(Checks.expect("promptHandler", "[Name? [default]] → Quarkus FX",
                () -> state.prompts + " → " + script(callbacks, "document.getElementById('prompt').textContent")
                        .toString().substring("prompt() → ".length())));
        style.add(Checks.expect("WebEngine title property", "Title set by script", callbacks.engine::getTitle));

        state.loadChecks.getChildren().setAll(Checks.view("Loading", load));
        state.styleChecks.getChildren().setAll(Checks.view("User style sheet, zoom & callbacks", style));
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ((State) content.getProperties().get(STATE)).ready;
    }

    @Override
    public void dispose(Node content) {
        State state = (State) content.getProperties().get(STATE);
        if (state != null) {
            for (Loaded loaded : state.all()) {
                loaded.engine.loadContent("");
            }
        }
    }
}
