package com.sibam.graph.model.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sibam.engine.vao.BikeLegPredictionVao;
import com.sibam.engine.vao.BusLegDelayVao;
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
        String arrival,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean navigationAvailable,
        List<NavigationStep> steps,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BikeLegPredictionVao bikePrediction,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BusLegDelayVao busDelayPrediction
) {
    public Leg(
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
        this(
                mode,
                origin,
                destination,
                duration,
                distance,
                polyline,
                code,
                headsignName,
                freeStands,
                freeBikes,
                departure,
                arrival,
                null,
                null,
                null,
                null
        );
    }
}
