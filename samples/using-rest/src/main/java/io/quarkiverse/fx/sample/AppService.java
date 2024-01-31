package io.quarkiverse.fx.sample;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class AppService {

    @Inject
    @RestClient
    StarWarsRestClient starWarsRestClient;

    public Uni<PlanetsResult> getPlanets() {
        return this.starWarsRestClient.getPlanets();
    }

    public Uni<PeopleResult> getPeople() {
        return this.starWarsRestClient.getPeople();
    }
}
