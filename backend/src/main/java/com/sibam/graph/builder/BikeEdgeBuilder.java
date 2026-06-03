package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;

import java.util.List;

public class BikeEdgeBuilder {
    public Edge build(
            int fromNodeId,
            int toNodeId,
            int distanceMeters,
            int travelTimeSeconds
    ) {
        return new Edge(
                fromNodeId,
                toNodeId,
                EdgeType.BIKE,
                distanceMeters,
                travelTimeSeconds,
                null,
                List.of()
        );
    }
}
