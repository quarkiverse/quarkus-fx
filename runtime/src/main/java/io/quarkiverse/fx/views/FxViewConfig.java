package io.quarkiverse.fx.views;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "fx", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class FxViewConfig {

    /**
     * Root location for fx resources (.fxml)
     */
    @ConfigItem(defaultValue = "/")
    public String fxmlRoot;

    /**
     * Root location for style resources (.css)
     */
    @ConfigItem(defaultValue = "/")
    public String styleRoot;

    /**
     * Root location for bundle resources (.properties)
     */
    @ConfigItem(defaultValue = "/")
    public String bundleRoot;
}
