package io.quarkiverse.fx.deployment;

import io.quarkiverse.fx.QuarkusFxBuildTimeConfig;

import java.util.function.BooleanSupplier;

public class IsAutoLaunch implements BooleanSupplier {

  private final QuarkusFxBuildTimeConfig config;

  IsAutoLaunch(final QuarkusFxBuildTimeConfig config) {
    this.config = config;
  }

  @Override
  public boolean getAsBoolean() {
    return this.config.autoLaunch;
  }
}
