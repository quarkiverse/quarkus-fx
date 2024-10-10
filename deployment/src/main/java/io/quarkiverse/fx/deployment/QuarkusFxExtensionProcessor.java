package io.quarkiverse.fx.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import io.quarkiverse.fx.views.FxViewLocation;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.VoidType;
import org.jboss.logging.Logger;

import io.quarkiverse.fx.FXMLLoaderProducer;
import io.quarkiverse.fx.FxStartupLatch;
import io.quarkiverse.fx.HostServicesProducer;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.RunOnFxThreadInterceptor;
import io.quarkiverse.fx.livereload.LiveReloadRecorder;
import io.quarkiverse.fx.views.FxView;
import io.quarkiverse.fx.views.FxViewRecorder;
import io.quarkiverse.fx.views.FxViewRepository;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.runtime.annotations.QuarkusMain;

class QuarkusFxExtensionProcessor {

    private static final String FEATURE = "quarkus-fx";

    // Controller suffix naming convention (optional)
    private static final String CONTROLLER_SUFFIX = "Controller";

    private static final Logger LOGGER = Logger.getLogger(QuarkusFxExtensionProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem fxmlLoader() {
        return new AdditionalBeanBuildItem(FXMLLoaderProducer.class);
    }

    @BuildStep
    AdditionalBeanBuildItem hostServices() {
        return new AdditionalBeanBuildItem(HostServicesProducer.class);
    }

    @BuildStep
    AdditionalBeanBuildItem startupLatch() {
        return new AdditionalBeanBuildItem(FxStartupLatch.class);
    }

    @BuildStep
    AdditionalBeanBuildItem fxViewRepository() {
        return new AdditionalBeanBuildItem(FxViewRepository.class);
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
        } else {
            LOGGER.info("Existing @QuarkusMain annotation were found, Quarkus-FX will not generate QuarkusFxApplication.");
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void handleLiveReload(
            final LiveReloadBuildItem liveReloadBuildItem,
            final LiveReloadRecorder recorder) {

        recorder.process(liveReloadBuildItem.isLiveReload());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void fxViews(
            final CombinedIndexBuildItem combinedIndex,
            final FxViewRecorder recorder,
            final BeanContainerBuildItem beanContainerBuildItem) {

        List<FxViewLocation> viewLocations = new ArrayList<>();

        // Look for all @FxView annotations
        Collection<AnnotationInstance> annotations = combinedIndex.getComputingIndex().getAnnotations(FxView.class);
        for (AnnotationInstance annotation : annotations) {

            ClassInfo target = annotation.target().asClass();
            AnnotationValue value = annotation.value();
            AnnotationValue fxmlLocationValue = annotation.value("fxmlLocation");
            AnnotationValue styleLocationValue = annotation.value("styleLocation");
            AnnotationValue bundleLocationValue = annotation.value("bundleLocation");

            String fxmlLocation = fxmlLocationValue != null ? fxmlLocationValue.asString() : "";
            String styleLocation = styleLocationValue != null ? styleLocationValue.asString() : "";
            String bundleLocation = bundleLocationValue != null ? bundleLocationValue.asString() : "";

            if (value != null) {
                // Custom value is set in annotation : use it
                String customName = value.asString();
                viewLocations.add(new FxViewLocation(customName, fxmlLocation, styleLocation, bundleLocation));
            } else {
                // Use convention
                // If controller is named "MySampleController", expected fxml would be MySample.fxml
                // Controller suffix is optional but shall be present to respect convention
                String name = target.simpleName();
                if (name.endsWith(CONTROLLER_SUFFIX)) {
                    // Valid convention
                    LOGGER.debugf(
                            "Found controller annotated with %s : %s",
                            FxView.class.getName(),
                            name);

                    // Remove the controller suffix
                    String baseName = name.substring(0, name.length() - CONTROLLER_SUFFIX.length());
                    viewLocations.add(new FxViewLocation(baseName, fxmlLocation, styleLocation, bundleLocation));
                } else {
                    LOGGER.warnf(
                            "Type %s is annotated with %s but does not comply with naming convention (shall end with %s)",
                            name,
                            FxView.class.getName(),
                            CONTROLLER_SUFFIX);

                    viewLocations.add(new FxViewLocation(name, fxmlLocation, styleLocation, bundleLocation));
                }
            }
        }

        LOGGER.infof("Fx views : {}", viewLocations);

        recorder.process(viewLocations, beanContainerBuildItem.getValue());
    }
}
