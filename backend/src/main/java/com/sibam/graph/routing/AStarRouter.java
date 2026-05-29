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
import com.sibam.graph.spatial.DistanceCalculator;
import com.sibam.graph.spatial.HelperService;
import com.sibam.graph.spatial.SpatialSearchService;
import com.sibam.graph.storage.GraphStore;
import com.sibam.engine.VaoSerializer;
import com.sibam.engine.vao.LineScheduleVao;
import com.sibam.engine.vao.RouteScheduleVao;
import com.sibam.engine.vao.StopScheduleVao;
import com.sibam.service.GoogleRoutesService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.text.Normalizer;
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
    private static final double BIKE_SPEED_MPS = 4.5;
    private static final int TRANSFER_PENALTY_SECONDS = 300;
    private static final int WALK_LONG_DISTANCE_THRESHOLD_METERS = 1_000;
    private static final int BIKE_LONG_DISTANCE_THRESHOLD_METERS = 1_000;
    private static final double WALK_LONG_DISTANCE_COST_MULTIPLIER = 1.5;
    private static final double BIKE_LONG_DISTANCE_COST_MULTIPLIER = 1.5;
    private static final DateTimeFormatter DEPARTURE_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    private final GraphStore graphStore;
    private final SpatialSearchService spatialSearchService;
    private final HelperService helperService;
    private final VaoSerializer vaoSerializer;
    private final WalkingEdgeBuilder walkingEdgeBuilder;
    private final GoogleRoutesService googleRoutesService;
    private final HeuristicService heuristicService;

    public AStarRouter(
            GraphStore graphStore,
            SpatialSearchService spatialSearchService,
            HelperService helperService,
            VaoSerializer vaoSerializer,
            GoogleRoutesService googleRoutesService,
            HeuristicService heuristicService
    ) {
        this.graphStore = graphStore;
        this.spatialSearchService = spatialSearchService;
        this.helperService = helperService;
        this.vaoSerializer = vaoSerializer;
        this.walkingEdgeBuilder = new WalkingEdgeBuilder(helperService);
        this.googleRoutesService = googleRoutesService;
        this.heuristicService = heuristicService;
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

        long startMillis = toEpochMillis(startTime);
        PathResult pathResult = findPath(
                routingGraph,
                ORIGIN_NODE_ID,
                DESTINATION_NODE_ID,
                allowBike,
                allowBus,
                startMillis
        );
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
        return findPath(requireGraph(), startNodeId, goalNodeId, true, true, toEpochMillis(LocalTime.now()));
    }

    private PathResult findPath(
            Graph graph,
            int startNodeId,
            int goalNodeId,
            boolean allowBike,
            boolean allowBus,
            long startMillis
    ) {
        PriorityQueue<NodeRecord> openSet =
                new PriorityQueue<>(Comparator.comparingDouble(NodeRecord::fScore));

        Map<Integer, Integer> gScore = new HashMap<>();
        Map<Integer, Integer> cameFrom = new HashMap<>();
        Map<Integer, Edge> cameFromEdge = new HashMap<>();
        Map<Integer, PathStepTiming> cameFromTiming = new HashMap<>();

        gScore.put(startNodeId, 0);
        openSet.add(new NodeRecord(startNodeId, heuristicSeconds(graph, startNodeId, goalNodeId, null)));

        while (!openSet.isEmpty()) {
            NodeRecord current = openSet.poll();
            if (!gScore.containsKey(current.nodeId())) {
                continue;
            }

            if (current.nodeId() == goalNodeId) {
                return reconstruct(
                        cameFrom,
                        cameFromEdge,
                        cameFromTiming,
                        current.nodeId(),
                        gScore.get(goalNodeId)
                );
            }

            for (Edge edge : graph.getNeighbors(current.nodeId())) {
                if (!isAllowed(edge, allowBike, allowBus)) {
                    continue;
                }

                int currentCostSeconds = gScore.get(current.nodeId());
                EdgeRelaxation relaxation = relaxEdge(
                        graph,
                        edge,
                        cameFromEdge.get(current.nodeId()),
                        startMillis + secondsToMillis(currentCostSeconds)
                );
                int tentative = currentCostSeconds + relaxation.costSeconds();

                if (tentative < gScore.getOrDefault(edge.getToNodeId(), Integer.MAX_VALUE)) {
                    cameFrom.put(edge.getToNodeId(), current.nodeId());
                    cameFromEdge.put(edge.getToNodeId(), edge);
                    cameFromTiming.put(edge.getToNodeId(), new PathStepTiming(
                            relaxation.departureMillis(),
                            relaxation.arrivalMillis()
                    ));
                    gScore.put(edge.getToNodeId(), tentative);
                    openSet.add(new NodeRecord(
                            edge.getToNodeId(),
                            tentative + heuristicSeconds(graph, edge.getToNodeId(), goalNodeId, edge.getEdgeType())
                    ));
                }
            }
        }

        return null;
    }

    private EdgeRelaxation relaxEdge(Graph graph, Edge edge, Edge previousEdge, long currentMillis) {
        if (edge.getEdgeType() != EdgeType.BUS) {
            int costSeconds = edgeCostSeconds(graph, edge);
            long arrivalMillis = currentMillis + secondsToMillis(costSeconds);
            return new EdgeRelaxation(costSeconds, currentMillis, arrivalMillis);
        }

        if (previousEdge != null
                && previousEdge.getEdgeType() == EdgeType.BUS
                && Objects.equals(previousEdge.getRouteInfo(), edge.getRouteInfo())) {
            long arrivalMillis = currentMillis + secondsToMillis(edge.getCostSeconds());
            return new EdgeRelaxation(edge.getCostSeconds(), currentMillis, arrivalMillis);
        }

        long departureMillis = nextBusDepartureMillis(edge, currentMillis);
        long arrivalMillis = departureMillis + secondsToMillis(edge.getCostSeconds());
        int costSeconds = (int) Math.max(1, Math.round((arrivalMillis - currentMillis) / 1000.0));
        costSeconds += transferPenaltySeconds(previousEdge, edge);
        return new EdgeRelaxation(costSeconds, departureMillis, arrivalMillis);
    }

    private int transferPenaltySeconds(Edge previousEdge, Edge currentEdge) {
        if (previousEdge == null
                || previousEdge.getEdgeType() != EdgeType.BUS
                || currentEdge.getEdgeType() != EdgeType.BUS) {
            return 0;
        }

        RouteInfo previousRoute = previousEdge.getRouteInfo();
        RouteInfo currentRoute = currentEdge.getRouteInfo();
        if (previousRoute == null || currentRoute == null) {
            return TRANSFER_PENALTY_SECONDS;
        }

        boolean sameLine = previousRoute.lineId() == currentRoute.lineId();
        boolean sameRoute = previousRoute.routeId() == currentRoute.routeId();
        return sameLine || sameRoute ? 0 : TRANSFER_PENALTY_SECONDS;
    }

    private long nextBusDepartureMillis(Edge edge, long currentMillis) {
        RouteInfo routeInfo = edge.getRouteInfo();
        if (routeInfo == null) {
            return currentMillis;
        }

        ensureSchedulesLoaded();

        int scheduleStopPointId = edge.getScheduleStopPointId() == null
                ? edge.getFromNodeId()
                : edge.getScheduleStopPointId();
        StopScheduleVao stopSchedule = vaoSerializer.getSchedulesMap().get(scheduleStopPointId);
        if (stopSchedule == null || stopSchedule.scheduleForLine() == null) {
            return currentMillis;
        }

        return stopSchedule.scheduleForLine().stream()
                .filter(lineSchedule -> lineSchedule.lineId() == routeInfo.lineId())
                .flatMap(lineSchedule -> matchingRouteSchedules(lineSchedule, routeInfo).stream())
                .flatMap(routeSchedule -> routeSchedule.departures().stream())
                .map(departure -> departureMillis(departure, currentMillis))
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElse(currentMillis);
    }

    private List<RouteScheduleVao> matchingRouteSchedules(LineScheduleVao lineSchedule, RouteInfo routeInfo) {
        if (lineSchedule.routeAndSchedules() == null || lineSchedule.routeAndSchedules().isEmpty()) {
            return List.of();
        }

        String headsign = normalize(routeInfo.headsignName());
        List<RouteScheduleVao> matching = lineSchedule.routeAndSchedules().stream()
                .filter(routeSchedule -> {
                    String direction = normalize(routeSchedule.direction());
                    return !headsign.isBlank()
                            && (direction.contains(headsign)
                            || headsign.contains(direction)
                            || directionContainsDestination(direction, headsign));
                })
                .toList();

        if (!matching.isEmpty()) {
            return matching;
        }

        return lineSchedule.routeAndSchedules();
    }

    private boolean directionContainsDestination(String direction, String headsign) {
        String[] tokens = headsign.split(" ");
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i];
            if (token.length() >= 4) {
                return direction.contains(token);
            }
        }

        return false;
    }

    private Long departureMillis(String departure, long currentMillis) {
        try {
            LocalTime departureTime = LocalTime.parse(departure, DEPARTURE_TIME_FORMATTER);
            LocalDate currentDate = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(currentMillis),
                    ZoneId.systemDefault()
            ).toLocalDate();
            long candidate = currentDate.atTime(departureTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            if (candidate < currentMillis) {
                candidate = currentDate.plusDays(1)
                        .atTime(departureTime)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            }

            return candidate;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private void ensureSchedulesLoaded() {
        if (vaoSerializer.getSchedulesMap() == null || vaoSerializer.getSchedulesMap().isEmpty()) {
            vaoSerializer.fetchData();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutDiacritics
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
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
            Map<Integer, PathStepTiming> cameFromTiming,
            int current,
            int totalCost
    ) {
        List<Integer> path = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        List<PathStepTiming> timings = new ArrayList<>();

        path.add(current);

        while (cameFrom.containsKey(current)) {
            edges.add(cameFromEdge.get(current));
            timings.add(cameFromTiming.get(current));
            current = cameFrom.get(current);
            path.add(current);
        }

        Collections.reverse(path);
        Collections.reverse(edges);
        Collections.reverse(timings);

        return new PathResult(path, edges, timings, totalCost);
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
        int legDistanceMeters = previousEdge.getDistanceMeters();
        int legStartIndex = 0;

        for (int i = 1; i < pathResult.getEdges().size(); i++) {
            Edge currentEdge = pathResult.getEdges().get(i);

            if (canMerge(previousEdge, currentEdge)) {
                legDistanceMeters += currentEdge.getDistanceMeters();
                previousEdge = currentEdge;
                continue;
            }

            PathStepTiming firstTiming = pathResult.getTimings().get(legStartIndex);
            PathStepTiming lastTiming = pathResult.getTimings().get(i - 1);
            legs.add(toLeg(
                    graph,
                    pathResult.getEdges().subList(legStartIndex, i),
                    legStartEdge,
                    previousEdge,
                    durationSeconds(firstTiming, lastTiming),
                    legDistanceMeters,
                    firstTiming.departureMillis(),
                    lastTiming.arrivalMillis()
            ));

            legStartEdge = currentEdge;
            previousEdge = currentEdge;
            legDistanceMeters = currentEdge.getDistanceMeters();
            legStartIndex = i;
        }

        PathStepTiming firstTiming = pathResult.getTimings().get(legStartIndex);
        PathStepTiming lastTiming = pathResult.getTimings().getLast();
        legs.add(toLeg(
                graph,
                pathResult.getEdges().subList(legStartIndex, pathResult.getEdges().size()),
                legStartEdge,
                previousEdge,
                durationSeconds(firstTiming, lastTiming),
                legDistanceMeters,
                firstTiming.departureMillis(),
                lastTiming.arrivalMillis()
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

    private int durationSeconds(PathStepTiming firstTiming, PathStepTiming lastTiming) {
        return (int) Math.max(0, Math.round((lastTiming.arrivalMillis() - firstTiming.departureMillis()) / 1000.0));
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
        GeoPoint from = nodePoint(graph, firstEdge.getFromNodeId());
        GeoPoint to = nodePoint(graph, lastEdge.getToNodeId());

        // For WALK and BIKE legs fetch polyline from Google Routes API
        if (firstEdge.getEdgeType() == EdgeType.WALK || firstEdge.getEdgeType() == EdgeType.BIKE) {
            try {
                List<GeoPoint> apiPolyline = googleRoutesService.fetchPolyline(from, to, firstEdge.getEdgeType());
                if (apiPolyline != null && !apiPolyline.isEmpty()) {
                    return apiPolyline;
                }
            } catch (Exception ignored) {
                // Fallback to local polyline logic below
            }
        }

        if (firstEdge.getEdgeType() == EdgeType.TRANSFER) {
            return List.of(from, to);
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

    private double heuristicSeconds(Graph graph, int currentNodeId, int goalNodeId, EdgeType edgeType) {
        Node current = graph.getNodes().get(currentNodeId);
        Node goal = graph.getNodes().get(goalNodeId);
        if (current == null || goal == null) {
            return 0;
        }

        return heuristicService.estimate(current, goal, edgeType);
    }

    private int edgeCostSeconds(Graph graph, Edge edge) {
        if (edge.getEdgeType() != EdgeType.WALK && edge.getEdgeType() != EdgeType.BIKE) {
            return edge.getCostSeconds();
        }

        Node from = graph.getNodes().get(edge.getFromNodeId());
        Node to = graph.getNodes().get(edge.getToNodeId());
        if (from == null || to == null) {
            return edge.getCostSeconds();
        }

        GeoPoint fromPoint = new GeoPoint(from.getLat(), from.getLon());
        GeoPoint toPoint = new GeoPoint(to.getLat(), to.getLon());
        double distanceMeters = DistanceCalculator.correctedDistanceMeters(
                fromPoint,
                toPoint,
                edge.getEdgeType()
        );
        double speedMps = edge.getEdgeType() == EdgeType.WALK
                ? helperService.getWalkingSpeedMps()
                : BIKE_SPEED_MPS;
        int baseCostSeconds = (int) Math.max(1, Math.round(distanceMeters / speedMps));
        return applyModePenalty(edge.getEdgeType(), distanceMeters, baseCostSeconds);
    }

    private int applyModePenalty(EdgeType edgeType, double distanceMeters, int baseCostSeconds) {
        if (edgeType == EdgeType.WALK && distanceMeters > WALK_LONG_DISTANCE_THRESHOLD_METERS) {
            return (int) Math.max(1, Math.round(baseCostSeconds * WALK_LONG_DISTANCE_COST_MULTIPLIER));
        }

        if (edgeType == EdgeType.BIKE && distanceMeters > BIKE_LONG_DISTANCE_THRESHOLD_METERS) {
            return (int) Math.max(1, Math.round(baseCostSeconds * BIKE_LONG_DISTANCE_COST_MULTIPLIER));
        }

        return baseCostSeconds;
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

    private record EdgeRelaxation(int costSeconds, long departureMillis, long arrivalMillis) {
    }
}
