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

    //    /**
    //     * Root location for fx resources (.fxml)
    //     */
    //    @WithDefault("/")
    //    String fxmlRoot();
    //
    //    /**
    //     * Root location for style resources (.css)
    //     */
    //    @WithDefault("/")
    //    String styleRoot();
    //
    //    /**
    //     * Root location for bundle resources (.properties)
    //     */
    //    @WithDefault("/")
    //    String bundleRoot();

    /**
     * Default directory name for fx resources
     */
    @WithDefault("fxml")
    String fxmlDirectoryName();

    /**
     * Default directory name for fx resources
     */
    @WithDefault("style")
    String styleDirectoryName();

    /**
     * Default directory name for bundle resources (.properties)
     */
    @WithDefault("i18n")
    String bundleDirectoryName();

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
}
