package io.quarkiverse.fx.sample;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient
public interface StarWarsRestClient {

    @Path("/planets")
    @GET
    Uni<PlanetsResult> getPlanets();

    @Path("/people")
    @GET
    Uni<PeopleResult> getPeople();
}
