package io.quarkiverse.fx.sample;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkus.scheduler.Scheduled.SkipPredicate;
import io.quarkus.scheduler.ScheduledExecution;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

@Singleton
public class FxApplicationNotStarted implements SkipPredicate {

    private volatile boolean started;

    void onFxStartup(@Observes FxPostStartupEvent event) {
        this.started = true;
    }

    @Override
    public boolean test(ScheduledExecution execution) {
        return !this.started;
    }
}
