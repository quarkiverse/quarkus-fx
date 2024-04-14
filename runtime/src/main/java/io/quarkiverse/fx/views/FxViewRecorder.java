package io.quarkiverse.fx.views;

import java.util.List;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FxViewRecorder {

    public void process(final List<String> viewNames, final BeanContainer beanContainer) {
        FxViewRepository fxViewRepository = beanContainer.beanInstance(FxViewRepository.class);
        fxViewRepository.setViewNames(viewNames);
    }
}
