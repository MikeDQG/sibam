package com.sibam.graph.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sibam.graph.model.GeoPoint;

import java.util.List;

public record RouteAlternative(
        int rank,
        GeoPoint origin,
        @JsonProperty("origin_address")
        String originAddress,
        GeoPoint destination,
        @JsonProperty("destination_address")
        String destinationAddress,
        String label,
        long totalDurationSeconds,
        int totalDistanceMeters,
        List<String> modes,
        List<Leg> legs
) {
}
