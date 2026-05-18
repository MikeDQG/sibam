package com.sibam.graph.model.output;

import com.sibam.graph.model.GeoPoint;

import java.util.List;

public record Leg(
        String mode,
        GeoPoint origin,
        GeoPoint destination,
        String duration,
        String distance,
        List<GeoPoint> polyline,
        String code,
        String headsignName,
        String freeStands,
        String freeBikes,
        String departure,
        String arrival
) {
}
