package io.quarkiverse.fx.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FxTargetPlatformBuildItem extends SimpleBuildItem {

    private final String targetPlatform;

    public FxTargetPlatformBuildItem(String targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    public String getTargetPlatform() {
        return this.targetPlatform;
    }

    public boolean isWindows() {
        return this.targetPlatform.startsWith("win");
    }

    public boolean isMac() {
        return this.targetPlatform.startsWith("mac");
    }

    public boolean isLinux() {
        return this.targetPlatform.startsWith("linux");
    }

    public boolean isX64() {
        return !this.targetPlatform.contains("-");
    }

    public boolean isAarch64() {
        return this.targetPlatform.endsWith("-aarch64");
    }
}