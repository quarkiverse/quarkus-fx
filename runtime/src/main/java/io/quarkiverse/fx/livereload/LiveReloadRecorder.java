package io.quarkiverse.fx.livereload;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LiveReloadRecorder {

    private static final Logger LOGGER = Logger.getLogger(LiveReloadRecorder.class);

    public void process(boolean liveReload) {
        // Prepared for later
        LOGGER.debugf("Fx live reload : %b", liveReload);
    }
}
