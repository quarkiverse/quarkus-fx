package io.quarkiverse.fx.sample;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SystemUnixTimeProvider implements UnixTimeProvider {
    @Override
    public long getTime() {
        return Instant.now().getEpochSecond();
    }
}
