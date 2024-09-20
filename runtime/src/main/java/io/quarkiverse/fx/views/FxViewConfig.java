package io.quarkiverse.fx.views;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.fx")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface FxViewConfig {

    /**
     * Root location for fx resources (.fxml)
     */
    @WithDefault("/")
    String fxmlRoot();

    /**
     * Root location for style resources (.css)
     */
    @WithDefault("/")
    String styleRoot();

    /**
     * Root location for bundle resources (.properties)
     */
    @WithDefault("/")
    String bundleRoot();

    /**
     * Location for main resources (allowing stylesheet live reload)
     */
    @WithDefault("src/main/resources/")
    String mainResources();

    /**
     * Location for test resources (allowing stylesheet live reload)
     */
    @WithDefault("src/test/resources/")
    String testResources();
}
