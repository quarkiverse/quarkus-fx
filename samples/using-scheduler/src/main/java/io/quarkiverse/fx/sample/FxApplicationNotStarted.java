package io.quarkiverse.fx.sample;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkiverse.fx.FxStartupEvent;
import io.quarkus.scheduler.Scheduled.SkipPredicate;
import io.quarkus.scheduler.ScheduledExecution;

@Singleton
public class FxApplicationNotStarted implements SkipPredicate {

    private volatile boolean started;

    void onFxStartup(@Observes final FxStartupEvent event) {
        this.started = true;
    }

    @Override
    public boolean test(final ScheduledExecution execution) {
        return !this.started;
    }
}
