package com.sibam.graph.model.output;

import java.util.List;

public record RouteAlternative(
        int rank,
        String label,
        List<String> labels,
        long totalDurationSeconds,
        int totalDistanceMeters,
        List<String> modes,
        List<Leg> legs
) {
}
