package io.quarkiverse.fx.deployment;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.QuarkusUnitTest;
import javafx.application.HostServices;

class FxStartupTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    FxStartupLatch latch;

    @Inject
    HostServices hostServices;

    @Test
    @Timeout(value = 10)
    void test() {

        Assertions.assertNotNull(this.latch);
        CompletableFuture.runAsync(() -> Quarkus.run(QuarkusFxApplication.class));

        try {
            this.latch.await();

            // Ensure HostServices instance is made available by using injection
            Assertions.assertNotNull(this.hostServices);

            // Invoke service
            this.hostServices.getCodeBase();

        } catch (Exception e) {
            Assertions.fail(e);
        }
    }
}
