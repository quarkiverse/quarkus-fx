package io.quarkiverse.fx.deployment;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkiverse.fx.FXMLLoaderProducer;
import io.quarkiverse.fx.PrimaryStage;
import io.quarkiverse.fx.QuarkusFxApplication;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.QuarkusApplicationClassBuildItem;
import io.quarkus.runtime.annotations.QuarkusMain;

class QuarkusFxExtensionProcessor {

    private static final String FEATURE = "quarkus-fx";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem fxmlLoader() {
        return new AdditionalBeanBuildItem(FXMLLoaderProducer.class);
    }

    @BuildStep
    AdditionalBeanBuildItem primaryStage() {
        return new AdditionalBeanBuildItem(PrimaryStage.class);
    }

    @BuildStep
    void quarkusFxLauncher(
            final CombinedIndexBuildItem combinedIndex,
            final BuildProducer<AdditionalBeanBuildItem> additionalBean,
            final BuildProducer<QuarkusApplicationClassBuildItem> quarkusApplicationClass) {

        IndexView index = combinedIndex.getIndex();

        // Look for an existing @QuarkusMain annotation
        // If found, do nothing
        // Otherwise, provide a default QuarkusFxApplication that launches the FX application
        if (index.getAnnotations(DotName.createSimple(QuarkusMain.class.getName())).isEmpty()) {
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(QuarkusFxApplication.class));
            quarkusApplicationClass.produce(new QuarkusApplicationClassBuildItem(QuarkusFxApplication.class));
        }
    }
}
