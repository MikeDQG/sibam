package com.sibam.graph.routing;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PathResultTest {

    @Test
    void nodeOnlyConstructorInitializesEmptyEdgesAndTimings() {
        PathResult result = new PathResult(List.of(1, 2, 3), 120);

        assertThat(result.getNodeIds()).containsExactly(1, 2, 3);
        assertThat(result.getEdges()).isEmpty();
        assertThat(result.getTimings()).isEmpty();
        assertThat(result.getTotalCostSeconds()).isEqualTo(120);
    }

    @Test
    void edgeConstructorInitializesEmptyTimings() {
        Edge edge = new Edge(1, 2, EdgeType.WALK, 100, 80);

        PathResult result = new PathResult(List.of(1, 2), List.of(edge), 80);

        assertThat(result.getNodeIds()).containsExactly(1, 2);
        assertThat(result.getEdges()).containsExactly(edge);
        assertThat(result.getTimings()).isEmpty();
        assertThat(result.getTotalCostSeconds()).isEqualTo(80);
    }

    @Test
    void fullConstructorStoresEdgesTimingsAndTotalCost() {
        Edge edge = new Edge(1, 2, EdgeType.BIKE, 250, 60);
        PathStepTiming timing = new PathStepTiming(1_000L, 61_000L);

        PathResult result = new PathResult(List.of(1, 2), List.of(edge), List.of(timing), 60);

        assertThat(result.getNodeIds()).containsExactly(1, 2);
        assertThat(result.getEdges()).containsExactly(edge);
        assertThat(result.getTimings()).containsExactly(timing);
        assertThat(result.getTotalCostSeconds()).isEqualTo(60);
    }
}
