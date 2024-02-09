package io.quarkiverse.fx.sample;

import io.quarkiverse.fx.RunOnFxThread;
import io.quarkus.logging.Log;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

@Singleton
public class AppController {

  @FXML
  private Label timeLabel;

  @FXML
  private void initialize() {
    //
  }

  @RunOnFxThread
  public void onMessage(final String timeString) {
    this.timeLabel.setText(timeString);
  }

  void onMessage(@Observes final TimeEvent timeEvent) {
    Log.infof("Time: %s;%s", timeEvent.unixTime(), timeEvent.timeString());
    this.onMessage(timeEvent.timeString());
  }
}
