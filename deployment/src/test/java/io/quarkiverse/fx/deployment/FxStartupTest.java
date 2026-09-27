package io.quarkiverse.fx.deployment;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.QuarkusFxApplication;
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
        new Thread(() -> new QuarkusFxApplication().run()).start();

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
