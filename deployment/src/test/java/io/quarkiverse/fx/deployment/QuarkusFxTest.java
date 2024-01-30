package io.quarkiverse.fx.deployment;

import static org.awaitility.Awaitility.await;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxApplication;
import io.quarkiverse.fx.PrimaryStage;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class QuarkusFxTest {

    private static final int LAUNCH_TIMEOUT_MS = 3_000;
    private static final int A_FANCY_TEST_VALUE = 42;

    private static final String FXML_CONTENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <?import javafx.scene.layout.Pane?>
            <Pane xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1" fx:controller="%s" />
            """.formatted(TestController.class.getName());

    private static final AtomicBoolean primaryStageObserved = new AtomicBoolean(false);
    private static final AtomicBoolean testControllerInitialized = new AtomicBoolean(false);
    private static final AtomicInteger testServiceAnswer = new AtomicInteger(0);

    @Dependent
    public static class TestService {
        int getValue() {
            return A_FANCY_TEST_VALUE;
        }
    }

    @Singleton
    public static class TestController {

        @Inject
        TestService testService;

        @FXML
        void initialize() {
            testControllerInitialized.set(true);
            testServiceAnswer.set(this.testService.getValue());
        }
    }

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    FXMLLoader fxmlLoader;

    @Test
    void testFXMLLoaderInjection() {
        Assertions.assertNotNull(this.fxmlLoader);
    }

    @Test
    void testFXMLLaunchAndLoad() {
        // Non-blocking JavaFX launch
        CompletableFuture.runAsync(() -> Application.launch(FxApplication.class));

        await()
                .atMost(LAUNCH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .until(primaryStageObserved::get);

        try {
            InputStream fxmlStream = new ByteArrayInputStream(FXML_CONTENT.getBytes());
            Pane pane = this.fxmlLoader.load(fxmlStream);
            Assertions.assertNotNull(pane);
            Assertions.assertTrue(testControllerInitialized.get());
            Assertions.assertEquals(A_FANCY_TEST_VALUE, testServiceAnswer.get());
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    void observePrimaryStage(@Observes @PrimaryStage final Stage stage) {
        Assertions.assertNotNull(stage);
        primaryStageObserved.set(true);
    }
}
