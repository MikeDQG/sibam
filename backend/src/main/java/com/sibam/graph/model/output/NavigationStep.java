package com.sibam.graph.model.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NavigationStep(
        String instruction,
        String maneuver,
        int distanceMeters,
        long durationSeconds,
        JsonNode polyline
) {
}
