package io.quarkiverse.fx.showcase.pages.fxml;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.MainView;
import io.quarkiverse.fx.views.FxViewData;
import io.quarkiverse.fx.views.FxViewRepository;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * quarkus-fx features : a conventional {@code @FxView} retrieved from the {@link FxViewRepository},
 * {@code @RunOnFxThread}, the {@link FxStartupLatch}, the injected {@link HostServices} and the startup events.
 */
@Singleton
public class FxViewPage implements FeaturePage {

    private static final double CONTENT_WIDTH = MainView.PAGE_WIDTH - 32;
    private static final String VIEW = "ShowcaseView";
    private static final String STATUS_VIEW = "StatusCard";
    private static final double BOX_WIDTH = Math.floor((CONTENT_WIDTH - 24) / 3);
    private static final String READY = "fxml-fxview.ready";
    private static final String EXECUTOR = "fxml-fxview.executor";

    @Inject
    FxViewRepository repository;

    @Inject
    Instance<FXMLLoader> loaders;

    @Inject
    FxThreadProbe fxThreadProbe;

    @Inject
    FxStartupLatch startupLatch;

    @Inject
    HostServices hostServices;

    @Inject
    FxLifecycleRecorder lifecycle;

    @Override
    public String id() {
        return "fxml-fxview";
    }

    @Override
    public String title() {
        return "@FxView, @RunOnFxThread & HostServices";
    }

    @Override
    public String category() {
        return Categories.FXML;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        List<Check> checks = new ArrayList<>();

        // 1. the conventional view, loaded at startup by quarkus-fx
        FxViewData data = repository.getViewData(VIEW);
        checks.add(Checks.expect("getViewData(\"" + VIEW + "\")", true, () -> data != null));
        VBox repositoryBox = FxmlUi.demo("getViewData(\"ShowcaseView\") : loaded at startup");
        ShowcaseViewController repositoryController = null;
        if (data != null) {
            Parent root = data.getRootNode();
            repositoryController = data.getController();
            // the same node is shown every time the page is built : it moves to the new parent
            repositoryBox.getChildren().add(root);
            ShowcaseViewController c = repositoryController;
            checks.add(Checks.expect("view root / controller types", "VBox / ShowcaseViewController",
                    () -> root.getClass().getSimpleName() + " / " + c.getClass().getSimpleName()));
            checks.add(Checks.expect("controller initialize() + @FXML fields", "initialized, root field == root node",
                    () -> (c.initialized ? "initialized" : "not initialized") + ", root field == root node"
                            + (c.root == root ? "" : " FAILED")));
            checks.add(Checks.expect("bundle fxviews/ShowcaseView.properties", "@FxView ShowcaseView / 9 keys",
                    () -> c.resources.getString("view.title") + " / " + c.resources.keySet().size() + " keys"));
            checks.add(Checks.expect("FXMLLoader location = views root", true,
                    () -> c.location != null && c.location.toExternalForm().endsWith("fxviews/")));
            checks.add(Checks.expect("stylesheets=\"@ShowcaseView.css\"", "ShowcaseView.css",
                    () -> root.getStylesheets().stream().map(s -> s.substring(s.lastIndexOf('/') + 1)).toList()
                            .getFirst()));
            checks.add(Checks.expect("getPrimaryStage() == event stage", true,
                    () -> repository.getPrimaryStage() != null && repository.getPrimaryStage() == lifecycle.primaryStage));
        } else {
            repositoryBox.getChildren().add(error("No view data for " + VIEW));
        }

        // 2. the same FXML loaded again with a fresh CDI FXMLLoader : a new @Dependent controller
        VBox freshBox = FxmlUi.demo("Same FXML, fresh Instance<FXMLLoader>, fired 3x");
        try {
            FXMLLoader loader = loaders.get();
            loader.setLocation(Fx.resource("/fxviews/ShowcaseView.fxml"));
            ResourceBundle bundle = repositoryController != null ? repositoryController.resources
                    : FxmlUi.bundle("fxviews.ShowcaseView", checks, "ResourceBundle.getBundle(fxviews.ShowcaseView)");
            loader.setResources(bundle);
            Parent fresh = loader.load();
            ShowcaseViewController freshController = loader.getController();
            freshController.actionButton.fire();
            freshController.actionButton.fire();
            freshController.actionButton.fire();
            freshBox.getChildren().add(fresh);
            ShowcaseViewController repo = repositoryController;
            checks.add(Checks.expect("@Dependent controller per load", true, () -> freshController != repo));
            checks.add(Checks.expect("#onAction x3", "3 click(s)", () -> freshController.counterLabel.getText()));
        } catch (Throwable t) {
            checks.add(Check.fail("fresh load of ShowcaseView.fxml", FxmlUi.describe(t)));
            freshBox.getChildren().add(error(FxmlUi.describe(t)));
        }

        // 3. a second conventional view : custom name, sub-directory lookup, CDI presenter
        FxViewData status = repository.getViewData(STATUS_VIEW);
        VBox statusBox = FxmlUi.demo("getViewData(\"StatusCard\") : @FxView(\"StatusCard\"), sub-directory");
        checks.add(Checks.expect("getViewData(\"" + STATUS_VIEW + "\")", true, () -> status != null));
        if (status != null) {
            Parent root = status.getRootNode();
            StatusCardPresenter presenter = status.getController();
            statusBox.getChildren().add(root);
            checks.add(Checks.expect("StatusCard root / presenter", "TitledPane / StatusCardPresenter, initialized",
                    () -> root.getClass().getSimpleName() + " / " + presenter.getClass().getSimpleName()
                            + (presenter.initialized ? ", initialized" : ", not initialized")));
            checks.add(Checks.expect("FxViewConfig injected in presenter", "fxviews / DEV",
                    () -> presenter.viewsRootValue.getText() + " / " + presenter.reloadValue.getText()));
            checks.add(Checks.expect("sub-directory bundle + stylesheet", "StatusCard/StatusCard.properties, StatusCard.css",
                    () -> presenter.bundleValue.getText() + ", " + root.getStylesheets().stream()
                            .map(css -> css.substring(css.lastIndexOf('/') + 1)).toList().getFirst()));
        } else {
            statusBox.getChildren().add(error("No view data for " + STATUS_VIEW));
        }

        // 4. HostServices produced by quarkus-fx (never shows paths)
        checks.add(Checks.expect("@Inject HostServices", true, () -> hostServices != null));
        checks.add(Checks.expect("HostServices.resolveURI", "https://quarkus.io/guides/javafx",
                () -> hostServices.resolveURI("https://quarkus.io/guides/", "javafx")));
        checks.add(Checks.expect("HostServices.getDocumentBase() set", true,
                () -> hostServices.getDocumentBase() != null && !hostServices.getDocumentBase().isEmpty()));

        // 5. @RunOnFxThread from the FX thread : runs in place
        CompletableFuture<String> fromFx = new CompletableFuture<>();
        fxThreadProbe.record("called on FX thread", fromFx);
        checks.add(Checks.expect("@RunOnFxThread called on the FX thread", "ran synchronously: called on FX thread -> ran on FX thread: true",
                () -> (fromFx.isDone() ? "ran synchronously: " + fromFx.getNow(null) : "deferred")));

        // 6. @RunOnFxThread and FxStartupLatch from a background thread
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "showcase-fxview-worker");
            thread.setDaemon(true);
            return thread;
        });
        CompletableFuture<String> latch = new CompletableFuture<>();
        CompletableFuture<String> fromBackground = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                startupLatch.await();
                latch.complete("await() returned on a background thread (fx thread: "
                        + Platform.isFxApplicationThread() + ")");
            } catch (Throwable t) {
                latch.completeExceptionally(t);
            }
            try {
                fxThreadProbe.record("called on a background thread", fromBackground);
            } catch (Throwable t) {
                fromBackground.completeExceptionally(t);
            }
        });
        executor.shutdown();

        // Lifecycle events
        VBox events = new VBox(2);
        for (String event : lifecycle.events) {
            events.getChildren().add(eventLine(event));
        }
        checks.add(Checks.expect("startup events order", "StartupEvent, FxApplicationStartupEvent, FxViewLoadEvent, FxPostStartupEvent",
                () -> String.join(", ", lifecycle.events.stream().map(e -> e.substring(0, e.indexOf(' '))).toList())));
        checks.add(Checks.expect("FxApplicationStartupEvent application", "FxApplication",
                () -> lifecycle.application.getClass().getSimpleName()));
        VBox eventsBox = FxmlUi.demo("Startup events observed by a bean (@Observes)", events);

        VBox asyncBox = new VBox(2, FxmlUi.note("waiting for the worker thread..."));
        VBox threadBox = FxmlUi.demo("@RunOnFxThread + FxStartupLatch.await() from a worker thread", asyncBox);

        for (VBox box : List.of(repositoryBox, freshBox, statusBox)) {
            box.setPrefWidth(BOX_WIDTH);
            box.setMinWidth(BOX_WIDTH);
            box.setMaxWidth(BOX_WIDTH);
        }
        double half = (CONTENT_WIDTH - 12) / 2;
        for (VBox box : List.of(eventsBox, threadBox)) {
            box.setPrefWidth(half);
            box.setMinWidth(half);
            box.setMaxWidth(half);
        }
        HBox views = new HBox(12, repositoryBox, freshBox, statusBox);
        HBox runtime = new HBox(12, eventsBox, threadBox);

        int split = (checks.size() + 1) / 2;
        VBox left = new VBox(6, FxmlUi.checks("quarkus-fx checks", new ArrayList<>(checks.subList(0, split)), 230, half));
        VBox right = new VBox(6, FxmlUi.checks(" ", new ArrayList<>(checks.subList(split, checks.size())), 230, half));
        HBox checksRow = new HBox(12, left, right);
        VBox root = FxmlUi.page(10, views, runtime, checksRow);

        CompletableFuture<Void> async = CompletableFuture.allOf(latch, fromBackground)
                .handleAsync((v, error) -> {
                    List<Check> late = new ArrayList<>();
                    late.add(outcome("FxStartupLatch.await()", latch));
                    late.add(outcome("@RunOnFxThread from a worker thread", fromBackground));
                    asyncBox.getChildren().clear();
                    for (Check check : late) {
                        asyncBox.getChildren().add(eventLine(check.value()));
                    }
                    left.getChildren().add(FxmlUi.checks("Worker thread", late, 230, half));
                    return null;
                }, Fx.FX_THREAD);
        CompletableFuture<Void> shown = FxmlLoaderPage.whenShown(root, () -> {
        });
        root.getProperties().put(READY, CompletableFuture.allOf(async, shown));
        root.getProperties().put(EXECUTOR, executor);
        return root;
    }

    private static Check outcome(String name, CompletableFuture<String> future) {
        try {
            return Check.pass(name, future.join());
        } catch (Throwable t) {
            return Check.fail(name, FxmlUi.describe(t.getCause() != null ? t.getCause() : t));
        }
    }

    private static Label eventLine(String text) {
        Label line = new Label(text);
        line.getStyleClass().add("event-line");
        FxmlUi.mono(line);
        line.setWrapText(true);
        line.setMinHeight(Region.USE_PREF_SIZE);
        return line;
    }

    private static Label error(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("load-error");
        label.setWrapText(true);
        label.setMaxWidth(320);
        return label;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        Object ready = content.getProperties().get(READY);
        return ready instanceof CompletionStage<?> stage ? stage : CompletableFuture.completedFuture(null);
    }

    @Override
    public void dispose(Node content) {
        if (content.getProperties().get(EXECUTOR) instanceof ExecutorService executor) {
            executor.shutdownNow();
        }
    }
}
