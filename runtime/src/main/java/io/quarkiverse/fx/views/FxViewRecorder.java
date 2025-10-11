package io.quarkiverse.fx.views;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

import java.util.List;

@Recorder
public class FxViewRecorder {

    public void process(List<String> viewNames, BeanContainer beanContainer) {
        FxViewRepository fxViewRepository = beanContainer.beanInstance(FxViewRepository.class);
        fxViewRepository.setViewNames(viewNames);
    }
}
