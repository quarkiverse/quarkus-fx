package io.quarkiverse.fx.sample;

import java.io.IOException;

public interface UnixTimeProvider {
    // Provides the seconds elapsed since the Unix epoch 1970-01-01T00:00:00Z
    long getTime() throws IOException;
}
