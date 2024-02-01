package io.quarkiverse.fx;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import javafx.application.Platform;
import org.jboss.logging.Logger;

@Interceptor
@RunOnFxThread
public class RunOnFxThreadInterceptor {

    private static final Logger LOGGER = Logger.getLogger(RunOnFxThreadInterceptor.class);

    @AroundInvoke
    public Object runOnFxThread(final InvocationContext ctx) throws Exception {
        LOGGER.tracef("intercepted %s on thread %s", ctx.getMethod(), Thread.currentThread());
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
