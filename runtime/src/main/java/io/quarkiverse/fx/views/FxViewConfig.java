package io.quarkiverse.fx.views;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.fx")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface FxViewConfig {

    /**
     * Root location for fx views.
     * The extension will look for fx views from this root directory.
     */
    @WithDefault("/")
    String viewsRoot();

    /**
     * Enable (or disable) stylesheet live reload in dev mode
     */
    @WithDefault("true")
    boolean stylesheetReload();

    /**
     * Location for main resources (allowing stylesheet live reload in dev mode)
     */
    @WithDefault("src/main/resources/")
    String mainResources();

    /**
     * Location for target classes (where .class files are located)
     * In dev mode, if stylesheet reload is activated,
     * app will use stylesheet from sources instead of the ones in target and monitor changes
     */
    @WithDefault("target/classes/")
    String targetClasses();
}
