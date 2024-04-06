package io.quarkiverse.fx.deployment;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.VoidType;
import org.jboss.logging.Logger;

import io.quarkiverse.fx.FXMLLoaderProducer;
import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.FxView;
import io.quarkiverse.fx.FxViewRecorder;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.RunOnFxThreadInterceptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.runtime.annotations.QuarkusMain;

class QuarkusFxExtensionProcessor {

    private static final String FEATURE = "quarkus-fx";
    private static final Logger LOGGER = Logger.getLogger(QuarkusFxExtensionProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem fxmlLoader() {
        return new AdditionalBeanBuildItem(FXMLLoaderProducer.class);
    }

    /**
     * Register the FxApplication so the startup latch producer method is found
     *
     * @return build item for FxApplication
     */
    @BuildStep
    AdditionalBeanBuildItem startupLatch() {
        return new AdditionalBeanBuildItem(FxStartupLatch.class);
    }

    @BuildStep
    AdditionalBeanBuildItem runOnFxThreadInterceptor(final CombinedIndexBuildItem combinedIndex) {
        Consumer<AnnotationTarget> methodChecker = target -> {
            if (!(target.asMethod().returnType() instanceof VoidType)) {
                LOGGER.warnf("Method %s annotated with %s return value will be lost, set return type to void",
                        target.asMethod().name(),
                        RunOnFxThread.class.getSimpleName());
            }
        };

        Collection<AnnotationInstance> annotations = combinedIndex.getComputingIndex().getAnnotations(RunOnFxThread.class);
        for (AnnotationInstance annotation : annotations) {
            AnnotationTarget target = annotation.target();
            switch (target.kind()) {
                case METHOD -> methodChecker.accept(target);
                case CLASS -> target.asClass().methods().forEach(methodChecker);
            }
        }

        return new AdditionalBeanBuildItem(RunOnFxThread.class, RunOnFxThreadInterceptor.class);
    }

    @BuildStep
    void quarkusFxLauncher(
            final CombinedIndexBuildItem combinedIndex,
            @Overridable final BuildProducer<QuarkusApplicationClassBuildItem> quarkusApplicationClass) {

        IndexView index = combinedIndex.getIndex();

        // Look for an existing @QuarkusMain annotation
        // If found, do nothing
        // Otherwise, provide a default QuarkusFxApplication that launches the FX application
        if (index.getAnnotations(DotName.createSimple(QuarkusMain.class.getName())).isEmpty()) {
            quarkusApplicationClass.produce(new QuarkusApplicationClassBuildItem(QuarkusFxApplication.class));
        }
    }

    @BuildStep
    void fxView(
            final CombinedIndexBuildItem combinedIndex,
            final BuildProducer<FxViewBuildItem> producer) {

        // Look for all @FxView annotations
        Collection<AnnotationInstance> annotations = combinedIndex.getComputingIndex().getAnnotations(FxView.class);
        for (AnnotationInstance annotation : annotations) {
            ClassInfo classInfo = annotation.target().asClass();
            LOGGER.infof("Found @FxView " + classInfo);
            producer.produce(new FxViewBuildItem(classInfo));
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void build(
            final List<FxViewBuildItem> fxViewItems,
            final FxViewRecorder recorder,
            final BuildProducer<BeanContainerListenerBuildItem> containerListenerProducer) {

        System.out.println("build " + fxViewItems);

        List<String> list = fxViewItems.stream()
                .map(item -> item.getClassInfo().name().toString())
                .toList();

        recorder.process(list);
        //        containerListenerProducer.produce(new BeanContainerListenerBuildItem(recorder.process(list)));
    }
}
