package com.sibam.graph.model;

public class Edge {
    private final int fromNodeId;
    private final int toNodeId;
    private final EdgeType edgeType;

    private final int distanceMeters;
    private final int costSeconds;

    public Edge(
            int fromNodeId,
            int toNodeId,
            EdgeType edgeType,
            int distanceMeters,
            int costSeconds
    ) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.edgeType = edgeType;
        this.distanceMeters = distanceMeters;
        this.costSeconds = costSeconds;
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

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public int getCostSeconds() {
        return costSeconds;
    }
}
