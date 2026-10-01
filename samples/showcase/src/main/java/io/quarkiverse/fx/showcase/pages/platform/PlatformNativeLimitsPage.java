package io.quarkiverse.fx.showcase.pages.platform;

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
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import io.quarkiverse.fx.showcase.pages.web.WebSupport;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Where a native image behaves differently from the JVM because of JavaFX itself, or of GraalVM (os.name) : the cause,
 * found in the JavaFX (or GraalVM) sources, and a workaround, which must work in both runtimes. A difference that
 * quarkus-fx removes stays here with its cause and workaround, and its check expects the JVM behavior in both runtimes.
 * <p>
 * This page is {@link #runtimeDependent() runtime dependent} : its differences between a JVM run and a native run are
 * reported as EXPECTED by tools/Compare.java. Its checks still fail when a runtime does not behave as described.
 */
@Singleton
public class PlatformNativeLimitsPage implements FeaturePage {

    private static final String STATE = PlatformNativeLimitsPage.class.getName();
    private static final String CSS = "/showcase/web/loaded.css";
    private static final String HTML = "<html><body><h1>User style sheet</h1>"
            + "<p class='lead'>h1 and body styled by loaded.css</p></body></html>";
    private static final String STYLED = "accepted, h1 color rgb(46, 125, 50)";
    private static final String START_PAGE = "/showcase/web/loaded.html";
    private static final String STORAGE_PAGE = "/showcase/web/storage.html";
    private static final String STORAGE_NAME = STORAGE_PAGE.substring(STORAGE_PAGE.lastIndexOf('/') + 1);
    private static final String OPAQUE = "sessionStorage: SecurityError, origin: null";

    @Override
    public String id() {
        return "platform-native-limits";
    }

    @Override
    public String title() {
        return "Native image limits";
    }

    @Override
    public String category() {
        return Categories.PLATFORM;
    }

    @Override
    public int order() {
        return 90;
    }

    @Override
    public boolean runtimeDependent() {
        return true;
    }

    /** A WebView whose engine is given a user style sheet location. */
    private static final class StyledView {
        final WebView view = new WebView();
        final String location;
        String error;
        CompletionStage<?> loaded;

        StyledView(String location) {
            this.location = location;
            view.setContextMenuEnabled(false);
            view.setPrefSize(300, 84);
            view.setMinSize(300, 84);
            view.setMaxSize(300, 84);
            try {
                view.getEngine().setUserStyleSheetLocation(location);
            } catch (Throwable t) {
                error = Checks.describe(t);
            }
            loaded = WebSupport.loaded(view.getEngine(), 20_000);
            view.getEngine().loadContent(HTML);
        }

        /** Whether the style sheet was accepted, and applied. */
        String outcome() {
            if (error != null) {
                return "rejected: " + error;
            }
            return "accepted, h1 color "
                    + view.getEngine().executeScript("getComputedStyle(document.querySelector('h1')).color");
        }
    }

    /**
     * A WebView showing a class path page, which then navigates to {@code storage.html} itself : a link to the given URL,
     * clicked by a script.
     */
    private static final class NavigatingView {
        final WebView view = new WebView();
        final CompletableFuture<Void> navigated = new CompletableFuture<>();
        final CompletionStage<?> loaded;

        NavigatingView(String href) {
            view.setContextMenuEnabled(false);
            view.setPrefSize(300, 64);
            view.setMinSize(300, 64);
            view.setMaxSize(300, 64);
            WebEngine engine = view.getEngine();
            engine.getLoadWorker().stateProperty().addListener((observable, previous, state) -> {
                if (engine.getLocation().endsWith(STORAGE_NAME) && (state == Worker.State.SUCCEEDED
                        || state == Worker.State.FAILED || state == Worker.State.CANCELLED)) {
                    navigated.complete(null);
                }
            });
            loaded = WebSupport.loaded(engine, 20_000)
                    .thenAccept(state -> engine.executeScript("var a = document.createElement('a'); a.href = '" + href
                            + "'; document.body.appendChild(a); a.click();"))
                    .thenCompose(v -> Fx.timeout(navigated, 20_000, "navigation to " + href));
            engine.load(Fx.resourceUrl(START_PAGE));
        }

        /** The session storage and the origin seen by the page. */
        String outcome() {
            return String.valueOf(view.getEngine().executeScript(
                    "document.getElementById('storage').textContent + ', ' + document.getElementById('origin').textContent"));
        }
    }

    private static final class State {
        StyledView classpath;
        StyledView data;
        NavigatingView absolute;
        NavigatingView relative;
        final VBox checks = new VBox(new Label("Waiting for the pages to load..."));
        final VBox navigationChecks = new VBox(new Label("Waiting for the pages to load..."));
        final VBox osChecks = PlatformUi.checks(null, List.of(
                Check.info("os.name", System.getProperty("os.name")),
                Check.info("workaround : os.version", System.getProperty("os.version"))), 300,
                PlatformUi.CONTENT_WIDTH - 18);
        CompletionStage<?> ready;
    }

    @Override
    public Node build() {
        State state = new State();
        String classpathUrl = Fx.resourceUrl(CSS);
        state.classpath = new StyledView(classpathUrl);
        state.data = new StyledView("data:text/css;charset=utf-8;base64," + WebSupport.base64(WebSupport.resourceBytes(CSS)));
        state.absolute = new NavigatingView(Fx.resourceUrl(STORAGE_PAGE));
        state.relative = new NavigatingView(STORAGE_NAME);

        VBox userStyleSheet = PlatformUi.demo(
                "WebEngine.setUserStyleSheetLocation(url) : a style sheet on the class path (handled by quarkus-fx)",
                PlatformUi.note("Cause : a class path resource is a jar: (or file:) URL on the JVM, and a resource: URL in "
                        + "a native image. The user style sheet location only accepts file:, jar:, jrt: and data: URLs : "
                        + "any other scheme throws IllegalArgumentException(\"Invalid stylesheet URL\") (javafx.web, "
                        + "javafx.scene.web.WebEngine, userStyleSheetLocation property). quarkus-fx accepts resource: URLs "
                        + "there in native executables : the class path URL is applied in both runtimes."),
                PlatformUi.note("Workaround without quarkus-fx : read the style sheet and give its content as a data: URL "
                        + "(data:text/css;charset=utf-8;base64,...), which is what WebEngine itself does with a file: or "
                        + "jar: URL."),
                new HBox(16,
                        column("setUserStyleSheetLocation(class path URL)", state.classpath.view),
                        column("workaround : data: URL of the same file", state.data.view)),
                state.checks);
        VBox navigation = PlatformUi.demo("A WebView page navigating to a class path URL itself (a link, location.href)",
                PlatformUi.note("Cause : WebKit gives an opaque origin to the pages of the URL schemes it does not know, "
                        + "except jar:file (WebCore SecurityOriginData::shouldTreatAsOpaqueOrigin) : a jar:file: URL of "
                        + "the JVM is a local origin, a resource: URL is not. quarkus-fx gives WebKit the resource: URL of "
                        + "a page loaded with WebEngine.load as a jar:file:/resource:/... URL, with the origin of the JVM, "
                        + "but a page that navigates to a resource: URL itself (here a link to the class path URL of "
                        + "storage.html, given by Java) gets an opaque origin : sessionStorage and localStorage throw "
                        + "SecurityError, fetch and XMLHttpRequest of its resources fail, location.origin is null."),
                PlatformUi.note("Workaround : relative URLs between class path pages (the same origin in both runtimes), or "
                        + "WebEngine.load of the class path URL from Java."),
                new HBox(16,
                        column("link to the class path URL of storage.html", state.absolute.view),
                        column("workaround : link to storage.html (relative URL)", state.relative.view)),
                state.navigationChecks);
        VBox osName = PlatformUi.demo("System.getProperty(\"os.name\") : computed by the native executable itself",
                PlatformUi.note("Cause : the JVM takes os.name from the native code of the JDK (java_props_md.c), a native "
                        + "executable from its own copy of that code (GraalVM, SubstrateVM WindowsSystemPropertiesSupport), "
                        + "whose list of Windows versions stops at Windows Server 2022 : on Windows Server 2025 (build "
                        + "26100), the JVM says \"Windows Server 2025\" and a native executable \"Windows Server 2022\". "
                        + "Windows 10 and 11, macOS and Linux get the same value in both runtimes."),
                PlatformUi.note("Workaround : the family from the start of os.name (os.name.startsWith(\"Windows\"), as "
                        + "JavaFX does). No system property gives the product name in both runtimes : os.version is "
                        + "\"10.0\" from Windows 10 and Windows Server 2016 on, the build number (the registry's "
                        + "CurrentBuildNumber) tells them apart."),
                state.osChecks);
        VBox root = PlatformUi.page(10,
                PlatformUi.note("JavaFX APIs, and a system property, that behave differently in a native image, with a "
                        + "workaround working in both runtimes. Differences between the JVM and native snapshots of this "
                        + "page are expected."),
                PlatformUi.width(userStyleSheet, PlatformUi.CONTENT_WIDTH),
                PlatformUi.width(navigation, PlatformUi.CONTENT_WIDTH),
                PlatformUi.width(osName, PlatformUi.CONTENT_WIDTH));
        root.getProperties().put(STATE, state);

        String scheme = classpathUrl.substring(0, classpathUrl.indexOf(':'));
        boolean nativeImage = ShowcaseMode.runtime().equals("NATIVE");
        // the origin of a class path page : jar:file: URLs (and the jar:file:/resource:/ URLs of quarkus-fx), file: URLs
        String local = "sessionStorage: stored, origin: " + (scheme.equals("file") ? "file://" : "jar:file://");
        state.ready = CompletableFuture.allOf(state.classpath.loaded.toCompletableFuture(),
                state.data.loaded.toCompletableFuture(), state.absolute.loaded.toCompletableFuture(),
                state.relative.loaded.toCompletableFuture())
                .thenComposeAsync(v -> Fx.pulses(5), Fx.FX_THREAD)
                .thenApply(v -> {
                    List<Check> checks = new ArrayList<>();
                    checks.add(Check.info("class path URL scheme", scheme));
                    checks.add(Checks.expect("setUserStyleSheetLocation(class path URL)", STYLED,
                            state.classpath::outcome));
                    checks.add(Checks.expect("workaround : setUserStyleSheetLocation(data: URL)", STYLED,
                            state.data::outcome));
                    state.checks.getChildren().setAll(PlatformUi.checks(null, checks, 300,
                            PlatformUi.CONTENT_WIDTH - 18));
                    List<Check> navigationChecks = new ArrayList<>();
                    navigationChecks.add(Checks.expect("navigation to the class path URL", nativeImage ? OPAQUE : local,
                            state.absolute::outcome));
                    navigationChecks.add(Checks.expect("workaround : navigation to a relative URL", local,
                            state.relative::outcome));
                    state.navigationChecks.getChildren().setAll(PlatformUi.checks(null, navigationChecks, 300,
                            PlatformUi.CONTENT_WIDTH - 18));
                    return null;
                })
                .thenCompose(v -> Fx.pulses(5))
                .thenCompose(v -> WebSupport.stable(root, 10_000))
                .thenCompose(v -> Fx.delay(200));
        return root;
    }

    private static VBox column(String caption, WebView view) {
        return new VBox(4, WebSupport.caption(caption), view);
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ((State) content.getProperties().get(STATE)).ready;
    }

    @Override
    public void dispose(Node content) {
        State state = (State) content.getProperties().get(STATE);
        if (state != null) {
            state.classpath.view.getEngine().loadContent("");
            state.data.view.getEngine().loadContent("");
            state.absolute.view.getEngine().loadContent("");
            state.relative.view.getEngine().loadContent("");
        }
    }
}
