package io.quarkiverse.fx;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import javafx.application.Application;
import javafx.stage.Stage;

public class FxApplication extends Application {

  @Override
  public void start(final Stage primaryStage) {

    CDI.current()
      .getBeanManager()
      .getEvent()
      .select(new AnnotationLiteral<PrimaryStage>() {})
      .fire(primaryStage);
  }
}
