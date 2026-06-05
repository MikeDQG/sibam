package com.sibam.graph.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sibam.graph.model.GeoPoint;

import java.util.List;

/**
 * Javna alternativa poti v odgovoru /compute.
 *
 * Združuje rang, oznake načina, skupno trajanje/razdaljo in zaporedje etap,
 * ki jih frontend prikaže na zemljevidu.
 */
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
