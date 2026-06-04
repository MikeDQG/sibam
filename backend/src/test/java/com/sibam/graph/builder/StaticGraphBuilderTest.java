package com.sibam.graph.builder;

import com.sibam.engine.VaoSerializer;
import com.sibam.engine.vao.BikeAvailabilityVao;
import com.sibam.engine.vao.BikeStationVao;
import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.engine.vao.ShapeNodeVao;
import com.sibam.graph.model.BikeNode;
import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.Graph;
import com.sibam.graph.spatial.HelperService;
import com.sibam.service.MBajkDataService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaticGraphBuilderTest {

    private static final int BIKE_NODE_OFFSET = 1_000_000;

    private final HelperService helperService = new HelperService();

    private StaticGraphBuilder builder(VaoSerializer vao, MBajkDataService mbajk) {
        return new StaticGraphBuilder(vao, mbajk, helperService);
    }

    private VaoSerializer vaoWith(Map<Integer, BusStopVao> stops, Map<Integer, RouteVao> routes) {
        VaoSerializer vao = mock(VaoSerializer.class);
        when(vao.getBusStopsMap()).thenReturn(stops);
        when(vao.getRoutesMap()).thenReturn(routes);
        return vao;
    }

    private MBajkDataService mbajkWith(List<BikeStationVao> stations) {
        MBajkDataService svc = mock(MBajkDataService.class);
        when(svc.getBikeStationVaos()).thenReturn(stations);
        return svc;
    }

    private BusStopVao stop(int id, double lat, double lon) {
        return new BusStopVao(id, "Stop " + id, "Addr", lat, lon);
    }

    private BikeStationVao station(int number, double lat, double lon, int freeBikes, int freeStands) {
        return new BikeStationVao(
                number, "Bike " + number, "Addr", lat, lon, 20,
                new BikeAvailabilityVao(freeBikes, freeStands, 0, 0, "OPEN", OffsetDateTime.now())
        );
    }

    // ---- bus node creation ----

    @Test
    void buildCreatesBusNodeForEachStop() {
        VaoSerializer vao = vaoWith(
                Map.of(1, stop(1, 46.550, 15.600), 2, stop(2, 46.560, 15.610)),
                Map.of()
        );
        Graph graph = builder(vao, mbajkWith(List.of())).build();

        assertThat(graph.getNodes()).hasSize(2);
        assertThat(graph.getNodes().values()).allMatch(BusNode.class::isInstance);
    }

    @Test
    void buildDoesNotCallFetchDataWhenStopsArePresent() {
        VaoSerializer vao = vaoWith(Map.of(1, stop(1, 46.55, 15.60)), Map.of());

        builder(vao, mbajkWith(List.of())).build();

        verify(vao, never()).fetchData();
    }

    @Test
    void buildCallsFetchDataWhenBusStopsMapIsEmpty() {
        Map<Integer, BusStopVao> populated = Map.of(1, stop(1, 46.55, 15.60));
        VaoSerializer vao = mock(VaoSerializer.class);
        when(vao.getBusStopsMap())
                .thenReturn(Map.of())   // null check: not null
                .thenReturn(Map.of())   // isEmpty check: empty → triggers fetchData
                .thenReturn(populated); // for-loop
        when(vao.getRoutesMap()).thenReturn(Map.of());

        Graph graph = builder(vao, mbajkWith(List.of())).build();

        verify(vao).fetchData();
        assertThat(graph.getNodes()).hasSize(1);
    }

    @Test
    void buildCallsFetchDataWhenBusStopsMapIsNull() {
        Map<Integer, BusStopVao> populated = Map.of(1, stop(1, 46.55, 15.60));
        VaoSerializer vao = mock(VaoSerializer.class);
        when(vao.getBusStopsMap())
                .thenReturn(null)       // null check → triggers fetchData
                .thenReturn(populated); // for-loop
        when(vao.getRoutesMap()).thenReturn(Map.of());

        builder(vao, mbajkWith(List.of())).build();

        verify(vao).fetchData();
    }

    @Test
    void buildSkipsBusStopWithNullCoordinates() {
        BusStopVao nullCoords = new BusStopVao(99, "Bad", "Addr", null, null);
        VaoSerializer vao = vaoWith(Map.of(99, nullCoords), Map.of());

        Graph graph = builder(vao, mbajkWith(List.of())).build();

        assertThat(graph.getNodes()).isEmpty();
    }

    // ---- bike node creation ----

    @Test
    void buildCreatesBikeNodesFromMBajkService() {
        VaoSerializer vao = vaoWith(Map.of(), Map.of());
        List<BikeStationVao> stations = List.of(
                station(1, 46.550, 15.600, 5, 10),
                station(2, 46.560, 15.610, 3, 7)
        );

        Graph graph = builder(vao, mbajkWith(stations)).build();

        assertThat(graph.getNodes()).hasSize(2);
        assertThat(graph.getNodes().values()).allMatch(BikeNode.class::isInstance);
        assertThat(graph.getNodes()).containsKey(BIKE_NODE_OFFSET + 1);
        assertThat(graph.getNodes()).containsKey(BIKE_NODE_OFFSET + 2);
    }

    @Test
    void buildBikeNodeWithNullAvailabilityUsesZeroFreeBikesAndStands() {
        BikeStationVao noAvail = new BikeStationVao(1, "S", "A", 46.55, 15.60, 10, null);
        VaoSerializer vao = vaoWith(Map.of(), Map.of());

        Graph graph = builder(vao, mbajkWith(List.of(noAvail))).build();

        BikeNode node = (BikeNode) graph.getNodes().get(BIKE_NODE_OFFSET + 1);
        assertThat(node.getFreeBikes()).isZero();
        assertThat(node.getFreeStands()).isZero();
    }

    // ---- bike edge creation ----

    @Test
    void buildAddsBikeEdgesBetweenStationsWithFreeBikesAndStands() {
        List<BikeStationVao> stations = List.of(
                station(1, 46.550, 15.600, 5, 10),
                station(2, 46.551, 15.601, 3, 7)
        );
        VaoSerializer vao = vaoWith(Map.of(), Map.of());

        Graph graph = builder(vao, mbajkWith(stations)).build();

        int n1 = BIKE_NODE_OFFSET + 1;
        int n2 = BIKE_NODE_OFFSET + 2;
        assertThat(graph.getAdjacencyList().get(n1))
                .anyMatch(e -> e.getToNodeId() == n2 && e.getEdgeType() == EdgeType.BIKE);
        assertThat(graph.getAdjacencyList().get(n2))
                .anyMatch(e -> e.getToNodeId() == n1 && e.getEdgeType() == EdgeType.BIKE);
    }

    @Test
    void buildSkipsBikeEdgeFromStationWithNoFreeBikes() {
        List<BikeStationVao> stations = List.of(
                station(1, 46.550, 15.600, 0, 10), // no free bikes
                station(2, 46.551, 15.601, 3, 7)
        );
        VaoSerializer vao = vaoWith(Map.of(), Map.of());

        Graph graph = builder(vao, mbajkWith(stations)).build();

        int n1 = BIKE_NODE_OFFSET + 1;
        assertThat(graph.getAdjacencyList().get(n1))
                .noneMatch(e -> e.getEdgeType() == EdgeType.BIKE);
    }

    @Test
    void buildSkipsBikeEdgeToStationWithNoFreeStands() {
        List<BikeStationVao> stations = List.of(
                station(1, 46.550, 15.600, 5, 10),
                station(2, 46.551, 15.601, 3, 0)  // no free stands
        );
        VaoSerializer vao = vaoWith(Map.of(), Map.of());

        Graph graph = builder(vao, mbajkWith(stations)).build();

        int n1 = BIKE_NODE_OFFSET + 1;
        assertThat(graph.getAdjacencyList().get(n1))
                .noneMatch(e -> e.getEdgeType() == EdgeType.BIKE);
    }

    // ---- bus edge creation ----

    @Test
    void buildAddsBusEdgeBetweenConsecutiveBusStopShapeNodes() {
        List<ShapeNodeVao> shape = List.of(
                new ShapeNodeVao(1, 46.550, 15.600, 100),
                new ShapeNodeVao(2, 46.551, 15.601, 101)
        );
        RouteVao route = new RouteVao(1, 1, "R1", "Dest", "Head", shape, List.of());
        VaoSerializer vao = vaoWith(
                Map.of(100, stop(100, 46.550, 15.600), 101, stop(101, 46.551, 15.601)),
                Map.of(1, route)
        );

        Graph graph = builder(vao, mbajkWith(List.of())).build();

        assertThat(graph.getAdjacencyList().get(100))
                .anyMatch(e -> e.getToNodeId() == 101 && e.getEdgeType() == EdgeType.BUS);
    }

    @Test
    void buildNoBusEdgesWhenRoutesMapIsEmpty() {
        VaoSerializer vao = vaoWith(Map.of(1, stop(1, 46.55, 15.60)), Map.of());

        Graph graph = builder(vao, mbajkWith(List.of())).build();

        assertThat(graph.getAdjacencyList().get(1))
                .noneMatch(e -> e.getEdgeType() == EdgeType.BUS);
    }

    @Test
    void buildSkipsRouteWithNullShapeNodes() {
        RouteVao route = new RouteVao(1, 1, "R1", "Dest", "Head", null, List.of());
        VaoSerializer vao = vaoWith(
                Map.of(1, stop(1, 46.55, 15.60)),
                Map.of(1, route)
        );

        Graph graph = builder(vao, mbajkWith(List.of())).build();

        assertThat(graph.getAdjacencyList().get(1))
                .noneMatch(e -> e.getEdgeType() == EdgeType.BUS);
    }

    @Test
    void buildSkipsRouteWithEmptyShapeNodes() {
        RouteVao route = new RouteVao(1, 1, "R1", "Dest", "Head", List.of(), List.of());
        VaoSerializer vao = vaoWith(
                Map.of(1, stop(1, 46.55, 15.60)),
                Map.of(1, route)
        );

        Graph graph = builder(vao, mbajkWith(List.of())).build();

        assertThat(graph.getAdjacencyList().get(1))
                .noneMatch(e -> e.getEdgeType() == EdgeType.BUS);
    }

    @Test
    void buildSkipsBusEdgeWhenOnlyOneStopInShapeSequence() {
        List<ShapeNodeVao> shape = List.of(
                new ShapeNodeVao(1, 46.550, 15.600, null),  // waypoint, not a stop
                new ShapeNodeVao(2, 46.551, 15.601, 100)    // only one bus stop → no edge
        );
        RouteVao route = new RouteVao(1, 1, "R1", "Dest", "Head", shape, List.of());
        VaoSerializer vao = vaoWith(
                Map.of(100, stop(100, 46.551, 15.601)),
                Map.of(1, route)
        );

        Graph graph = builder(vao, mbajkWith(List.of())).build();

        assertThat(graph.getAdjacencyList().get(100))
                .noneMatch(e -> e.getEdgeType() == EdgeType.BUS);
    }

    // ---- walking edge creation ----

    @Test
    void buildAddsWalkingEdgesBetweenNearbyNodes() {
        // ~130m apart — within the 500m threshold
        VaoSerializer vao = vaoWith(
                Map.of(1, stop(1, 46.550, 15.600), 2, stop(2, 46.551, 15.601)),
                Map.of()
        );

        Graph graph = builder(vao, mbajkWith(List.of())).build();

        assertThat(graph.getAdjacencyList().get(1))
                .anyMatch(e -> e.getToNodeId() == 2 && e.getEdgeType() == EdgeType.WALK);
        assertThat(graph.getAdjacencyList().get(2))
                .anyMatch(e -> e.getToNodeId() == 1 && e.getEdgeType() == EdgeType.WALK);
    }

    @Test
    void buildDoesNotAddWalkingEdgesBetweenDistantNodes() {
        // ~7km apart — well outside the 500m threshold
        VaoSerializer vao = vaoWith(
                Map.of(1, stop(1, 46.55, 15.60), 2, stop(2, 46.60, 15.65)),
                Map.of()
        );

        Graph graph = builder(vao, mbajkWith(List.of())).build();

        assertThat(graph.getAdjacencyList().get(1))
                .noneMatch(e -> e.getEdgeType() == EdgeType.WALK);
    }
}
