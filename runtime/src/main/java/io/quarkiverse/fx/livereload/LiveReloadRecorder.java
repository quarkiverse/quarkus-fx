package io.quarkiverse.fx.livereload;

import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;

@Recorder
public class LiveReloadRecorder {

    private static final Logger LOGGER = Logger.getLogger(LiveReloadRecorder.class);

    public void process(boolean liveReload) {
        // Prepared for later
        LOGGER.debugf("Fx live reload : %b", liveReload);
    }
}
