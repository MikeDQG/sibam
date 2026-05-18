package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.RouteInfo;

import java.util.List;

public class BusEdgeBuilder {

    public Edge build(
            int fromNodeId,
            int toNodeId,
            int distanceMeters,
            int travelTimeSeconds,
            RouteInfo routeInfo,
            List<GeoPoint> polyline
    ) {
        return new Edge(
                fromNodeId,
                toNodeId,
                EdgeType.BUS,
                distanceMeters,
                travelTimeSeconds,
                routeInfo,
                polyline
        );
    }
}
