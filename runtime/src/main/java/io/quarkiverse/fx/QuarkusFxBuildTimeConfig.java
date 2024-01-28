package io.quarkiverse.fx;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "fx", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class QuarkusFxBuildTimeConfig {

  /**
   * Whether auto launch is enabled.
   */
  @ConfigItem(defaultValue = "true")
  public boolean autoLaunch;
}
