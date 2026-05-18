package com.sibam.graph.routing;

import com.sibam.graph.model.BikeNode;
import com.sibam.graph.model.Edge;
import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;
import com.sibam.graph.model.Graph;
import com.sibam.graph.model.LocationNode;
import com.sibam.graph.model.Node;
import com.sibam.graph.model.RouteInfo;
import com.sibam.graph.model.output.Journey;
import com.sibam.graph.model.output.Leg;
import com.sibam.graph.builder.WalkingEdgeBuilder;
import com.sibam.graph.spatial.HelperService;
import com.sibam.graph.spatial.SpatialSearchService;
import com.sibam.graph.storage.GraphStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

@Service
public class AStarRouter {

    private static final int ORIGIN_NODE_ID = -1;
    private static final int DESTINATION_NODE_ID = -2;
    private static final int NEAREST_STOP_LIMIT = 5;
    private static final double HEURISTIC_MAX_SPEED_MPS = 30.0;

    private final GraphStore graphStore;
    private final SpatialSearchService spatialSearchService;
    private final HelperService helperService;
    private final WalkingEdgeBuilder walkingEdgeBuilder;

    public AStarRouter(
            GraphStore graphStore,
            SpatialSearchService spatialSearchService,
            HelperService helperService
    ) {
        this.graphStore = graphStore;
        this.spatialSearchService = spatialSearchService;
        this.helperService = helperService;
        this.walkingEdgeBuilder = new WalkingEdgeBuilder(helperService);
    }

    public Journey findJourney(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon
    ) {
        return findJourney(originLat, originLon, destinationLat, destinationLon, null, null, LocalTime.now(), true, true);
    }

    public Journey findJourney(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon,
            LocalTime startTime
    ) {
        return findJourney(originLat, originLon, destinationLat, destinationLon, null, null, startTime, true, true);
    }

    public Journey findJourney(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon,
            String originAddress,
            String destinationAddress,
            LocalTime startTime
    ) {
        return findJourney(
                originLat,
                originLon,
                destinationLat,
                destinationLon,
                originAddress,
                destinationAddress,
                startTime,
                true,
                true
        );
    }

    public Journey findJourney(
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon,
            String originAddress,
            String destinationAddress,
            LocalTime startTime,
            boolean allowBike,
            boolean allowBus
    ) {
        Graph graph = requireGraph();
        List<Node> originStops = spatialSearchService.findNearest(graph, originLat, originLon, NEAREST_STOP_LIMIT);
        List<Node> destinationStops = spatialSearchService.findNearest(graph, destinationLat, destinationLon, NEAREST_STOP_LIMIT);

        if (originStops.isEmpty() || destinationStops.isEmpty()) {
            return null;
        }

        Graph routingGraph = withUserWalkingEdges(
                graph,
                originLat,
                originLon,
                destinationLat,
                destinationLon,
                originAddress,
                destinationAddress,
                originStops,
                destinationStops
        );

        PathResult pathResult = findPath(routingGraph, ORIGIN_NODE_ID, DESTINATION_NODE_ID, allowBike, allowBus);
        if (pathResult == null) {
            return null;
        }

        return toJourney(
                routingGraph,
                pathResult,
                new GeoPoint(originLat, originLon),
                originAddress,
                new GeoPoint(destinationLat, destinationLon),
                destinationAddress,
                startTime
        );
    }

    public PathResult findPath(int startNodeId, int goalNodeId) {
        return findPath(requireGraph(), startNodeId, goalNodeId, true, true);
    }

    private PathResult findPath(Graph graph, int startNodeId, int goalNodeId, boolean allowBike, boolean allowBus) {
        PriorityQueue<NodeRecord> openSet =
                new PriorityQueue<>(Comparator.comparingDouble(NodeRecord::fScore));

        Map<Integer, Integer> gScore = new HashMap<>();
        Map<Integer, Integer> cameFrom = new HashMap<>();
        Map<Integer, Edge> cameFromEdge = new HashMap<>();

        gScore.put(startNodeId, 0);
        openSet.add(new NodeRecord(startNodeId, heuristicSeconds(graph, startNodeId, goalNodeId)));

        while (!openSet.isEmpty()) {
            NodeRecord current = openSet.poll();
            if (!gScore.containsKey(current.nodeId())) {
                continue;
            }

            if (current.nodeId() == goalNodeId) {
                return reconstruct(cameFrom, cameFromEdge, current.nodeId(), gScore.get(goalNodeId));
            }

            for (Edge edge : graph.getNeighbors(current.nodeId())) {
                if (!isAllowed(edge, allowBike, allowBus)) {
                    continue;
                }

                int tentative = gScore.get(current.nodeId()) + edge.getCostSeconds();

                if (tentative < gScore.getOrDefault(edge.getToNodeId(), Integer.MAX_VALUE)) {
                    cameFrom.put(edge.getToNodeId(), current.nodeId());
                    cameFromEdge.put(edge.getToNodeId(), edge);
                    gScore.put(edge.getToNodeId(), tentative);
                    openSet.add(new NodeRecord(
                            edge.getToNodeId(),
                            tentative + heuristicSeconds(graph, edge.getToNodeId(), goalNodeId)
                    ));
                }
            }
        }

        return null;
    }

    private boolean isAllowed(Edge edge, boolean allowBike, boolean allowBus) {
        if (edge.getEdgeType() == EdgeType.BIKE) {
            return allowBike;
        }

        if (edge.getEdgeType() == EdgeType.BUS) {
            return allowBus;
        }

        return true;
    }

    private PathResult reconstruct(
            Map<Integer, Integer> cameFrom,
            Map<Integer, Edge> cameFromEdge,
            int current,
            int totalCost
    ) {
        List<Integer> path = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();

        path.add(current);

        while (cameFrom.containsKey(current)) {
            edges.add(cameFromEdge.get(current));
            current = cameFrom.get(current);
            path.add(current);
        }

        Collections.reverse(path);
        Collections.reverse(edges);

        return new PathResult(path, edges, totalCost);
    }

    private Journey toJourney(
            Graph graph,
            PathResult pathResult,
            GeoPoint origin,
            String originAddress,
            GeoPoint destination,
            String destinationAddress,
            LocalTime startTime
    ) {
        if (pathResult.getEdges().isEmpty()) {
            return new Journey("success", origin, originAddress, destination, destinationAddress, "0", "0", List.of());
        }

        List<Leg> legs = new ArrayList<>();
        Edge legStartEdge = pathResult.getEdges().getFirst();
        Edge previousEdge = legStartEdge;
        int legCostSeconds = previousEdge.getCostSeconds();
        int legDistanceMeters = previousEdge.getDistanceMeters();
        int legStartIndex = 0;
        LocalTime legDeparture = startTime;
        long legDepartureMillis = toEpochMillis(startTime);

        for (int i = 1; i < pathResult.getEdges().size(); i++) {
            Edge currentEdge = pathResult.getEdges().get(i);

            if (canMerge(previousEdge, currentEdge)) {
                legCostSeconds += currentEdge.getCostSeconds();
                legDistanceMeters += currentEdge.getDistanceMeters();
                previousEdge = currentEdge;
                continue;
            }

            LocalTime legArrival = legDeparture.plusSeconds(legCostSeconds);
            long legArrivalMillis = legDepartureMillis + secondsToMillis(legCostSeconds);
            legs.add(toLeg(
                    graph,
                    pathResult.getEdges().subList(legStartIndex, i),
                    legStartEdge,
                    previousEdge,
                    legCostSeconds,
                    legDistanceMeters,
                    legDepartureMillis,
                    legArrivalMillis
            ));

            legStartEdge = currentEdge;
            previousEdge = currentEdge;
            legCostSeconds = currentEdge.getCostSeconds();
            legDistanceMeters = currentEdge.getDistanceMeters();
            legStartIndex = i;
            legDeparture = legArrival;
            legDepartureMillis = legArrivalMillis;
        }

        long legArrivalMillis = legDepartureMillis + secondsToMillis(legCostSeconds);
        legs.add(toLeg(
                graph,
                pathResult.getEdges().subList(legStartIndex, pathResult.getEdges().size()),
                legStartEdge,
                previousEdge,
                legCostSeconds,
                legDistanceMeters,
                legDepartureMillis,
                legArrivalMillis
        ));

        int totalDistanceMeters = pathResult.getEdges().stream()
                .mapToInt(Edge::getDistanceMeters)
                .sum();

        return new Journey(
                "success",
                origin,
                originAddress,
                destination,
                destinationAddress,
                String.valueOf(secondsToMillis(pathResult.getTotalCostSeconds())),
                String.valueOf(totalDistanceMeters),
                legs
        );
    }

    private boolean canMerge(Edge previousEdge, Edge currentEdge) {
        if (previousEdge.getEdgeType() != currentEdge.getEdgeType()) {
            return false;
        }

        if (previousEdge.getEdgeType() != EdgeType.BUS) {
            return true;
        }

        return Objects.equals(previousEdge.getRouteInfo(), currentEdge.getRouteInfo());
    }

    private Leg toLeg(
            Graph graph,
            List<Edge> edges,
            Edge firstEdge,
            Edge lastEdge,
            int durationSeconds,
            int distanceMeters,
            long departureMillis,
            long arrivalMillis
    ) {
        RouteInfo routeInfo = firstEdge.getRouteInfo();

        return new Leg(
                toMode(firstEdge.getEdgeType()),
                nodePoint(graph, firstEdge.getFromNodeId()),
                nodePoint(graph, lastEdge.getToNodeId()),
                String.valueOf(secondsToMillis(durationSeconds)),
                String.valueOf(distanceMeters),
                polyline(graph, edges, firstEdge, lastEdge),
                routeInfo == null ? null : routeInfo.lineCode(),
                routeInfo == null ? null : routeInfo.headsignName(),
                freeStands(graph, firstEdge, lastEdge),
                freeBikes(graph, firstEdge),
                String.valueOf(departureMillis),
                String.valueOf(arrivalMillis)
        );
    }

    private String toMode(EdgeType edgeType) {
        return switch (edgeType) {
            case BUS -> "BUS";
            case BIKE -> "BIKE";
            case WALK, TRANSFER -> "WALK";
        };
    }

    private String freeBikes(Graph graph, Edge firstEdge) {
        if (firstEdge.getEdgeType() != EdgeType.BIKE) {
            return null;
        }

        Node node = graph.getNodes().get(firstEdge.getFromNodeId());
        if (node instanceof BikeNode bikeNode) {
            return String.valueOf(bikeNode.getFreeBikes());
        }

        return null;
    }

    private String freeStands(Graph graph, Edge firstEdge, Edge lastEdge) {
        if (firstEdge.getEdgeType() != EdgeType.BIKE) {
            return null;
        }

        Node node = graph.getNodes().get(lastEdge.getToNodeId());
        if (node instanceof BikeNode bikeNode) {
            return String.valueOf(bikeNode.getFreeStands());
        }

        return null;
    }

    private GeoPoint nodePoint(Graph graph, int nodeId) {
        Node node = graph.getNodes().get(nodeId);
        return new GeoPoint(node.getLat(), node.getLon());
    }

    private List<GeoPoint> polyline(Graph graph, List<Edge> edges, Edge firstEdge, Edge lastEdge) {
        if (firstEdge.getEdgeType() == EdgeType.WALK || firstEdge.getEdgeType() == EdgeType.TRANSFER) {
            return List.of(nodePoint(graph, firstEdge.getFromNodeId()), nodePoint(graph, lastEdge.getToNodeId()));
        }

        List<GeoPoint> points = new ArrayList<>();
        for (Edge edge : edges) {
            List<GeoPoint> edgePolyline = edge.getPolyline().isEmpty()
                    ? List.of(nodePoint(graph, edge.getFromNodeId()), nodePoint(graph, edge.getToNodeId()))
                    : edge.getPolyline();
            appendPolyline(points, edgePolyline);
        }

        return points;
    }

    private void appendPolyline(List<GeoPoint> target, List<GeoPoint> source) {
        for (GeoPoint point : source) {
            if (!target.isEmpty() && target.getLast().equals(point)) {
                continue;
            }

            target.add(point);
        }
    }

    private Graph withUserWalkingEdges(
            Graph graph,
            double originLat,
            double originLon,
            double destinationLat,
            double destinationLon,
            String originAddress,
            String destinationAddress,
            List<Node> originStops,
            List<Node> destinationStops
    ) {
        Map<Integer, Node> nodes = new HashMap<>(graph.getNodes());
        Map<Integer, List<Edge>> adjacencyList = new HashMap<>();
        for (Map.Entry<Integer, List<Edge>> entry : graph.getAdjacencyList().entrySet()) {
            adjacencyList.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        LocationNode origin = new LocationNode(ORIGIN_NODE_ID, originLat, originLon, originAddress);
        LocationNode destination = new LocationNode(DESTINATION_NODE_ID, destinationLat, destinationLon, destinationAddress);
        nodes.put(origin.getId(), origin);
        nodes.put(destination.getId(), destination);
        adjacencyList.put(origin.getId(), new ArrayList<>());
        adjacencyList.put(destination.getId(), new ArrayList<>());

        for (Node stop : originStops) {
            adjacencyList.get(origin.getId()).add(walkingEdgeBuilder.build(origin, stop));
        }

        for (Node stop : destinationStops) {
            adjacencyList.computeIfAbsent(stop.getId(), ignored -> new ArrayList<>())
                    .add(walkingEdgeBuilder.build(stop, destination));
        }

        adjacencyList.get(origin.getId()).add(walkingEdgeBuilder.build(origin, destination));

        return new Graph(nodes, adjacencyList);
    }

    private double heuristicSeconds(Graph graph, int currentNodeId, int goalNodeId) {
        Node current = graph.getNodes().get(currentNodeId);
        Node goal = graph.getNodes().get(goalNodeId);
        if (current == null || goal == null) {
            return 0;
        }

        return helperService.haversineMeters(
                current.getLat(),
                current.getLon(),
                goal.getLat(),
                goal.getLon()
        ) / HEURISTIC_MAX_SPEED_MPS;
    }

    private long toEpochMillis(LocalTime time) {
        LocalDateTime dateTime = LocalDate.now().atTime(time);
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private long secondsToMillis(int seconds) {
        return seconds * 1000L;
    }

    private Graph requireGraph() {
        Graph graph = graphStore.getGraph();
        if (graph == null) {
            throw new IllegalStateException("Graph is not initialized");
        }

        return graph;
    }

    private record NodeRecord(int nodeId, double fScore) {
    }
}
