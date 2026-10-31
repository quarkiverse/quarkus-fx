package io.quarkiverse.fx.livereload;

import org.jboss.logging.Logger;

import io.quarkiverse.fx.FxLifecycle;
import io.quarkus.arc.runtime.BeanContainer;

public final class LiveReloadSupport {

    private static final Logger LOGGER = Logger.getLogger(LiveReloadSupport.class);

    private LiveReloadSupport() {
    }

    public static void process(boolean liveReload, BeanContainer beanContainer) {
        beanContainer.beanInstance(FxLifecycle.class).setLiveReload(liveReload);
        LOGGER.debugf("Fx live reload : %b", liveReload);
    }
}
