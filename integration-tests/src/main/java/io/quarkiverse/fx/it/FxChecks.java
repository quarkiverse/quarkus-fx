package io.quarkiverse.fx.it;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkiverse.fx.views.FxViewData;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.Quarkus;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The checks of the integration tests, run once Quarkus FX has started the JavaFX application.
 * <p>
 * They run on their own thread, each one using JavaFX on the JavaFX application thread ({@link #onFx}) and waiting for
 * it with a timeout : the JavaFX application thread is never blocked. Each check prints
 * {@code RESULT <check> OK <details>} (details : {@code key=value} words) or {@code RESULT <check> FAILED <exception>},
 * or {@code RESULT <check> SKIPPED <reason>}, then {@code SUMMARY ok=<n> skipped=<n> failed=<n> [<failed checks>]} is
 * printed and the application exits with 0, or 1 when a check failed.
 */
@ApplicationScoped
public class FxChecks {

    static final String FX = "fx";

    static final String FAIL = "fail";

    static final Set<String> SCENARIOS = Set.of(FX, FAIL);

    /**
     * A class path directory whose name has spaces and a non-ASCII character (U+00E9, NFC).
     */
    static final String ENCODED_DIRECTORY = "assets/space and café/";

    static final String IMAGE = "image café.png";

    /**
     * The end of the URL of the image in a native executable : percent-encoded as the JDK encodes jar: URLs, the
     * non-ASCII characters as UTF-8 escapes (quarkus-fx ResourceUrlSubstitutions).
     */
    static final String ENCODED_IMAGE_URL_END = "assets/space%20and%20caf%C3%A9/image%20caf%C3%A9.png";

    // The colors (ARGB) of the resources
    static final int IMAGE_ARGB = 0xffe04040; // assets/space and café/image café.png
    static final int TILE_ARGB = 0xfff0a30a; // views/images/tile.png
    static final int FILL_ARGB = 0xff2e7d32; // assets/space and café/style sheet.css
    static final int ACCENT_ARGB = 0xff0096c9; // FxPalette.ACCENT, views/Main.css

    static final long TIMEOUT_SECONDS = 60;

    @Inject
    FxViewRepository viewRepository;

    @Inject
    HostServices hostServices;

    @Inject
    Instance<FXMLLoader> fxmlLoaders;

    @Inject
    FxThreadProbe fxThreadProbe;

    private final CompletableFuture<Stage> started = new CompletableFuture<>();

    private final List<String> failures = new ArrayList<>();

    // The web views of the checks : referenced until the application exits
    private final List<WebView> webViews = new ArrayList<>();

    private int ok;

    private int skipped;

    /**
     * Fired by Quarkus FX on the JavaFX application thread, once the application has started and the FXML views are
     * loaded.
     */
    void onPostStartup(@Observes FxPostStartupEvent event) {
        started.complete(event.getPrimaryStage());
    }

    void start(String scenario) {
        String runThread = Thread.currentThread().getName();
        Thread thread = new Thread(() -> run(scenario, runThread), "fx-it-checks");
        // does not keep the JVM running : the JVM mode tests run the application in the JVM of the tests
        thread.setDaemon(true);
        thread.start();
    }

    private void run(String scenario, String runThread) {
        Stage stage = awaitStartup();
        if (stage == null) {
            exit(1);
            return;
        }
        check("environment", () -> onFx(() -> environment(runThread)));
        if (FAIL.equals(scenario)) {
            check("deliberate-failure", () -> {
                throw new IllegalStateException("the failing check of the fail scenario");
            });
        } else {
            check("static-initializer", () -> onFx(FxChecks::staticInitializer));
            check("fx-view", () -> onFx(() -> fxView(stage)));
            check("stage", () -> onFx(() -> stage(stage)));
            check("stylesheet", () -> onFx(this::stylesheet));
            check("fxml-loader", () -> onFx(this::fxmlLoader));
            check("encoded-resource-names", FxChecks::encodedResourceNames);
            check("encoded-stylesheet", () -> onFx(FxChecks::encodedStylesheet));
            check("image", () -> onFx(FxChecks::image));
            check("host-services", this::hostServicesCheck);
            check("run-on-fx-thread", this::runOnFxThread);
            String webViewUnsupported = webViewUnsupported();
            if (webViewUnsupported != null) {
                skip("webview", webViewUnsupported);
                skip("webview-missing-page", webViewUnsupported);
            } else {
                check("webview", this::webView);
                check("webview-missing-page", this::webViewMissingPage);
            }
        }
        System.out.println("SUMMARY ok=" + ok + " skipped=" + skipped + " failed=" + failures.size() + " " + failures);
        exit(failures.isEmpty() ? 0 : 1);
    }

    private Stage awaitStartup() {
        try {
            return started.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("RESULT startup FAILED JavaFX not started in " + TIMEOUT_SECONDS + " s : " + e);
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------------ checks

    private static Object environment(String runThread) {
        require(Platform.isFxApplicationThread(), "not run on the JavaFX application thread");
        return "mode=" + (ImageMode.current().isNativeImage() ? "native" : "jvm")
                + " os=" + System.getProperty("os.name").replace(' ', '_')
                + " arch=" + System.getProperty("os.arch")
                + " javafx=" + System.getProperty("javafx.version")
                + " runThread=" + runThread.replace(' ', '_')
                + " screens=" + Screen.getScreens().size()
                + " outputScale=" + Screen.getPrimary().getOutputScaleX();
    }

    /**
     * A class creating JavaFX objects in its static initializer : initialized at run time in a native executable.
     */
    private static Object staticInitializer() {
        require(argb(FxPalette.ACCENT) == ACCENT_ARGB, "FxPalette.ACCENT " + FxPalette.ACCENT);
        require(FxPalette.TITLE.getSize() == 16, "FxPalette.TITLE " + FxPalette.TITLE);
        return "accent=" + FxPalette.ACCENT + " title=" + FxPalette.TITLE.getFamily().replace(' ', '_');
    }

    /**
     * The view views/Main.fxml of the @FxView MainController, loaded by Quarkus FX at startup.
     */
    private Object fxView(Stage stage) {
        require(viewRepository.getPrimaryStage() == stage, "primary stage " + viewRepository.getPrimaryStage());
        FxViewData view = viewRepository.getViewData(MainController.VIEW);
        require(view != null, "view " + MainController.VIEW + " not loaded");
        Parent root = view.getRootNode();
        MainController controller = view.getController();
        require(controller != null && controller.greeting != null, "controller " + controller);
        // %greeting : the resource bundle views/Main.properties
        require("Main view".equals(controller.greeting.getText()), "greeting " + controller.greeting.getText());
        // @FXML initialize, with a bean injected in the controller
        require("Hello Main view".equals(controller.initializedWith), "initialized with " + controller.initializedWith);
        // <Image url="@images/quarkus-logo.png"/> : relative to the views root
        Image logo = controller.logo.getImage();
        require(logo != null && !logo.isError() && logo.getWidth() == 450 && logo.getHeight() == 273,
                "logo " + (logo == null ? null : logo.getUrl() + " " + logo.getException()));
        return "root=" + root.getClass().getSimpleName() + " controller=" + controller.getClass().getSimpleName()
                + " logo=" + (int) logo.getWidth() + "x" + (int) logo.getHeight() + " logoUrl=" + scheme(logo.getUrl());
    }

    /**
     * The primary stage shows the view : a window of the platform.
     */
    private Object stage(Stage stage) {
        Parent root = viewRepository.getViewData(MainController.VIEW).getRootNode();
        stage.setTitle("Quarkus FX integration tests");
        stage.setScene(new Scene(root, 480, 360));
        stage.setX(40);
        stage.setY(40);
        stage.show();
        require(stage.isShowing(), "the primary stage is not showing");
        return "size=" + (int) stage.getWidth() + "x" + (int) stage.getHeight() + " outputScale="
                + stage.getOutputScaleX();
    }

    /**
     * The stylesheet of the view (stylesheets="@Main.css"), and a url() image relative to it.
     */
    private Object stylesheet() {
        FxViewData view = viewRepository.getViewData(MainController.VIEW);
        Parent root = view.getRootNode();
        MainController controller = view.getController();
        root.applyCss();
        require(root.getStylesheets().size() == 1, "stylesheets " + root.getStylesheets());
        // JavaFX only applies a stylesheet whose URL is a valid URI
        URI stylesheet = URI.create(root.getStylesheets().get(0));
        Paint textFill = controller.greeting.getTextFill();
        require(textFill instanceof Color color && argb(color) == ACCENT_ARGB, "text fill of the greeting " + textFill);
        Image tile = backgroundImage(controller.tile);
        requireColor(tile, 8, 8, TILE_ARGB, "url() image of Main.css");
        return "stylesheetUrl=" + stylesheet.getScheme() + " textFill=" + textFill + " tile=" + (int) tile.getWidth()
                + "x" + (int) tile.getHeight();
    }

    /**
     * The injected FXMLLoader, with a controller that is a bean (registered for reflection : not an @FxView).
     */
    private Object fxmlLoader() throws IOException {
        FXMLLoader loader = fxmlLoaders.get();
        StackPane pane;
        try (InputStream in = resource("assets/form.fxml").openStream()) {
            pane = loader.load(in);
        }
        FormController controller = loader.getController();
        require(controller != null && controller.message != null, "controller " + controller);
        require("Hello FXMLLoader".equals(controller.message.getText()), "message " + controller.message.getText());
        return "root=" + pane.getClass().getSimpleName() + " message=" + controller.message.getText().replace(' ', '_');
    }

    /**
     * The URL of a resource whose name has spaces and a non-ASCII character : a valid URI, and it opens, as in JVM mode
     * (a native executable without quarkus-fx left the name as it is in resource: URLs).
     */
    private static Object encodedResourceNames() throws Exception {
        String name = ENCODED_DIRECTORY + IMAGE;
        URL url = resource(name);
        String external = url.toExternalForm();
        require(!external.contains(" "), "unencoded space in " + external);
        URI uri = url.toURI();
        // compared in NFC : the file names of macOS may be decomposed (NFD) in the file: URLs of JVM mode
        String path = Normalizer.normalize(uri.getSchemeSpecificPart(), Normalizer.Form.NFC);
        require(path.endsWith("/" + name), "URI " + uri + " is not the one of " + name);
        if (ImageMode.current().isNativeImage()) {
            require(external.toUpperCase(Locale.ROOT).endsWith(ENCODED_IMAGE_URL_END.toUpperCase(Locale.ROOT)),
                    "resource URL " + external);
        }
        // the connections decode the escapes : the URL, the URL of its URI, a URL resolved against another one
        requirePng(url);
        requirePng(uri.toURL());
        requirePng(new URL(resource(ENCODED_DIRECTORY + "page.html"), "image%20caf%C3%A9.png"));
        return "urlScheme=" + url.getProtocol() + " url=" + external.substring(external.lastIndexOf("assets/"));
    }

    /**
     * A stylesheet whose URL has spaces and a non-ASCII character, with a url() image, rendered.
     */
    private static Object encodedStylesheet() {
        URL css = resource(ENCODED_DIRECTORY + "style sheet.css");
        Region region = new Region();
        region.getStyleClass().add("encoded-name");
        Group group = new Group(region);
        Scene scene = new Scene(group);
        scene.getStylesheets().add(css.toExternalForm());
        group.applyCss();
        group.layout();
        Background background = region.getBackground();
        require(background != null && !background.getFills().isEmpty(), "stylesheet not applied : " + css);
        Paint fill = background.getFills().get(0).getFill();
        require(fill instanceof Color color && argb(color) == FILL_ARGB, "background color " + fill);
        Image image = backgroundImage(region);
        requireColor(image, 8, 8, IMAGE_ARGB, "url() image of the stylesheet");
        // rendered : the image, over the background color
        WritableImage snapshot = region.snapshot(new SnapshotParameters(), null);
        require(snapshot.getWidth() == 32 && snapshot.getHeight() == 32,
                "snapshot " + snapshot.getWidth() + "x" + snapshot.getHeight());
        requireColor(snapshot, 4, 4, IMAGE_ARGB, "snapshot of the background image");
        requireColor(snapshot, 24, 24, FILL_ARGB, "snapshot of the background color");
        return "size=" + (int) region.getWidth() + "x" + (int) region.getHeight() + " image=" + (int) image.getWidth()
                + "x" + (int) image.getHeight();
    }

    /**
     * Images of a resource whose name has spaces and a non-ASCII character : from its URL, its stream, its name.
     */
    private static Object image() throws IOException {
        URL url = resource(ENCODED_DIRECTORY + IMAGE);
        Image fromUrl = new Image(url.toExternalForm());
        requireColor(fromUrl, 8, 8, IMAGE_ARGB, "Image of " + url);
        Image fromStream;
        try (InputStream in = url.openStream()) {
            fromStream = new Image(in);
        }
        requireColor(fromStream, 8, 8, IMAGE_ARGB, "Image of the stream of " + url);
        // a URL without a scheme : a class path resource, looked up with the context class loader
        Image fromName = new Image(ENCODED_DIRECTORY + IMAGE);
        requireColor(fromName, 8, 8, IMAGE_ARGB, "Image of the class path name");
        return "size=" + (int) fromUrl.getWidth() + "x" + (int) fromUrl.getHeight() + " argb="
                + Integer.toHexString(fromUrl.getPixelReader().getArgb(8, 8));
    }

    /**
     * The injected HostServices : in a native executable, the code base is the directory of the executable (a
     * NullPointerException without quarkus-fx).
     */
    private Object hostServicesCheck() {
        String codeBase = hostServices.getCodeBase();
        String documentBase = hostServices.getDocumentBase();
        require(codeBase != null, "no code base");
        if (ImageMode.current().isNativeImage()) {
            require(codeBase.startsWith("file:") && codeBase.endsWith("/"), "code base " + codeBase);
        }
        return "codeBase=" + (codeBase.isEmpty() ? "<empty>" : codeBase) + " documentBase=" + documentBase;
    }

    /**
     * A @RunOnFxThread method called from another thread : run later on the JavaFX application thread.
     */
    private Object runOnFxThread() throws Exception {
        CompletableFuture<String> thread = new CompletableFuture<>();
        fxThreadProbe.record(thread);
        String name = await(thread);
        require(!name.startsWith(FxThreadProbe.NOT_FX), name);
        return "thread=" + name.replace(' ', '_');
    }

    /**
     * @return why WebView cannot run, or null : in a macOS native executable, libjfxwebkit.dylib links libjvm.dylib
     *         (without using it), which GraalVM only writes next to the executable when it needs the libraries of the
     *         JDK (AWT, with Quarkus Desktop)
     */
    private static String webViewUnsupported() {
        if (ImageMode.current().isNativeImage() && System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            return "reason=no_libjvm.dylib_for_libjfxwebkit.dylib_in_macOS_native_executables";
        }
        return null;
    }

    /**
     * A class path page in the directory with spaces and a non-ASCII character, with a script and an image relative to
     * it.
     */
    private Object webView() throws Exception {
        URL page = resource(ENCODED_DIRECTORY + "page.html");
        CompletableFuture<Worker.State> loaded = new CompletableFuture<>();
        WebEngine engine = onFx(() -> load(page.toExternalForm(), loaded));
        Worker.State state = await(loaded);
        if (state != Worker.State.SUCCEEDED) {
            throw new IllegalStateException("load of " + page + " : " + state + " "
                    + onFx(() -> String.valueOf(engine.getLoadWorker().getException())));
        }
        // WebKit may give the title after the end of the load
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (onFx(engine::getTitle) == null && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        return onFx(() -> {
            String title = engine.getTitle();
            // what WebKit loaded, in the message of a failure
            String webKit = " location " + engine.getLocation() + " document "
                    + engine.executeScript("document.URL + ' ' + document.documentElement.outerHTML.length");
            require("Quarkus FX WebView".equals(title), "title " + title + webKit);
            Object script = engine.executeScript("document.getElementById('script').textContent");
            Object imageWidth = engine.executeScript("document.getElementById('image').naturalWidth");
            require("loaded by page.js".equals(script), "script " + script + webKit);
            require(imageWidth instanceof Number width && width.intValue() == 16, "image width " + imageWidth + webKit);
            return "title=" + title.replace(' ', '_') + " imageWidth=" + imageWidth + " pageLocation="
                    + scheme(engine.getLocation());
        });
    }

    /**
     * A missing class path page fails to load, as in JVM mode (a native executable without quarkus-fx loaded it as an
     * empty page).
     */
    private Object webViewMissingPage() throws Exception {
        URL missing = new URL(resource(ENCODED_DIRECTORY + "page.html"), "missing.html");
        CompletableFuture<Worker.State> loaded = new CompletableFuture<>();
        onFx(() -> load(missing.toExternalForm(), loaded));
        Worker.State state = await(loaded);
        require(state == Worker.State.FAILED, "load of the missing page " + missing + " : " + state);
        return "state=" + state.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Loads the URL in a new web view (on the JavaFX application thread) : the future completes with the final state of
     * the load.
     */
    private WebEngine load(String url, CompletableFuture<Worker.State> done) {
        WebView webView = new WebView();
        webViews.add(webView);
        WebEngine engine = webView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((observable, previous, state) -> {
            if (state == Worker.State.SUCCEEDED || state == Worker.State.FAILED || state == Worker.State.CANCELLED) {
                done.complete(state);
            }
        });
        engine.load(url);
        return engine;
    }

    // ----------------------------------------------------------------------------------------------------- support

    private void check(String name, Callable<Object> check) {
        try {
            Object value = check.call();
            System.out.println("RESULT " + name + " OK " + value);
            ok++;
        } catch (Throwable t) {
            System.out.println("RESULT " + name + " FAILED " + t);
            t.printStackTrace(System.out);
            failures.add(name);
        }
    }

    private void skip(String name, String reason) {
        System.out.println("RESULT " + name + " SKIPPED " + reason);
        skipped++;
    }

    /**
     * Runs the task on the JavaFX application thread and waits for its result.
     */
    private static <T> T onFx(Callable<T> task) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(task.call());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        return await(result);
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    /**
     * Exits with the code : QuarkusFxApplication.run returns 0 once Quarkus exits, and the first exit code wins.
     */
    private static void exit(int code) {
        System.out.flush();
        Quarkus.asyncExit(code);
        Platform.exit();
        if (ImageMode.current().isNativeImage()) {
            // an executable that does not exit fails its test instead of hanging it (no timeout in the test launcher)
            Thread watchdog = new Thread(() -> {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
                } catch (InterruptedException e) {
                    return;
                }
                System.out.println("EXIT not done in " + TIMEOUT_SECONDS + " s : halting");
                System.out.flush();
                Runtime.getRuntime().halt(3);
            }, "fx-it-exit-watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
        }
    }

    /**
     * A class path resource, looked up as JavaFX does (the context class loader).
     */
    static URL resource(String name) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        require(url != null, "resource " + name + " not found");
        return url;
    }

    private static void requirePng(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            byte[] header = in.readNBytes(4);
            require(header.length == 4 && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N'
                    && header[3] == 'G', "not a PNG image : " + url);
        }
    }

    private static Image backgroundImage(Region region) {
        Background background = region.getBackground();
        require(background != null && !background.getImages().isEmpty(),
                "no background image of " + region.getStyleClass());
        return background.getImages().get(0).getImage();
    }

    private static void requireColor(Image image, int x, int y, int expected, String what) {
        require(image != null && !image.isError(),
                what + " : not loaded " + (image == null ? "" : image.getUrl() + " " + image.getException()));
        require(image.getWidth() > x && image.getHeight() > y,
                what + " : size " + image.getWidth() + "x" + image.getHeight());
        int actual = image.getPixelReader().getArgb(x, y);
        for (int shift = 0; shift < 32; shift += 8) {
            require(Math.abs(((actual >> shift) & 0xff) - ((expected >> shift) & 0xff)) <= 2, what + " : pixel (" + x
                    + "," + y + ") is " + Integer.toHexString(actual) + ", expected " + Integer.toHexString(expected));
        }
    }

    private static int argb(Color color) {
        return (int) Math.round(color.getOpacity() * 255) << 24 | (int) Math.round(color.getRed() * 255) << 16
                | (int) Math.round(color.getGreen() * 255) << 8 | (int) Math.round(color.getBlue() * 255);
    }

    private static String scheme(String url) {
        int colon = url == null ? -1 : url.indexOf(':');
        return colon < 0 ? String.valueOf(url) : url.substring(0, colon);
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
