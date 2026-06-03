package com.sibam.graph.routing;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedCostFunctionTest {

    // Config: bikeThreshold=1000m, bikeLong=5.0x, walkLong=3.0x, bikeShort=1.5x
    private final RoutingConfig config = new RoutingConfig(300, 1000, 5.0, 3.0, 1.5);
    private final WeightedCostFunction fn = new WeightedCostFunction(config);

    @Test
    void busEdgeReturnsBaseCost() {
        Edge edge = new Edge(1, 2, EdgeType.BUS, 500, 100);
        assertThat(fn.calculateCost(edge)).isEqualTo(100);
    }

    @Test
    void shortBikeEdgeAppliesShortMultiplier() {
        Edge edge = new Edge(1, 2, EdgeType.BIKE, 500, 100);
        assertThat(fn.calculateCost(edge)).isEqualTo(150); // 100 * 1.5
    }

    @Test
    void longBikeEdgeAppliesLongMultiplier() {
        Edge edge = new Edge(1, 2, EdgeType.BIKE, 1001, 100);
        assertThat(fn.calculateCost(edge)).isEqualTo(500); // 100 * 5.0
    }

    @Test
    void bikeEdgeAtExactThresholdIsShort() {
        Edge edge = new Edge(1, 2, EdgeType.BIKE, 1000, 100);
        assertThat(fn.calculateCost(edge)).isEqualTo(150); // not > threshold → short
    }

    @Test
    void shortWalkEdgeReturnsBaseCost() {
        Edge edge = new Edge(1, 2, EdgeType.WALK, 500, 100);
        assertThat(fn.calculateCost(edge)).isEqualTo(100);
    }

    @Test
    void longWalkEdgeAppliesLongMultiplier() {
        Edge edge = new Edge(1, 2, EdgeType.WALK, 1001, 100);
        assertThat(fn.calculateCost(edge)).isEqualTo(300); // 100 * 3.0
    }

    @Test
    void costIsNeverBelowOne() {
        Edge edge = new Edge(1, 2, EdgeType.BIKE, 500, 0);
        assertThat(fn.calculateCost(edge)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void applyModePenaltyMatchesCalculateCost() {
        int direct = fn.calculateCost(new Edge(1, 2, EdgeType.BIKE, 500, 200));
        int via = fn.applyModePenalty(EdgeType.BIKE, 500, 200);
        assertThat(direct).isEqualTo(via);
    }
}
