package com.sibam.graph.routing;

import com.sibam.engine.VaoSerializer;
import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.Graph;
import com.sibam.graph.model.Node;
import com.sibam.graph.model.RouteInfo;
import com.sibam.graph.model.output.Journey;
import com.sibam.graph.model.output.Leg;
import com.sibam.graph.model.output.NavigationStep;
import com.sibam.graph.spatial.HelperService;
import com.sibam.graph.spatial.SpatialSearchService;
import com.sibam.graph.storage.InMemoryGraphStore;
import com.sibam.service.BusDelayPredictionService;
import com.sibam.service.GoogleRoutesService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class AStarRouterTest {

    @Test
    void journeyKeepsRequestedCoordinatesAndOnlyUsesNearestNodesForEdges() {
        double originLat = 46.538077;
        double originLon = 15.603520;
        double destinationLat = 46.540000;
        double destinationLon = 15.606000;

        Node nearbyStop = new BusNode(1, 46.539000, 15.604500, "Nearby stop");
        Graph graph = new Graph(
                Map.of(nearbyStop.getId(), nearbyStop),
                Map.of(nearbyStop.getId(), new ArrayList<>())
        );

        InMemoryGraphStore graphStore = new InMemoryGraphStore();
        graphStore.replaceGraph(graph);
        HelperService helperService = new HelperService();
        AStarRouter router = new AStarRouter(
                graphStore,
                new SpatialSearchService(helperService),
                helperService,
                mock(VaoSerializer.class),
                mock(GoogleRoutesService.class),
                new HeuristicService(),
                new WeightedCostFunction(routingConfig()),
                routingConfig(),
                null,
                null,
                null
        );

        Journey journey = router.findJourney(
                originLat,
                originLon,
                destinationLat,
                destinationLon,
                null,
                null,
                LocalTime.NOON,
                true,
                true
        );

        assertThat(journey).isNotNull();
        assertThat(journey.origin().lat()).isEqualTo(originLat);
        assertThat(journey.origin().lon()).isEqualTo(originLon);
        assertThat(journey.destination().lat()).isEqualTo(destinationLat);
        assertThat(journey.destination().lon()).isEqualTo(destinationLon);
        assertThat(journey.legs()).isNotEmpty();
        assertThat(journey.legs().getFirst().origin()).isEqualTo(journey.origin());
        assertThat(journey.legs().getLast().destination()).isEqualTo(journey.destination());
    }

    @Test
    void transferPenaltyAppliesAcrossWalkConnectorBetweenBusLines() {
        RouteInfo routeA = new RouteInfo(1, 101, "A", "1");
        RouteInfo routeB = new RouteInfo(2, 202, "B", "2");
        Graph graph = new Graph(
                Map.of(
                        1, new BusNode(1, 46.0, 15.0, "A"),
                        2, new BusNode(2, 46.0, 15.001, "B"),
                        3, new BusNode(3, 46.0, 15.002, "C"),
                        4, new BusNode(4, 46.0, 15.003, "D")
                ),
                Map.of(
                        1, List.of(bus(1, 2, 100, routeA)),
                        2, List.of(new Edge(2, 3, EdgeType.WALK, 10, 10)),
                        3, List.of(bus(3, 4, 100, routeB)),
                        4, List.of()
                )
        );

        PathResult result = routerFor(graph).findPath(1, 4);

        assertThat(result.getNodeIds()).containsExactly(1, 2, 3, 4);
        assertThat(result.getTotalCostSeconds()).isEqualTo(510);
    }

    @Test
    void sameNodeReachedWithDifferentLastBusRouteCanLeadToDifferentBestPath() {
        RouteInfo routeA = new RouteInfo(1, 101, "A", "1");
        RouteInfo routeB = new RouteInfo(2, 202, "B", "2");
        Graph graph = new Graph(
                Map.of(
                        1, new BusNode(1, 46.0, 15.0, "Start"),
                        2, new BusNode(2, 46.0, 15.001, "Transfer"),
                        3, new BusNode(3, 46.0, 15.002, "Same line approach"),
                        4, new BusNode(4, 46.0, 15.003, "Goal")
                ),
                Map.of(
                        1, List.of(
                                bus(1, 2, 100, routeA),
                                bus(1, 3, 190, routeB)
                        ),
                        2, List.of(bus(2, 4, 100, routeB)),
                        3, List.of(new Edge(3, 2, EdgeType.WALK, 10, 10)),
                        4, List.of()
                )
        );

        PathResult result = routerFor(graph).findPath(1, 4);

        assertThat(result.getNodeIds()).containsExactly(1, 3, 2, 4);
        assertThat(result.getTotalCostSeconds()).isEqualTo(300);
    }

    @Test
    void walkLegIncludesNavigationStepsWhenGoogleReturnsThem() {
        double originLat = 46.538077;
        double originLon = 15.603520;
        double destinationLat = 46.538493;
        double destinationLon = 15.611431;

        Node nearbyStop = new BusNode(1, 46.538100, 15.603600, "Nearby");
        Graph graph = new Graph(
                Map.of(nearbyStop.getId(), nearbyStop),
                Map.of(nearbyStop.getId(), new ArrayList<>())
        );

        GoogleRoutesService googleRoutesService = mock(GoogleRoutesService.class);
        when(googleRoutesService.fetchRouteDetails(any(), any(), eq(EdgeType.WALK)))
                .thenReturn(new GoogleRoutesService.RouteDetails(
                        List.of(
                                new com.sibam.graph.model.GeoPoint(originLat, originLon),
                                new com.sibam.graph.model.GeoPoint(destinationLat, destinationLon)
                        ),
                        List.of(new NavigationStep(
                                "Head east on Maroltova ulica toward Pohorska ulica",
                                "DEPART",
                                40,
                                6,
                                0,
                                1
                        ))
                ));

        Journey journey = routerFor(graph, googleRoutesService).findJourney(
                originLat,
                originLon,
                destinationLat,
                destinationLon,
                null,
                null,
                LocalTime.NOON,
                true,
                true
        );

        assertThat(journey.legs()).hasSize(1);
        assertThat(journey.legs().getFirst().mode()).isEqualTo("WALK");
        assertThat(journey.legs().getFirst().navigationAvailable()).isTrue();
        assertThat(journey.legs().getFirst().steps()).hasSize(1);
        assertThat(journey.legs().getFirst().steps().getFirst().instruction())
                .isEqualTo("Head east on Maroltova ulica toward Pohorska ulica");
        assertThat(journey.legs().getFirst().steps().getFirst().maneuver()).isEqualTo("DEPART");
    }

    @Test
    void sameStopBusChangeCreatesZeroDistanceTransferLeg() {
        RouteInfo routeA = new RouteInfo(1, 101, "A", "1");
        RouteInfo routeB = new RouteInfo(2, 202, "B", "2");
        Node start = new BusNode(1, 46.0, 15.0, "Start");
        Node transferStop = new BusNode(2, 46.0, 15.001, "Transfer");
        Node destination = new BusNode(3, 46.0, 15.002, "Destination");
        Graph graph = new Graph(
                Map.of(
                        start.getId(), start,
                        transferStop.getId(), transferStop,
                        destination.getId(), destination
                ),
                Map.of(
                        1, List.of(bus(1, 2, 1, routeA)),
                        2, List.of(bus(2, 3, 1, routeB)),
                        3, List.of()
                )
        );

        Journey journey = routerFor(graph, mock(GoogleRoutesService.class), routingConfig(0)).findJourney(
                start.getLat(),
                start.getLon(),
                destination.getLat(),
                destination.getLon(),
                null,
                null,
                LocalTime.NOON,
                true,
                true
        );

        assertThat(journey.legs()).extracting("mode")
                .containsExactly("WALK", "BUS", "TRANSFER", "BUS", "WALK");
        assertThat(journey.legs().get(2).distance()).isEqualTo("0");
        assertThat(journey.legs().get(2).origin()).isEqualTo(journey.legs().get(2).destination());
    }

    @Test
    void rejectsRouteWhenOriginIsTooFarFromNearestStop() {
        Node nearbyStop = new BusNode(1, 46.0, 15.0, "Only stop");
        Graph graph = new Graph(
                Map.of(nearbyStop.getId(), nearbyStop),
                Map.of(nearbyStop.getId(), new ArrayList<>())
        );

        assertThatThrownBy(() -> routerFor(graph).findJourney(
                46.5,
                15.5,
                nearbyStop.getLat(),
                nearbyStop.getLon(),
                null,
                null,
                LocalTime.NOON,
                true,
                true
        ))
                .isInstanceOf(RouteAccessDistanceException.class)
                .hasMessageContaining("Izhodišče");
    }

    // Bus delay prediction

    @Test
    void busLegIncludesBoardingDelayPrediction() throws Exception {
        RouteInfo route = new RouteInfo(84, 1001, "Pekre", "P18");
        Node start = new BusNode(1, 46.0, 15.0, "Start");
        Node mid   = new BusNode(2, 46.0, 15.001, "Mid");
        Node end   = new BusNode(3, 46.0, 15.002, "End");
        Graph graph = new Graph(
                Map.of(1, start, 2, mid, 3, end),
                Map.of(
                        1, List.of(bus(1, 2, 60, route)),
                        2, List.of(bus(2, 3, 60, route)),
                        3, List.of()
                )
        );

        BusDelayPredictionService predService = mock(BusDelayPredictionService.class);
        when(predService.predictDelay(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyFloat(), anyFloat(), anyFloat(), anyInt())).thenReturn(90);

        Journey journey = routerWithBusPrediction(graph, predService).findJourney(
                start.getLat(), start.getLon(), end.getLat(), end.getLon(),
                null, null, LocalTime.NOON, true, true
        );

        Leg busLeg = journey.legs().stream()
                .filter(l -> "BUS".equals(l.mode())).findFirst().orElseThrow();
        assertThat(busLeg.busDelayPrediction()).isNotNull();
        assertThat(busLeg.busDelayPrediction().predictedBoardingDelaySeconds()).isEqualTo(90);
    }

    @Test
    void busLegOmitsBoardingDelayWhenServiceThrows() throws Exception {
        RouteInfo route = new RouteInfo(84, 1001, "Pekre", "P18");
        Node start = new BusNode(1, 46.0, 15.0, "Start");
        Node end   = new BusNode(2, 46.0, 15.001, "End");
        Graph graph = new Graph(
                Map.of(1, start, 2, end),
                Map.of(1, List.of(bus(1, 2, 60, route)), 2, List.of())
        );

        BusDelayPredictionService predService = mock(BusDelayPredictionService.class);
        when(predService.predictDelay(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyFloat(), anyFloat(), anyFloat(), anyInt()))
                .thenAnswer(inv -> { throw new RuntimeException("model not loaded"); });

        Journey journey = routerWithBusPrediction(graph, predService).findJourney(
                start.getLat(), start.getLon(), end.getLat(), end.getLon(),
                null, null, LocalTime.NOON, true, true
        );

        Leg busLeg = journey.legs().stream()
                .filter(l -> "BUS".equals(l.mode())).findFirst().orElseThrow();
        assertThat(busLeg.busDelayPrediction()).isNull();
    }

    @Test
    void busLegUsesStopSequenceFromRoutesMap() throws Exception {
        int lineId = 84;
        RouteInfo route = new RouteInfo(lineId, 1001, "Pekre", "P18");
        // BusNode ID = stop ID used in the graph
        Node start = new BusNode(10, 46.0, 15.0, "Start");
        Node end   = new BusNode(20, 46.0, 15.001, "End");
        Graph graph = new Graph(
                Map.of(10, start, 20, end),
                Map.of(10, List.of(bus(10, 20, 60, route)), 20, List.of())
        );

        // Route has 5 stops; stop 10 is at position 3 (1-based)
        List<BusStopVao> stops = List.of(
                new BusStopVao(5,  "A", null, 46.0, 14.999),
                new BusStopVao(7,  "B", null, 46.0, 14.9995),
                new BusStopVao(10, "C", null, 46.0, 15.0),
                new BusStopVao(15, "D", null, 46.0, 15.0005),
                new BusStopVao(20, "E", null, 46.0, 15.001)
        );
        RouteVao routeVao = new RouteVao(1001, lineId, "P18", "Pekre", "Pekre", List.of(), stops);

        BusDelayPredictionService predService = mock(BusDelayPredictionService.class);
        when(predService.predictDelay(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyFloat(), anyFloat(), anyFloat(), anyInt())).thenReturn(60);

        VaoSerializer vaoSerializer = mock(VaoSerializer.class);
        when(vaoSerializer.getSchedulesMap()).thenReturn(Map.of());
        when(vaoSerializer.getRoutesMap()).thenReturn(Map.of(1001, routeVao));

        InMemoryGraphStore graphStore = new InMemoryGraphStore();
        graphStore.replaceGraph(graph);
        HelperService helperService = new HelperService();
        AStarRouter router = new AStarRouter(
                graphStore, new SpatialSearchService(helperService), helperService,
                vaoSerializer, mock(GoogleRoutesService.class), new HeuristicService(),
                new WeightedCostFunction(routingConfig()), routingConfig(),
                null, null, predService
        );

        router.findJourney(start.getLat(), start.getLon(), end.getLat(), end.getLon(),
                null, null, LocalTime.NOON, true, true);

        ArgumentCaptor<Integer> seqCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(predService).predictDelay(
                eq(lineId), seqCaptor.capture(),
                anyInt(), anyInt(), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyInt()
        );
        assertThat(seqCaptor.getValue()).isEqualTo(3); // stop 10 is the 3rd stop in the list
    }

    private AStarRouter routerWithBusPrediction(Graph graph, BusDelayPredictionService predService) {
        InMemoryGraphStore graphStore = new InMemoryGraphStore();
        graphStore.replaceGraph(graph);
        HelperService helperService = new HelperService();
        VaoSerializer vaoSerializer = mock(VaoSerializer.class);
        when(vaoSerializer.getSchedulesMap()).thenReturn(Map.of());
        when(vaoSerializer.getRoutesMap()).thenReturn(Map.of());
        return new AStarRouter(
                graphStore, new SpatialSearchService(helperService), helperService,
                vaoSerializer, mock(GoogleRoutesService.class), new HeuristicService(),
                new WeightedCostFunction(routingConfig()), routingConfig(),
                null, null, predService
        );
    }

    private AStarRouter routerFor(Graph graph) {
        return routerFor(graph, mock(GoogleRoutesService.class));
    }

    private AStarRouter routerFor(Graph graph, GoogleRoutesService googleRoutesService) {
        return routerFor(graph, googleRoutesService, routingConfig());
    }

    private AStarRouter routerFor(Graph graph, GoogleRoutesService googleRoutesService, RoutingConfig routingConfig) {
        InMemoryGraphStore graphStore = new InMemoryGraphStore();
        graphStore.replaceGraph(graph);
        HelperService helperService = new HelperService();
        VaoSerializer vaoSerializer = mock(VaoSerializer.class);
        when(vaoSerializer.getSchedulesMap()).thenReturn(Map.of());
        when(vaoSerializer.getSchedulesMap(any(LocalDate.class))).thenReturn(Map.of());
        when(vaoSerializer.getScheduleDates()).thenReturn(List.of());
        when(vaoSerializer.isRouteActiveOnDate(anyInt(), any(LocalDate.class))).thenReturn(true);
        return new AStarRouter(
                graphStore,
                new SpatialSearchService(helperService),
                helperService,
                vaoSerializer,
                googleRoutesService,
                new HeuristicService(),
                new WeightedCostFunction(routingConfig),
                routingConfig,
                null,
                null,
                null
        );
    }

    private Edge bus(int fromNodeId, int toNodeId, int costSeconds, RouteInfo routeInfo) {
        return new Edge(fromNodeId, toNodeId, EdgeType.BUS, costSeconds, costSeconds, routeInfo);
    }

    private RoutingConfig routingConfig() {
        return new RoutingConfig(300, 1000, 5.0, 3.0, 1.5);
    }

    private RoutingConfig routingConfig(int transferPenaltySeconds) {
        return new RoutingConfig(transferPenaltySeconds, 1000, 5.0, 3.0, 1.5);
    }
}
