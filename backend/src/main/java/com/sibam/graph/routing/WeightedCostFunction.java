package com.sibam.graph.routing;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WeightedCostFunction implements CostFunction {

    private final RoutingConfig routingConfig;
    private final WeatherRoutingAdjuster weatherRoutingAdjuster;

    @Autowired
    public WeightedCostFunction(
            RoutingConfig routingConfig,
            WeatherRoutingAdjuster weatherRoutingAdjuster
    ) {
        this.routingConfig = routingConfig;
        this.weatherRoutingAdjuster = weatherRoutingAdjuster;
    }

    public WeightedCostFunction(RoutingConfig routingConfig) {
        this(routingConfig, null);
    }

    @Override
    public int calculateCost(Edge edge) {
        return calculateCost(edge, WeatherRoutingContext.neutral());
    }

    @Override
    public int calculateCost(Edge edge, WeatherRoutingContext weatherContext) {
        return applyModePenalty(
                edge.getEdgeType(),
                edge.getDistanceMeters(),
                edge.getCostSeconds(),
                weatherContext
        );
    }

    @Override
    public int applyModePenalty(EdgeType edgeType, double distanceMeters, int baseCostSeconds) {
        return applyModePenalty(edgeType, distanceMeters, baseCostSeconds, WeatherRoutingContext.neutral());
    }

    private int applyModePenalty(
            EdgeType edgeType,
            double distanceMeters,
            int baseCostSeconds,
            WeatherRoutingContext weatherContext
    ) {
        int weightedCost = applyPreferencePenalty(edgeType, distanceMeters, baseCostSeconds);
        if (weatherRoutingAdjuster == null) {
            return weightedCost;
        }

        return weatherRoutingAdjuster.adjustedEdgeCost(edgeType, weightedCost, weatherContext);
    }

    private int applyPreferencePenalty(EdgeType edgeType, double distanceMeters, int baseCostSeconds) {
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
