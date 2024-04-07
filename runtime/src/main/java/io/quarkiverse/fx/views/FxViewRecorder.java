package io.quarkiverse.fx.views;

import java.util.List;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FxViewRecorder {

    private final RuntimeValue<FxViewConfig> config;

    public FxViewRecorder(final RuntimeValue<FxViewConfig> config) {
        this.config = config;
    }

    public void process(final List<String> viewNames, final BeanContainer beanContainer) {

        // Read config and build view data
        FxViewConfig value = this.config.getValue();

        List<FxViewData> viewData = viewNames.stream()
                .map(name -> new FxViewData(
                        value.fxmlRoot + name,
                        value.styleRoot + name,
                        value.bundleRoot + name))
                .toList();

        FxViewRepository fxViewRepository = beanContainer.beanInstance(FxViewRepository.class);
        fxViewRepository.setViewData(viewData);
    }
}
