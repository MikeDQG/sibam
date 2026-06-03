package com.sibam.graph.model.output;

import java.util.List;

public record RouteAlternative(
        int rank,
        String label,
        long totalDurationSeconds,
        int totalDistanceMeters,
        List<String> modes,
        List<Leg> legs
) {
}
