package io.quarkiverse.fx.deployment;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxPlatform;
import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;

class FxStartupTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    FxStartupLatch latch;

    @Inject
    HostServices hostServices;

    @Inject
    Instance<FXMLLoader> loaders;

    @Test
    @Timeout(value = 10)
    void test() {

        Assertions.assertNotNull(this.latch);
        new Thread(() -> new QuarkusFxApplication().run()).start();

        try {
            this.latch.await();

            // Ensure HostServices instance is made available by using injection
            Assertions.assertNotNull(this.hostServices);

            // Invoke service
            this.hostServices.getCodeBase();

            // Ordinary FX event handlers run with the persistent thread's classloader.
            // Loaders created there must still resolve the current application's controllers.
            ClassLoader runtimeLoader = Thread.currentThread().getContextClassLoader();
            Assertions.assertTrue(FxPlatform.launch().invoke(FxPlatform.class.getClassLoader(),
                    application -> Assertions.assertSame(runtimeLoader, this.loaders.get().getClassLoader())));

        } catch (Exception e) {
            Assertions.fail(e);
        }
    }
}
