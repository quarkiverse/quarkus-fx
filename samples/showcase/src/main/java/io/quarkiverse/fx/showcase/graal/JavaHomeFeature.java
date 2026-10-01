package io.quarkiverse.fx.showcase.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeSystemProperties;

/**
 * HijrahChronology (DatePicker with the Hijrah calendar) reads java.home in its static initializer, and java.home is not
 * set in native executables: https://github.com/oracle/graal/issues/11410
 * A java.home system property set on the command line still takes precedence over this default.
 * Registered in META-INF/native-image/io.quarkiverse.fx.showcase/quarkus-fx-showcase/native-image.properties.
 */
public final class JavaHomeFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        RuntimeSystemProperties.register("java.home", "");
    }
}
