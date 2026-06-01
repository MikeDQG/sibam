package com.sibam.graph.routing;

import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.Node;
import com.sibam.graph.spatial.DistanceCalculator;
import org.springframework.stereotype.Service;

@Service
public class HeuristicService {

    private static final double HEURISTIC_MAX_SPEED_MPS = 30.0;
    private static final double WALK_HEURISTIC_SPEED_MPS = 2.0;
    private static final double BIKE_HEURISTIC_SPEED_MPS = 6.0;

    public double estimate(Node current, Node goal) {
        return estimate(current, goal, null);
    }

    public double estimate(Node current, Node goal, EdgeType edgeType) {
        return estimateDistanceMeters(current, goal, edgeType) / heuristicSpeedMps(edgeType);
    }

    public double estimateDistanceMeters(Node current, Node goal, EdgeType edgeType) {
        GeoPoint currentPoint = new GeoPoint(current.getLat(), current.getLon());
        GeoPoint goalPoint = new GeoPoint(goal.getLat(), goal.getLon());

        if (edgeType == EdgeType.WALK || edgeType == EdgeType.BIKE) {
            return DistanceCalculator.correctedDistanceMeters(currentPoint, goalPoint, edgeType);
        }

        return DistanceCalculator.haversineMeters(
                current.getLat(),
                current.getLon(),
                goal.getLat(),
                goal.getLon()
        );
    }

    private double heuristicSpeedMps(EdgeType edgeType) {
        if (edgeType == EdgeType.WALK) {
            return WALK_HEURISTIC_SPEED_MPS;
        }

        if (edgeType == EdgeType.BIKE) {
            return BIKE_HEURISTIC_SPEED_MPS;
        }

        return HEURISTIC_MAX_SPEED_MPS;
    }
}
