package io.quarkiverse.fx.views;

import java.util.List;

import io.quarkus.arc.runtime.BeanContainer;

public final class FxViewSupport {

    private FxViewSupport() {
    }

    public static void process(List<String> viewNames, BeanContainer beanContainer) {
        FxViewRepository fxViewRepository = beanContainer.beanInstance(FxViewRepository.class);
        fxViewRepository.setViewNames(viewNames);
    }
}
