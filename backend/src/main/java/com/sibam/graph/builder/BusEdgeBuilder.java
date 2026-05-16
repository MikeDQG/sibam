package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;

public class BusEdgeBuilder {

    public Edge build(
            int fromNodeId,
            int toNodeId,
            int distanceMeters,
            int travelTimeSeconds
    ) {
        return new Edge(
                fromNodeId,
                toNodeId,
                EdgeType.BUS,
                distanceMeters,
                travelTimeSeconds
        );
    }
}