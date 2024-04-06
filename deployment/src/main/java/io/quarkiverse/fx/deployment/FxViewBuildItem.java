package io.quarkiverse.fx.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class FxViewBuildItem extends MultiBuildItem {

    private final ClassInfo classInfo;

    public FxViewBuildItem(final ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    public ClassInfo getClassInfo() {
        return this.classInfo;
    }
}
