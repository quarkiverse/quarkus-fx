package io.quarkiverse.fx.showcase.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Renders every page to a PNG file and writes a report (checks and errors) : the output of a JVM run and of a native
 * run are compared by tools/Compare.java.
 */
@Singleton
public class SnapshotRunner {

    private static final Logger LOG = Logger.getLogger(SnapshotRunner.class);

    @ConfigProperty(name = "showcase.snapshot.dir")
    Optional<String> dir;

    @ConfigProperty(name = "showcase.snapshot.pages")
    Optional<List<String>> pageFilter;

    @ConfigProperty(name = "showcase.snapshot.exit", defaultValue = "true")
    boolean exit;

    @ConfigProperty(name = "showcase.snapshot.settle-millis", defaultValue = "400")
    int settleMillis;

    @ConfigProperty(name = "showcase.snapshot.ready-timeout-seconds", defaultValue = "30")
    int readyTimeoutSeconds;

    @ConfigProperty(name = "showcase.snapshot.scale", defaultValue = "1")
    double scale;

    private final Map<String, List<String>> uncaught = Collections.synchronizedMap(new LinkedHashMap<>());
    private volatile String currentPageId = "_startup";

    void onStartup(@Observes StartupEvent event) {
        if (enabled()) {
            ShowcaseMode.enableSnapshot();
            Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                uncaught.computeIfAbsent(currentPageId, id -> Collections.synchronizedList(new ArrayList<>()))
                        .add(thread.getName() + ": " + Checks.describe(error));
                if (previous != null) {
                    previous.uncaughtException(thread, error);
                } else {
                    error.printStackTrace();
                }
            });
        }
    }

    public boolean enabled() {
        return dir.isPresent();
    }

    public void run(MainView view, Stage stage, List<FeaturePage> pages) {
        Path out = Path.of(dir.orElseThrow()).toAbsolutePath();
        List<FeaturePage> selected = pages.stream()
                .filter(page -> pageFilter.map(filters -> filters.stream().anyMatch(f -> page.id().startsWith(f.trim())))
                        .orElse(true))
                .toList();
        LOG.infof("Snapshot run of %d pages into %s", selected.size(), out);
        // no hover effect, whatever the mouse position
        view.pageFrame().setMouseTransparent(true);

        List<Map<String, Object>> results = new ArrayList<>();
        CompletionStage<Void> chain = Fx.pulses(5)
                .thenCompose(v -> Fx.delay(settleMillis))
                .thenCompose(v -> Fx.pulses(2))
                .thenRunAsync(() -> {
                    // limited to the scene : focus rings may be painted outside of it
                    SnapshotParameters parameters = parameters();
                    parameters.setViewport(new Rectangle2D(0, 0, stage.getScene().getWidth() * scale,
                            stage.getScene().getHeight() * scale));
                    writeImage(stage.getScene().getRoot().snapshot(parameters, null), out.resolve("_main-window.png"));
                }, Fx.FX_THREAD);
        for (FeaturePage page : selected) {
            chain = chain.thenComposeAsync(v -> capture(view, page, out), Fx.FX_THREAD).thenAccept(results::add);
        }
        chain.whenCompleteAsync((v, error) -> {
            if (error != null) {
                LOG.error("Snapshot run aborted", error);
            }
            view.clear();
            writeReport(out, results);
            long failed = results.stream().filter(r -> !((List<?>) r.get("errors")).isEmpty()).count();
            LOG.infof("Snapshot run finished : %d pages, %d with errors", results.size(), failed);
            if (exit) {
                // quarkus-fx exits JavaFX once it has detached from it (Platform::exit before would discard that work)
                Quarkus.asyncExit();
            }
        }, Fx.FX_THREAD);
    }

    private CompletionStage<Map<String, Object>> capture(MainView view, FeaturePage page, Path out) {
        currentPageId = page.id();
        long start = System.nanoTime();
        List<String> errors = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", page.id());
        result.put("title", page.title());
        result.put("category", page.category());
        result.put("runtimeDependent", page.runtimeDependent());

        view.select(page);
        Node content = view.currentContent();
        Throwable buildError = view.currentError();
        if (buildError != null) {
            errors.add("build: " + Checks.describe(buildError));
        }

        CompletionStage<?> ready;
        try {
            ready = buildError == null ? page.ready(content) : CompletableFuture.completedFuture(null);
        } catch (Throwable t) {
            ready = CompletableFuture.failedFuture(t);
        }

        return Fx.timeout(ready, readyTimeoutSeconds * 1000.0, "ready")
                .handle((v, error) -> {
                    if (error != null) {
                        errors.add("ready: " + Checks.describe(unwrap(error)));
                    }
                    return null;
                })
                .thenCompose(v -> Fx.pulses(3))
                .thenCompose(v -> Fx.delay(settleMillis))
                .thenCompose(v -> Fx.pulses(2))
                .thenCompose(v -> {
                    // a page may have requested the focus while getting ready
                    view.releaseFocus();
                    return Fx.pulses(2);
                })
                .thenComposeAsync(v -> {
                    WritableImage image = view.pageFrame().snapshot(parameters(), null);
                    String file = page.id() + ".png";
                    writeImage(image, out.resolve(file));
                    result.put("snapshot", file);
                    result.put("width", (int) image.getWidth());
                    result.put("height", (int) image.getHeight());

                    CompletionStage<Map<String, Image>> extras;
                    try {
                        extras = buildError == null ? page.extraSnapshots(content)
                                : CompletableFuture.completedFuture(Map.of());
                    } catch (Throwable t) {
                        extras = CompletableFuture.failedFuture(t);
                    }
                    return Fx.timeout(extras, readyTimeoutSeconds * 1000.0, "extra snapshots");
                }, Fx.FX_THREAD)
                .handleAsync((extras, error) -> {
                    List<String> extraFiles = new ArrayList<>();
                    if (error != null) {
                        errors.add("extraSnapshots: " + Checks.describe(unwrap(error)));
                    } else {
                        extras.forEach((name, image) -> {
                            String file = page.id() + "--" + name + ".png";
                            writeImage(image, out.resolve(file));
                            extraFiles.add(file);
                        });
                    }
                    Collections.sort(extraFiles);
                    result.put("extras", extraFiles);
                    List<Map<String, Object>> checks = new ArrayList<>();
                    for (Check check : content == null ? List.<Check> of() : Checks.collect(content)) {
                        Map<String, Object> c = new LinkedHashMap<>();
                        c.put("name", check.name());
                        c.put("value", check.value());
                        c.put("ok", check.ok());
                        checks.add(c);
                        if (Boolean.FALSE.equals(check.ok())) {
                            errors.add("check failed: " + check.name() + " = " + check.value());
                        }
                    }
                    List<String> pageUncaught = uncaught.getOrDefault(page.id(), List.of());
                    pageUncaught.forEach(e -> errors.add("uncaught: " + e));
                    result.put("checks", checks);
                    result.put("errors", errors);
                    result.put("millis", (System.nanoTime() - start) / 1_000_000);
                    LOG.infof("%-40s %s", page.id(), errors.isEmpty() ? "ok" : errors.size() + " error(s)");
                    return result;
                }, Fx.FX_THREAD);
    }

    private SnapshotParameters parameters() {
        SnapshotParameters parameters = new SnapshotParameters();
        if (scale != 1) {
            parameters.setTransform(javafx.scene.transform.Transform.scale(scale, scale));
        }
        return parameters;
    }

    private static Throwable unwrap(Throwable error) {
        while (error instanceof java.util.concurrent.CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    private static void writeImage(Image image, Path file) {
        try {
            PngWriter.write(image, file);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private void writeReport(Path out, List<Map<String, Object>> pages) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("runtime", ShowcaseMode.runtime());
        report.put("javafxVersion", System.getProperty("javafx.runtime.version"));
        report.put("javaVersion", System.getProperty("java.version"));
        report.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " "
                + System.getProperty("os.arch"));
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getBounds();
        report.put("screen", bounds.getWidth() + "x" + bounds.getHeight() + " @" + screen.getOutputScaleX() + "x, "
                + screen.getDpi() + " dpi");
        report.put("pipeline", ShowcaseMode.graphicsPipeline());
        Map<String, Object> features = new LinkedHashMap<>();
        for (ConditionalFeature feature : ConditionalFeature.values()) {
            features.put(feature.name(), Platform.isSupported(feature));
        }
        report.put("conditionalFeatures", features);
        report.put("pages", pages);
        Map<String, Object> other = new LinkedHashMap<>(uncaught);
        pages.forEach(p -> other.remove((String) p.get("id")));
        report.put("uncaughtOutsidePages", other);
        try {
            Files.writeString(out.resolve("report.json"), Json.write(report), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to write report", e);
        }
    }
}
