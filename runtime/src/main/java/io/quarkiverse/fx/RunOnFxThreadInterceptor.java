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

    @AroundInvoke
    public Object runOnFxThread(final InvocationContext ctx) throws Exception {
        LOGGER.tracef("intercepted %s on thread %s", ctx.getMethod(), Thread.currentThread());

        // Block thread until the startup latch has been cleared
        // This will return immediately after FX is ready and primary Stage instance is available
        this.startupLatch.await();

        if (Platform.isFxApplicationThread()) {
            return ctx.proceed();
        } else {
            Platform.runLater(() -> {
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
