package io.quarkiverse.fx.deployment;

import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxApplication;
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

    @Test
    @Timeout(value = 5)
    void test() {

        Assertions.assertNotNull(this.latch);
        CompletableFuture.runAsync(() -> Quarkus.run(QuarkusFxApplication.class));

        try {
            this.latch.await();

            // Ensure HostServices instance is made available
            HostServices hostServices = FxApplication.getHostServicesInstance();
            Assertions.assertNotNull(hostServices);

        } catch (InterruptedException e) {
            Assertions.fail(e);
        }
    }
}
