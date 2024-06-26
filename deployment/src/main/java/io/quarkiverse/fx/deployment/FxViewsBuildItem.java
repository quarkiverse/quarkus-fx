package io.quarkiverse.fx.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FxViewsBuildItem extends SimpleBuildItem {

    private final List<String> viewNames;

    public FxViewsBuildItem(final List<String> viewNames) {
        this.viewNames = viewNames;
    }

    List<String> getViewNames() {
        return this.viewNames;
    }
}
