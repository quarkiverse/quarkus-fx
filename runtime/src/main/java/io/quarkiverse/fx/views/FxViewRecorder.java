package io.quarkiverse.fx.views;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FxViewRecorder {

    public void process(Map<String, String> viewControllers, BeanContainer beanContainer) {
        Map<String, Class<?>> controllers = new LinkedHashMap<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        viewControllers.forEach((name, controller) -> {
            try {
                controllers.put(name, Class.forName(controller, false, loader));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot load controller " + controller + " for view '" + name + "'", e);
            }
        });
        beanContainer.beanInstance(FxViewRepository.class).setViewControllers(controllers);
    }

    public void process(List<String> viewNames, BeanContainer beanContainer) {
        FxViewRepository fxViewRepository = beanContainer.beanInstance(FxViewRepository.class);
        fxViewRepository.setViewNames(viewNames);
    }
}
