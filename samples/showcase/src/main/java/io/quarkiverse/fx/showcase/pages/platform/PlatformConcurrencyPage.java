package io.quarkiverse.fx.showcase.pages.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * javafx.concurrent (Task, Service, Worker states) and the Platform threading primitives (runLater ordering, nested
 * event loops, pulses, CompletableFuture with an FX executor).
 */
@Singleton
public class PlatformConcurrencyPage implements FeaturePage {

    @Override
    public String id() {
        return "platform-concurrency";
    }

    @Override
    public String title() {
        return "Tasks, Services & Threading";
    }

    @Override
    public String category() {
        return Categories.PLATFORM;
    }

    @Override
    public int order() {
        return 20;
    }

    /** Displays a worker : title, state chips, determinate progress bar, message and value. */
    private static final class WorkerView extends VBox {

        final List<Worker.State> states = new CopyOnWriteArrayList<>();
        final FlowPane chips = new FlowPane(3, 3);

        WorkerView(String caption, Worker<?> worker) {
            super(4);
            getStyleClass().add("demo-box");
            states.add(worker.getState());
            addChip(worker.getState());
            worker.stateProperty().addListener((observable, oldState, newState) -> {
                states.add(newState);
                addChip(newState);
            });
            ProgressBar bar = new ProgressBar();
            // never indeterminate : a progress of -1 would animate
            bar.progressProperty().bind(Bindings.max(0, worker.progressProperty()));
            bar.setPrefWidth(150);
            Label message = new Label();
            message.textProperty().bind(worker.messageProperty());
            message.getStyleClass().add("kv-value");
            Label value = new Label();
            value.textProperty().bind(Bindings.createStringBinding(() -> "value: " + worker.getValue()
                    + (worker.getException() != null ? ", exception: " + worker.getException().getMessage() : ""),
                    worker.valueProperty(), worker.exceptionProperty()));
            value.getStyleClass().add("kv-value");
            HBox progress = new HBox(8, bar, message);
            progress.setAlignment(Pos.CENTER_LEFT);
            getChildren().addAll(PlatformUi.caption(caption), chips, progress, value);
            chips.setPrefWrapLength(460);
        }

        private void addChip(Worker.State state) {
            if (!chips.getChildren().isEmpty()) {
                Label arrow = new Label("→");
                arrow.getStyleClass().add("state-arrow");
                chips.getChildren().add(arrow);
            }
            Label chip = new Label(state.name());
            chip.getStyleClass().addAll("state-chip", "state-" + state.name().toLowerCase(java.util.Locale.ROOT));
            chips.getChildren().add(chip);
        }

        String sequence() {
            return String.join(" > ", states.stream().map(Enum::name).toList());
        }
    }

    @Override
    public Node build() throws Exception {
        ExecutorService executor = PlatformUi.executor("showcase-concurrency");
        List<Check> checks = new ArrayList<>();
        List<CompletableFuture<?>> pending = new ArrayList<>();

        // 1. a successful Task : title, message, progress, value updates
        Task<Integer> sum = new Task<>() {
            @Override
            protected Integer call() {
                updateTitle("Sum of squares");
                int total = 0;
                for (int i = 1; i <= 10; i++) {
                    total += i * i;
                    updateValue(total);
                    updateProgress(i, 10);
                    updateMessage("step " + i + "/10");
                }
                updateMessage("done");
                return total;
            }
        };
        List<String> handlers = new CopyOnWriteArrayList<>();
        sum.setOnScheduled(e -> handlers.add("onScheduled"));
        sum.setOnRunning(e -> handlers.add("onRunning"));
        sum.setOnSucceeded(e -> handlers.add("onSucceeded(fx=" + Platform.isFxApplicationThread() + ")"));
        WorkerView sumView = new WorkerView("Task<Integer> : updateProgress / updateMessage / updateValue", sum);
        pending.add(Fx.when(sum.stateProperty(), Worker.State.SUCCEEDED::equals).toCompletableFuture());

        // 2. a failing Task
        Task<String> failing = new Task<>() {
            @Override
            protected String call() {
                updateProgress(4, 10);
                updateMessage("about to fail");
                throw new IllegalStateException("boom (expected)");
            }
        };
        WorkerView failingView = new WorkerView("Task throwing an exception", failing);
        pending.add(Fx.when(failing.stateProperty(), Worker.State.FAILED::equals).toCompletableFuture());

        // 3. a Task cancelled while running
        CountDownLatch never = new CountDownLatch(1);
        Task<String> blocked = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateProgress(1, 2);
                updateMessage("waiting to be cancelled");
                never.await();
                return "not cancelled";
            }
        };
        WorkerView blockedView = new WorkerView("Task cancelled while RUNNING", blocked);
        Fx.when(blocked.stateProperty(), Worker.State.RUNNING::equals)
                .thenRun(() -> Platform.runLater(() -> blocked.cancel(true)));
        pending.add(Fx.when(blocked.stateProperty(), Worker.State.CANCELLED::equals).toCompletableFuture());

        // 4. a Service restarted once it succeeded
        Service<String> service = new Service<>() {
            private int runs;

            @Override
            protected Task<String> createTask() {
                int run = ++runs;
                return new Task<>() {
                    @Override
                    protected String call() {
                        updateProgress(run, 2);
                        updateMessage("run " + run + " of 2");
                        return "run #" + run;
                    }
                };
            }
        };
        service.setExecutor(executor);
        List<String> serviceValues = new CopyOnWriteArrayList<>();
        service.valueProperty().addListener((observable, oldValue, newValue) -> serviceValues.add(String.valueOf(newValue)));
        CompletableFuture<Void> serviceDone = new CompletableFuture<>();
        service.setOnSucceeded(e -> {
            if ("run #1".equals(service.getValue())) {
                Platform.runLater(service::restart);
            } else {
                serviceDone.complete(null);
            }
        });
        WorkerView serviceView = new WorkerView("Service : start(), restart() after SUCCEEDED", service);
        pending.add(serviceDone);

        // 5. a ScheduledService cancelled after its third run
        ScheduledService<Integer> scheduled = new ScheduledService<>() {
            private int runs;

            @Override
            protected Task<Integer> createTask() {
                int run = ++runs;
                return new Task<>() {
                    @Override
                    protected Integer call() {
                        updateProgress(run, 3);
                        updateMessage("run " + run + " of 3");
                        return run * 10;
                    }
                };
            }
        };
        scheduled.setExecutor(executor);
        // a zero period : every iteration starts immediately. With a longer period, whether the next run is delayed
        // by a Timer depends on the elapsed time, and so does the sequence of states.
        scheduled.setPeriod(Duration.ZERO);
        List<String> lastValues = new CopyOnWriteArrayList<>();
        CompletableFuture<Void> scheduledDone = new CompletableFuture<>();
        scheduled.lastValueProperty().addListener((observable, oldValue, newValue) -> {
            lastValues.add(String.valueOf(newValue));
        });
        scheduled.setOnSucceeded(e -> {
            if (Integer.valueOf(30).equals(scheduled.getValue())) {
                scheduled.cancel();
            }
        });
        scheduled.setOnCancelled(e -> scheduledDone.complete(null));
        WorkerView scheduledView = new WorkerView("ScheduledService : period 0, cancel() after the 3rd run", scheduled);
        Label lastValue = new Label();
        lastValue.textProperty().bind(Bindings.concat("lastValue: ", scheduled.lastValueProperty().asString(),
                ", currentFailureCount: ", scheduled.currentFailureCountProperty().asString()));
        lastValue.getStyleClass().add("kv-value");
        scheduledView.getChildren().add(lastValue);
        pending.add(scheduledDone);

        executor.execute(sum);
        executor.execute(failing);
        executor.execute(blocked);
        service.start();
        scheduled.start();

        // 6. Platform.runLater ordering, per posting thread
        List<String> order = new CopyOnWriteArrayList<>();
        List<String> workerOrder = new CopyOnWriteArrayList<>();
        CompletableFuture<Void> orderDone = new CompletableFuture<>();
        Platform.runLater(() -> {
            order.add("A");
            Platform.runLater(() -> order.add("D (posted by A)"));
        });
        Platform.runLater(() -> order.add("B"));
        Platform.runLater(() -> order.add("C"));
        CompletableFuture.runAsync(() -> {
            for (int i = 1; i <= 5; i++) {
                String name = "W" + i;
                Platform.runLater(() -> workerOrder.add(name));
            }
            Platform.runLater(() -> Platform.runLater(() -> orderDone.complete(null)));
        }, executor);
        pending.add(orderDone);

        // 7. nested event loop (runs A, B and C above) returning a value
        Object key = new Object();
        List<String> nested = new ArrayList<>();
        Platform.runLater(() -> {
            nested.add("inside: nested loop running=" + Platform.isNestedLoopRunning());
            Platform.exitNestedEventLoop(key, "value from exitNestedEventLoop");
        });
        Object returned = Platform.enterNestedEventLoop(key);
        nested.add("returned: " + returned);
        nested.add("after: nested loop running=" + Platform.isNestedLoopRunning());
        checks.add(Checks.expect("enterNestedEventLoop / exitNestedEventLoop",
                "inside: nested loop running=true | returned: value from exitNestedEventLoop | after: nested loop running=false",
                () -> String.join(" | ", nested)));

        // 8. CompletableFuture : a worker thread, then the FX thread as executor
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> "computed on worker (fx=" + Platform.isFxApplicationThread() + ")", executor)
                .thenApplyAsync(value -> value + ", continued on FX executor (fx=" + Platform.isFxApplicationThread()
                        + ")", Fx.FX_THREAD);
        pending.add(future);

        VBox workers = new VBox(8, sumView, failingView, blockedView, serviceView, scheduledView);
        PlatformUi.width(workers, PlatformUi.HALF_WIDTH);

        VBox results = new VBox(3, PlatformUi.note("waiting for the workers..."));
        VBox resultsBox = PlatformUi.demo("Platform threading primitives", results);
        VBox rightColumn = new VBox(10, resultsBox);
        PlatformUi.width(rightColumn, PlatformUi.HALF_WIDTH);
        PlatformUi.width(resultsBox, PlatformUi.HALF_WIDTH);

        VBox root = PlatformUi.page(0, new HBox(12, workers, rightColumn));

        // 9. Platform.requestNextPulse, once shown
        CompletableFuture<String> pulse = new CompletableFuture<>();
        Fx.when(root.sceneProperty(), Objects::nonNull).thenAccept(scene -> requestPulse(scene, pulse));
        pending.add(pulse);

        CompletableFuture<Void> all = CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
        CompletableFuture<Object> ready = Fx.timeout(all, 15_000, "workers").handleAsync((v, error) -> {
            List<Check> late = new ArrayList<>(checks);
            late.add(Checks.expect("Task states", "READY > SCHEDULED > RUNNING > SUCCEEDED", sumView::sequence));
            late.add(Checks.expect("Task handlers", "onScheduled, onRunning, onSucceeded(fx=true)",
                    () -> String.join(", ", handlers)));
            late.add(Checks.expect("Task title / message / progress / value", "Sum of squares / done / 1.0 / 385",
                    () -> sum.getTitle() + " / " + sum.getMessage() + " / " + sum.getProgress() + " / " + sum.getValue()));
            late.add(Checks.expect("failing Task states", "READY > SCHEDULED > RUNNING > FAILED", failingView::sequence));
            late.add(Checks.expect("failing Task exception", "IllegalStateException: boom (expected)",
                    () -> failing.getException().getClass().getSimpleName() + ": " + failing.getException().getMessage()));
            late.add(Checks.expect("cancelled Task states", "READY > SCHEDULED > RUNNING > CANCELLED", blockedView::sequence));
            late.add(Checks.expect("cancelled Task isCancelled / value", "true / null",
                    () -> blocked.isCancelled() + " / " + blocked.getValue()));
            late.add(Checks.expect("Service states (restart)",
                    "READY > SCHEDULED > RUNNING > SUCCEEDED > CANCELLED > READY > SCHEDULED > RUNNING > SUCCEEDED",
                    serviceView::sequence));
            late.add(Checks.expect("Service values", "run #1, null, run #2", () -> String.join(", ", serviceValues)));
            late.add(Checks.run("ScheduledService states", scheduledView::sequence));
            late.add(Checks.expect("ScheduledService lastValue changes", "10, 20, 30", () -> String.join(", ", lastValues)));
            late.add(Checks.expect("runLater order (FX thread)", "A, B, C, D (posted by A)", () -> String.join(", ", order)));
            late.add(Checks.expect("runLater order (worker thread)", "W1, W2, W3, W4, W5",
                    () -> String.join(", ", workerOrder)));
            late.add(Checks.expect("CompletableFuture + FX executor",
                    "computed on worker (fx=false), continued on FX executor (fx=true)", () -> future.getNow("pending")));
            late.add(Checks.expect("Platform.requestNextPulse()", "pre-layout pulse listener called (fx=true)",
                    () -> pulse.getNow("no pulse")));
            if (error != null) {
                late.add(Check.fail("workers completed", Checks.describe(error)));
            }
            results.getChildren().clear();
            results.getChildren().add(PlatformUi.line("runLater (FX thread) → " + String.join(", ", order)));
            results.getChildren().add(PlatformUi.line("runLater (worker) → " + String.join(", ", workerOrder)));
            for (String line : nested) {
                results.getChildren().add(PlatformUi.line("nested loop → " + line));
            }
            results.getChildren().add(PlatformUi.line("CompletableFuture → " + future.getNow("pending")));
            results.getChildren().add(PlatformUi.line("requestNextPulse → " + pulse.getNow("no pulse")));
            rightColumn.getChildren().add(PlatformUi.checks("Checks", late, 190, PlatformUi.HALF_WIDTH));
            return null;
        }, Fx.FX_THREAD).toCompletableFuture();
        root.getProperties().put(PlatformUi.READY, ready);
        root.getProperties().put(PlatformUi.EXECUTOR, executor);
        return root;
    }

    /**
     * On the note that no pulse may be scheduled while nothing changes, a pre-layout pulse listener is installed and a
     * pulse is requested explicitly.
     */
    private static void requestPulse(Scene scene, CompletableFuture<String> pulse) {
        Runnable[] listener = new Runnable[1];
        listener[0] = () -> {
            scene.removePreLayoutPulseListener(listener[0]);
            pulse.complete("pre-layout pulse listener called (fx=" + Platform.isFxApplicationThread() + ")");
        };
        scene.addPreLayoutPulseListener(listener[0]);
        Platform.requestNextPulse();
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return PlatformUi.ready(content);
    }

    @Override
    public void dispose(Node content) {
        PlatformUi.shutdown(content);
    }
}
