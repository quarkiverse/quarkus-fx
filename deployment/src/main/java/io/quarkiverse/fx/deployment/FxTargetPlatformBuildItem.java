package io.quarkiverse.fx.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FxTargetPlatformBuildItem extends SimpleBuildItem {

    private final String targetPlatform;

    public FxTargetPlatformBuildItem(String targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public boolean isWindows() {
        return targetPlatform.startsWith("win");
    }

    public boolean isMac() {
        return targetPlatform.startsWith("mac");
    }

    public boolean isLinux() {
        return targetPlatform.startsWith("linux");
    }

    public boolean isX64() {
        return !targetPlatform.contains("-");
    }

    public boolean isAarch64() {
        return targetPlatform.endsWith("-aarch64");
    }
}