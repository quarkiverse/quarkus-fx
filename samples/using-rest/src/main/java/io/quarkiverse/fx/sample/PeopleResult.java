package io.quarkiverse.fx.sample;

import java.util.List;

public record PeopleResult(int count, List<People> results) {
}
