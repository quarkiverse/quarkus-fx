package io.quarkiverse.fx.swt;

import org.jboss.logging.Logger;

import io.quarkiverse.fx.FxLifecycle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import javafx.application.Platform;

/**
 * JavaFX embedded in SWT, with Quarkus Desktop SWT : the first FXCanvas starts JavaFX on the SWT user interface thread,
 * which becomes the JavaFX Application Thread. Quarkus FX does not launch a JavaFX application then.
 */
@Recorder
public class SwtEmbeddingRecorder {

    private static final Logger LOGGER = Logger.getLogger(SwtEmbeddingRecorder.class);

    public static final String GUIDE = "https://docs.quarkiverse.io/quarkus-fx/dev/index.html#swt";

    /**
     * The system property that FXCanvas sets when it starts JavaFX (FXCanvas.initFx), which outlives a restart of the
     * Quarkus application in the JVM.
     */
    static final String APPLICATION_TYPE_PROPERTY = "com.sun.javafx.application.type";

    static final String FXCANVAS_APPLICATION_TYPE = "FXCanvas";

    /**
     * Called when the application starts, before the SWT user interface runs.
     */
    public void embed(BeanContainer beanContainer) {
        if (FXCANVAS_APPLICATION_TYPE.equals(System.getProperty(APPLICATION_TYPE_PROPERTY))) {
            // A restart of dev mode, or another application in this JVM (tests) : JavaFX starts once per JVM
            LOGGER.warnf("JavaFX already runs embedded in SWT in this JVM : it stays bound to the SWT user interface "
                    + "thread that started it, FXCanvas does not work on another one. Restart the JVM, see %s", GUIDE);
        }
        // Closing the last FXCanvas would otherwise exit JavaFX, which cannot start again in this JVM : the application
        // ends with SWT, as it ends with Quarkus when Quarkus FX launches JavaFX. A flag before JavaFX starts.
        Platform.setImplicitExit(false);
        beanContainer.beanInstance(FxLifecycle.class).embedInSwt();
    }
}
