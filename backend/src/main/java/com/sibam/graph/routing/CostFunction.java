package com.sibam.graph.routing;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;

public interface CostFunction {

    /**
     * Calculates the cost in seconds for traversing an edge.
     * Applies dynamic weights based on edge type to favor transit over long-distance biking/walking.
     *
     * @param edge the edge to calculate cost for
     * @return cost in seconds
     */
    int calculateCost(Edge edge);

    /**
     * Applies mode penalty for edges.
     * - Transit edges: no penalty (baseWeight = 1.0)
     * - Bike edges above the configured threshold: long-distance bike multiplier
     * - Bike edges at or below the configured threshold: short-distance bike multiplier
     * - Walk edges above the configured threshold: long-distance walk multiplier
     *
     * @param edgeType the type of edge
     * @param distanceMeters the distance of the edge in meters
     * @param baseCostSeconds the base cost in seconds
     * @return adjusted cost in seconds
     */
    int applyModePenalty(EdgeType edgeType, double distanceMeters, int baseCostSeconds);
}
