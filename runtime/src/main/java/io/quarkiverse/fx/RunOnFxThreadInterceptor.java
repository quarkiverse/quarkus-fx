package io.quarkiverse.fx;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

import javafx.application.Platform;

@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
@RunOnFxThread
public class RunOnFxThreadInterceptor {

    private static final Logger LOGGER = Logger.getLogger(RunOnFxThreadInterceptor.class);

    // The startup latch signalled by FxApplication
    @Inject
    FxStartupLatch startupLatch;

    @Inject
    FxLifecycle lifecycle;

    @AroundInvoke
    public Object runOnFxThread(InvocationContext ctx) throws Exception {
        LOGGER.tracef("intercepted %s on thread %s", ctx.getMethod(), Thread.currentThread());

        // Block thread until the startup latch has been cleared
        // This will return immediately after FX is ready and primary Stage instance is available
        if (Platform.isFxApplicationThread()) {
            return ctx.proceed();
        } else {
            // Embedded in SWT, no application releases the latch : FXCanvas starts JavaFX, and runLater fails before
            if (!this.lifecycle.isEmbeddedInSwt()) {
                this.startupLatch.await();
            }
            this.lifecycle.runLater(() -> {
                try {
                    ctx.proceed();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        }
    }

}
