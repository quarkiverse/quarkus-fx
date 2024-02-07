package io.quarkiverse.fx.sample;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import io.quarkiverse.fx.RunOnFxThread;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import javafx.stage.Stage;

/**
 * A sample CDI bean that receives Quarkus scheduler events that are routed back to the javafx interface
 */
@ApplicationScoped
public class ClockEvents {
    @Inject
    UnixTimeProvider timeProvider;
    @Inject
    Event<TimeEvent> timeEvent;

    /**
     * A scheduler callback method that delegates to method that is to be run on the
     * JavaFX creates an application thread. This allows for better startup behavior
     * than direct use of javafx.application.Platform#runLater(...) as the interceptor
     * associated with @RunOnFxThread is able to synchronize with the
     * {@linkplain io.quarkiverse.fx.FxApplication#start(javafx.stage.Stage)} completion.
     */
    @Scheduled(every = "1s")
    @RunOnFxThread
    void timeIncrement() {
        Log.info("timeIncrement");
        sendTimeEvent();
    }

    private void sendTimeEvent() {
        try {
            long unixTime = timeProvider.getTime();
            String timeString = String.format("%016x", unixTime);
            TimeEvent event = new TimeEvent(unixTime, timeString);
            Log.infof("%d/%s", unixTime, timeString);
            timeEvent.fireAsync(event);
        } catch (IOException e) {
            Log.error("Failed to send TimeEvent", e);
        }
    }
}
