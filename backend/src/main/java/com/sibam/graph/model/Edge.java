package com.sibam.graph.model;

import java.util.List;

public class Edge {
    private final int fromNodeId;
    private final int toNodeId;
    private final EdgeType edgeType;
    private final RouteInfo routeInfo;
    private final List<GeoPoint> polyline;
    private final Integer scheduleStopPointId;

    private final int distanceMeters;
    private final int costSeconds;

    public Edge(
            int fromNodeId,
            int toNodeId,
            EdgeType edgeType,
            int distanceMeters,
            int costSeconds
    ) {
        this(fromNodeId, toNodeId, edgeType, distanceMeters, costSeconds, null, List.of());
    }

    public Edge(
            int fromNodeId,
            int toNodeId,
            EdgeType edgeType,
            int distanceMeters,
            int costSeconds,
            RouteInfo routeInfo
    ) {
        this(fromNodeId, toNodeId, edgeType, distanceMeters, costSeconds, routeInfo, List.of());
    }

    public Edge(
            int fromNodeId,
            int toNodeId,
            EdgeType edgeType,
            int distanceMeters,
            int costSeconds,
            RouteInfo routeInfo,
            List<GeoPoint> polyline
    ) {
        this(fromNodeId, toNodeId, edgeType, distanceMeters, costSeconds, routeInfo, polyline, null);
    }

    public Edge(
            int fromNodeId,
            int toNodeId,
            EdgeType edgeType,
            int distanceMeters,
            int costSeconds,
            RouteInfo routeInfo,
            List<GeoPoint> polyline,
            Integer scheduleStopPointId
    ) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.edgeType = edgeType;
        this.distanceMeters = distanceMeters;
        this.costSeconds = costSeconds;
        this.routeInfo = routeInfo;
        this.polyline = polyline == null ? List.of() : List.copyOf(polyline);
        this.scheduleStopPointId = scheduleStopPointId;
    }


    public int getFromNodeId() {
        return fromNodeId;
    }

    public int getToNodeId() {
        return toNodeId;
    }

    public EdgeType getEdgeType() {
        return edgeType;
    }

    public RouteInfo getRouteInfo() {
        return routeInfo;
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public int getCostSeconds() {
        return costSeconds;
    }

    public List<GeoPoint> getPolyline() {
        return polyline;
    }

    public Integer getScheduleStopPointId() {
        return scheduleStopPointId;
    }
}
