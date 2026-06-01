package com.sibam.graph.routing;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import org.springframework.stereotype.Component;

@Component
public class WeightedCostFunction implements CostFunction {

    private final RoutingConfig routingConfig;

    public WeightedCostFunction(RoutingConfig routingConfig) {
        this.routingConfig = routingConfig;
    }

    @Override
    public int calculateCost(Edge edge) {
        return applyModePenalty(edge.getEdgeType(), edge.getDistanceMeters(), edge.getCostSeconds());
    }

    @Override
    public int applyModePenalty(EdgeType edgeType, double distanceMeters, int baseCostSeconds) {
        if (edgeType == EdgeType.WALK && distanceMeters > routingConfig.getBikeDistanceThresholdMeters()) {
            return weightedCost(baseCostSeconds, routingConfig.getWalkLongDistanceMultiplier());
        }

        if (edgeType == EdgeType.BIKE && distanceMeters > routingConfig.getBikeDistanceThresholdMeters()) {
            return weightedCost(baseCostSeconds, routingConfig.getBikeLongDistanceMultiplier());
        }

        if (edgeType == EdgeType.BIKE) {
            return weightedCost(baseCostSeconds, routingConfig.getBikeShortDistanceMultiplier());
        }

        return baseCostSeconds;
    }

    private int weightedCost(int baseCostSeconds, double multiplier) {
        return (int) Math.max(1, Math.round(baseCostSeconds * multiplier));
    }
}
