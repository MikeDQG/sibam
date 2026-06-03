package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BikeEdgeBuilderTest {

    private final BikeEdgeBuilder builder = new BikeEdgeBuilder();

    @Test
    void buildCreatesBikeEdge() {
        Edge edge = builder.build(1, 2, 500, 120);
        assertThat(edge.getEdgeType()).isEqualTo(EdgeType.BIKE);
    }

    @Test
    void fromAndToNodeIdsAreSet() {
        Edge edge = builder.build(5, 10, 500, 120);
        assertThat(edge.getFromNodeId()).isEqualTo(5);
        assertThat(edge.getToNodeId()).isEqualTo(10);
    }

    @Test
    void distanceAndCostArePreserved() {
        Edge edge = builder.build(1, 2, 800, 200);
        assertThat(edge.getDistanceMeters()).isEqualTo(800);
        assertThat(edge.getCostSeconds()).isEqualTo(200);
    }

    @Test
    void routeInfoIsNull() {
        Edge edge = builder.build(1, 2, 500, 120);
        assertThat(edge.getRouteInfo()).isNull();
    }
}
