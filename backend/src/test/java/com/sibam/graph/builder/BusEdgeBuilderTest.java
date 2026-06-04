package com.sibam.graph.builder;

import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.RouteInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusEdgeBuilderTest {

    private final BusEdgeBuilder builder = new BusEdgeBuilder();

    private final RouteInfo route = new RouteInfo(1, 101, "Pobrežje", "6");

    @Test
    void buildCreatesBusEdge() {
        Edge edge = builder.build(1, 2, 300, 60, route, List.of(), null);
        assertThat(edge.getEdgeType()).isEqualTo(EdgeType.BUS);
    }

    @Test
    void fromAndToNodeIdsAreSet() {
        Edge edge = builder.build(3, 7, 300, 60, route, List.of(), null);
        assertThat(edge.getFromNodeId()).isEqualTo(3);
        assertThat(edge.getToNodeId()).isEqualTo(7);
    }

    @Test
    void distanceAndCostArePreserved() {
        Edge edge = builder.build(1, 2, 1200, 180, route, List.of(), null);
        assertThat(edge.getDistanceMeters()).isEqualTo(1200);
        assertThat(edge.getCostSeconds()).isEqualTo(180);
    }

    @Test
    void routeInfoIsPreserved() {
        Edge edge = builder.build(1, 2, 300, 60, route, List.of(), null);
        assertThat(edge.getRouteInfo()).isEqualTo(route);
    }

    @Test
    void polylineIsPreserved() {
        List<GeoPoint> polyline = List.of(new GeoPoint(46.55, 15.64), new GeoPoint(46.56, 15.65));
        Edge edge = builder.build(1, 2, 300, 60, route, polyline, null);
        assertThat(edge.getPolyline()).hasSize(2);
    }

    @Test
    void scheduleStopPointIdIsPreserved() {
        Edge edge = builder.build(1, 2, 300, 60, route, List.of(), 42);
        assertThat(edge.getScheduleStopPointId()).isEqualTo(42);
    }

    @Test
    void nullScheduleStopPointIdIsAllowed() {
        Edge edge = builder.build(1, 2, 300, 60, route, List.of(), null);
        assertThat(edge.getScheduleStopPointId()).isNull();
    }
}
