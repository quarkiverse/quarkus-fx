package io.quarkiverse.fx.livereload;

import org.jboss.logging.Logger;

import io.quarkiverse.fx.FxLifecycle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LiveReloadRecorder {

    private static final Logger LOGGER = Logger.getLogger(LiveReloadRecorder.class);

    public void process(boolean liveReload, BeanContainer beanContainer) {
        beanContainer.beanInstance(FxLifecycle.class).setLiveReload(liveReload);
        LOGGER.debugf("Fx live reload : %b", liveReload);
    }
}
