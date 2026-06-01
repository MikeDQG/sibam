package com.sibam.graph.routing;

import com.sibam.engine.VaoSerializer;
import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.Graph;
import com.sibam.graph.model.Node;
import com.sibam.graph.model.RouteInfo;
import com.sibam.graph.model.output.Journey;
import com.sibam.graph.spatial.HelperService;
import com.sibam.graph.spatial.SpatialSearchService;
import com.sibam.graph.storage.InMemoryGraphStore;
import com.sibam.service.GoogleRoutesService;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class AStarRouterTest {

    @Test
    void journeyKeepsRequestedCoordinatesAndOnlyUsesNearestNodesForEdges() {
        double originLat = 46.538077;
        double originLon = 15.603520;
        double destinationLat = 46.561754;
        double destinationLon = 15.629752;

        Node nearbyStop = new BusNode(1, 46.55947, 15.64502, "Slomskov trg");
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
                routingConfig()
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

    private AStarRouter routerFor(Graph graph) {
        InMemoryGraphStore graphStore = new InMemoryGraphStore();
        graphStore.replaceGraph(graph);
        HelperService helperService = new HelperService();
        VaoSerializer vaoSerializer = mock(VaoSerializer.class);
        when(vaoSerializer.getSchedulesMap()).thenReturn(Map.of());
        return new AStarRouter(
                graphStore,
                new SpatialSearchService(helperService),
                helperService,
                vaoSerializer,
                mock(GoogleRoutesService.class),
                new HeuristicService(),
                new WeightedCostFunction(routingConfig()),
                routingConfig()
        );
    }

    private Edge bus(int fromNodeId, int toNodeId, int costSeconds, RouteInfo routeInfo) {
        return new Edge(fromNodeId, toNodeId, EdgeType.BUS, costSeconds, costSeconds, routeInfo);
    }

    private RoutingConfig routingConfig() {
        return new RoutingConfig(300, 1000, 5.0, 3.0, 1.5);
    }
}
