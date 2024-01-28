package io.quarkiverse.fx.deployment;

import io.quarkiverse.fx.FXMLLoaderProducer;
import io.quarkiverse.fx.PrimaryStage;
import io.quarkiverse.fx.QuarkusFxLaunchRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

class QuarkusFxExtensionProcessor {

  private static final String FEATURE = "quarkus-fx-extension";

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

  @Record(RUNTIME_INIT)
  @BuildStep(onlyIf = IsAutoLaunch.class)
  void launchFxApplication(final QuarkusFxLaunchRecorder recorder) {
    recorder.launchFxApplication();
  }
}
