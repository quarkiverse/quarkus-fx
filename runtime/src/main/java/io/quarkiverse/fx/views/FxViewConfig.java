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
     * Stylesheet live reload strategy.
     *
     * @see StylesheetReloadStrategy
     *      NEVER : never live reload stylesheets
     *      DEV : live reload is enabled in dev mode
     *      ALWAYS : live reload is always enabled
     */
    @WithDefault("DEV")
    StylesheetReloadStrategy stylesheetReloadStrategy();

    /**
     * Location for source resources (allowing stylesheet live reload in dev mode)
     */
    @WithDefault("src/main/resources/")
    String sourceResources();

    /**
     * Location for target resources (where resources files are located after build)
     * In dev mode, if stylesheet reload is activated,
     * app will use stylesheet from sources instead of the ones in target and monitor changes
     */
    @WithDefault("target/classes/")
    String targetResources();
}
