package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.Node;
import com.sibam.graph.spatial.DistanceCalculator;
import com.sibam.graph.spatial.HelperService;

import java.util.List;

public class WalkingEdgeBuilder {

    private final HelperService helperService;

    public WalkingEdgeBuilder(HelperService helperService) {
        this.helperService = helperService;
    }

    public Edge build(Node from, Node to) {
        GeoPoint fromPoint = new GeoPoint(from.getLat(), from.getLon());
        GeoPoint toPoint = new GeoPoint(to.getLat(), to.getLon());
        int distance = (int) Math.round(DistanceCalculator.correctedDistanceMeters(
                fromPoint,
                toPoint,
                EdgeType.WALK
        ));
        int walkingSeconds = (int) Math.max(1, Math.round(distance / helperService.getWalkingSpeedMps()));

        return new Edge(
                from.getId(),
                to.getId(),
                EdgeType.WALK,
                distance,
                walkingSeconds,
                null,
                List.of(
                        fromPoint,
                        toPoint
                )
        );
    }
}
