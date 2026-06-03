package com.sibam.graph.model.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sibam.graph.model.GeoPoint;

import java.util.List;

public record RouteAlternativesResponse(
        String status,
        GeoPoint origin,
        @JsonProperty("origin_address")
        String originAddress,
        GeoPoint destination,
        @JsonProperty("destination_address")
        String destinationAddress,
        List<RouteAlternative> routes,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        RouteAlternative route,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        RouteAlternative bestRoute
) {
}
