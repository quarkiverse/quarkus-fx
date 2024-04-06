package io.quarkiverse.fx;

import java.util.List;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FxViewRecorder {

    public void process(final List<String> views) {
        FxApplication.setViews(views);
    }

    //    public BeanContainerListener process(final List<String> list) {
    //        System.out.println("FxViewRecorder Process()");
    //        return beanContainer -> {
    //            System.out.println("beanContainer");
    //            FxApplication loader = beanContainer.beanInstance(FxApplication.class);
    //            System.out.println(list);
    //            loader.setViews(list);
    //        };
    //    }
}
