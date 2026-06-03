package com.sibam.graph.builder;

import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.Node;
import com.sibam.graph.spatial.HelperService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalkingEdgeBuilderTest {

    private final WalkingEdgeBuilder builder = new WalkingEdgeBuilder(new HelperService());

    private Node node(int id, double lat, double lon) {
        return new BusNode(id, lat, lon, "Stop " + id);
    }

    @Test
    void buildCreatesWalkEdge() {
        Edge edge = builder.build(node(1, 46.550, 15.640), node(2, 46.551, 15.641));
        assertThat(edge.getEdgeType()).isEqualTo(EdgeType.WALK);
    }

    @Test
    void fromAndToNodeIdsAreCorrect() {
        Edge edge = builder.build(node(3, 46.550, 15.640), node(7, 46.551, 15.641));
        assertThat(edge.getFromNodeId()).isEqualTo(3);
        assertThat(edge.getToNodeId()).isEqualTo(7);
    }

    @Test
    void distanceIsPositiveForDifferentCoordinates() {
        Edge edge = builder.build(node(1, 46.550, 15.640), node(2, 46.560, 15.650));
        assertThat(edge.getDistanceMeters()).isGreaterThan(0);
    }

    @Test
    void walkingSecondsIsAtLeastOne() {
        Edge edge = builder.build(node(1, 46.550, 15.640), node(2, 46.550, 15.640));
        assertThat(edge.getCostSeconds()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void polylineContainsFromAndToPoints() {
        Edge edge = builder.build(node(1, 46.550, 15.640), node(2, 46.560, 15.650));
        assertThat(edge.getPolyline()).hasSize(2);
        assertThat(edge.getPolyline().get(0).lat()).isEqualTo(46.550);
        assertThat(edge.getPolyline().get(1).lat()).isEqualTo(46.560);
    }

    @Test
    void longerDistanceProducesMoreWalkingSeconds() {
        Edge short_ = builder.build(node(1, 46.550, 15.640), node(2, 46.551, 15.641));
        Edge long_  = builder.build(node(1, 46.550, 15.640), node(2, 46.560, 15.650));
        assertThat(long_.getCostSeconds()).isGreaterThan(short_.getCostSeconds());
    }
}
