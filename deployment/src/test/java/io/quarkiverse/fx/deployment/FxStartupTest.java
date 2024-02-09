package io.quarkiverse.fx.deployment;

import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CompletableFuture;

public class FxStartupTest {

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
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }
  }
}
