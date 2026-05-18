package com.sibam.graph.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sibam.graph.model.GeoPoint;

import java.util.List;

public record Journey(
        String status,
        GeoPoint origin,
        @JsonProperty("origin_address")
        String originAddress,
        GeoPoint destination,
        @JsonProperty("destination_address")
        String destinationAddress,
        String duration,
        String distance,
        List<Leg> legs
) {
}
