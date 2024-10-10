package io.quarkiverse.fx.deployment;

import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import javafx.application.HostServices;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CompletableFuture;

class FxStartupTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    FxStartupLatch latch;

    @Inject
    HostServices hostServices;

    @Test
    @Timeout(value = 5)
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
