package com.sibam.graph.model.output;

import java.util.List;

/**
 * Javna alternativa poti v odgovoru /compute.
 *
 * Združuje rang, oznake načina, skupno trajanje/razdaljo in zaporedje etap,
 * ki jih frontend prikaže na zemljevidu.
 */
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
