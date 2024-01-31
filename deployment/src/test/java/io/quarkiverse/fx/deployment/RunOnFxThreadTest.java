package io.quarkiverse.fx.deployment;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxApplication;
import io.quarkiverse.fx.PrimaryStage;
import io.quarkiverse.fx.RunOnFxThread;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class RunOnFxThreadTest {

    private static final int LAUNCH_TIMEOUT_MS = 1_000;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Observer.class));

    @Test
    void annotatedMethodsExecuteOnFxThread() {
        // launch
        CompletableFuture.runAsync(() -> Application.launch(FxApplication.class));
        // wait for stage to be observed
        Awaitility.await().atMost(LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(Observer::stageObserved);
        // stage should not be null
        Assertions.assertNotNull(Observer.OBSERVED_STAGE.get());
        //
        Assertions.assertTrue(Observer.STAGE_OBSERVER_METHOD_THREAD_IS_FX_THREAD);
        Assertions.assertFalse(Observer.DO_ON_CURRENT_METHOD_THREAD_IS_FX_THREAD);
        Assertions.assertTrue(Observer.DO_ON_FX_METHOD_THREAD_IS_FX_THREAD);
    }

    @Dependent
    static class Observer {
        public static final AtomicReference<Stage> OBSERVED_STAGE = new AtomicReference<>();
        public static boolean STAGE_OBSERVER_METHOD_THREAD_IS_FX_THREAD;
        public static boolean DO_ON_CURRENT_METHOD_THREAD_IS_FX_THREAD;
        public static boolean DO_ON_FX_METHOD_THREAD_IS_FX_THREAD;

        public void observeStage(@Observes @PrimaryStage final Stage stage) throws ExecutionException, InterruptedException {
            // Observe current thread
            STAGE_OBSERVER_METHOD_THREAD_IS_FX_THREAD = Platform.isFxApplicationThread();
            // launch methods and observe threads
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(this::doOnCurrentThread).get();
            executor.submit(this::doOnFxThread).get();
            executor.shutdownNow();
            // observe stage
            Observer.OBSERVED_STAGE.set(stage);
        }

        void doOnCurrentThread() {
            DO_ON_CURRENT_METHOD_THREAD_IS_FX_THREAD = Platform.isFxApplicationThread();
        }

        @RunOnFxThread
        void doOnFxThread() {
            DO_ON_FX_METHOD_THREAD_IS_FX_THREAD = Platform.isFxApplicationThread();
        }

        public static boolean stageObserved() {
            return OBSERVED_STAGE.get() != null;
        }
    }

}
