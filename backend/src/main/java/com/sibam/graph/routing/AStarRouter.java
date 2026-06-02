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
import com.sibam.graph.model.output.NavigationStep;
import com.sibam.graph.builder.WalkingEdgeBuilder;
import com.sibam.graph.spatial.HelperService;
import com.sibam.graph.spatial.SpatialSearchService;
import com.sibam.graph.storage.GraphStore;
import com.sibam.engine.VaoSerializer;
import com.sibam.engine.vao.BikeLegPredictionVao;
import com.sibam.engine.vao.LineScheduleVao;
import com.sibam.engine.vao.RouteScheduleVao;
import com.sibam.engine.vao.StopScheduleVao;
import com.sibam.dto.prediction.BikePredictionRequest;
import com.sibam.service.BikePredictionService;
import com.sibam.service.GoogleRoutesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    private static final Logger log = LoggerFactory.getLogger(AStarRouter.class);

    private static final int ORIGIN_NODE_ID = -1;
    private static final int DESTINATION_NODE_ID = -2;
    private static final int NEAREST_STOP_LIMIT = 5;
    private static final DateTimeFormatter DEPARTURE_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    private final GraphStore graphStore;
    private final SpatialSearchService spatialSearchService;
    private final HelperService helperService;
    private final VaoSerializer vaoSerializer;
    private final WalkingEdgeBuilder walkingEdgeBuilder;
    private final GoogleRoutesService googleRoutesService;
    private final HeuristicService heuristicService;
    private final CostFunction costFunction;
    private final RoutingConfig routingConfig;
    private final WeatherRoutingAdjuster weatherRoutingAdjuster;
    private final BikePredictionService bikePredictionService;

    public AStarRouter(
            GraphStore graphStore,
            SpatialSearchService spatialSearchService,
            HelperService helperService,
            VaoSerializer vaoSerializer,
            GoogleRoutesService googleRoutesService,
            HeuristicService heuristicService,
            CostFunction costFunction,
            RoutingConfig routingConfig,
            WeatherRoutingAdjuster weatherRoutingAdjuster,
            BikePredictionService bikePredictionService
    ) {
        this.graphStore = graphStore;
        this.spatialSearchService = spatialSearchService;
        this.helperService = helperService;
        this.vaoSerializer = vaoSerializer;
        this.walkingEdgeBuilder = new WalkingEdgeBuilder(helperService);
        this.googleRoutesService = googleRoutesService;
        this.heuristicService = heuristicService;
        this.costFunction = costFunction;
        this.routingConfig = routingConfig;
        this.weatherRoutingAdjuster = weatherRoutingAdjuster;
        this.bikePredictionService = bikePredictionService;
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

        validateAccessDistance(originAddress == null || originAddress.isBlank() ? "Izhodišče" : originAddress, originLat, originLon, originStops.getFirst());
        validateAccessDistance(destinationAddress == null || destinationAddress.isBlank() ? "Cilj" : destinationAddress, destinationLat, destinationLon, destinationStops.getFirst());

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

        WeatherRoutingContext weatherContext = currentWeatherContext();
        long startMillis = toEpochMillis(startTime);
        PathResult pathResult = findPath(
                routingGraph,
                ORIGIN_NODE_ID,
                DESTINATION_NODE_ID,
                allowBike,
                allowBus,
                startMillis,
                weatherContext
        );
        if (pathResult == null) {
            return null;
        }

        log.info(
                "Path created with heuristics: \ntransferPenaltySeconds={} \nbikeDistanceThresholdMeters={} \nbikeLongDistanceMultiplier={} \nbikeShortDistanceMultiplier={} \nwalkLongDistanceMultiplier={}",
                routingConfig.getTransferPenaltySeconds(),
                routingConfig.getBikeDistanceThresholdMeters(),
                routingConfig.getBikeLongDistanceMultiplier(),
                routingConfig.getBikeShortDistanceMultiplier(),
                routingConfig.getWalkLongDistanceMultiplier()
                );

        return toJourney(
                routingGraph,
                pathResult,
                new GeoPoint(originLat, originLon),
                originAddress,
                new GeoPoint(destinationLat, destinationLon),
                destinationAddress,
                startTime,
                weatherContext
        );
    }

    public PathResult findPath(int startNodeId, int goalNodeId) {
        return findPath(
                requireGraph(),
                startNodeId,
                goalNodeId,
                true,
                true,
                toEpochMillis(LocalTime.now()),
                currentWeatherContext()
        );
    }

    private PathResult findPath(
            Graph graph,
            int startNodeId,
            int goalNodeId,
            boolean allowBike,
            boolean allowBus,
            long startMillis,
            WeatherRoutingContext weatherContext
    ) {
        PriorityQueue<NodeRecord> openSet =
                new PriorityQueue<>(Comparator.comparingDouble(NodeRecord::fScore));

        SearchState startState = new SearchState(startNodeId, null);
        Map<SearchState, Integer> gScore = new HashMap<>();
        Map<SearchState, SearchState> cameFrom = new HashMap<>();
        Map<SearchState, Edge> cameFromEdge = new HashMap<>();
        Map<SearchState, PathStepTiming> cameFromTiming = new HashMap<>();

        gScore.put(startState, 0);
        openSet.add(new NodeRecord(startState, heuristicSeconds(graph, startNodeId, goalNodeId)));

        while (!openSet.isEmpty()) {
            NodeRecord current = openSet.poll();
            if (!gScore.containsKey(current.state())) {
                continue;
            }

            if (current.state().nodeId() == goalNodeId) {
                return reconstruct(
                        cameFrom,
                        cameFromEdge,
                        cameFromTiming,
                        current.state(),
                        gScore.get(current.state())
                );
            }

            for (Edge edge : graph.getNeighbors(current.state().nodeId())) {
                if (!isAllowed(edge, allowBike, allowBus, weatherContext)) {
                    continue;
                }

                int currentCostSeconds = gScore.get(current.state());
                EdgeRelaxation relaxation = relaxEdge(
                        edge,
                        cameFromEdge.get(current.state()),
                        current.state().lastBusRoute(),
                        startMillis + secondsToMillis(currentCostSeconds),
                        weatherContext
                );
                int tentative = currentCostSeconds + relaxation.costSeconds();
                SearchState nextState = nextState(current.state(), edge);

                if (tentative < gScore.getOrDefault(nextState, Integer.MAX_VALUE)) {
                    cameFrom.put(nextState, current.state());
                    cameFromEdge.put(nextState, edge);
                    cameFromTiming.put(nextState, new PathStepTiming(
                            relaxation.departureMillis(),
                            relaxation.arrivalMillis()
                    ));
                    gScore.put(nextState, tentative);
                    openSet.add(new NodeRecord(
                            nextState,
                            tentative + heuristicSeconds(graph, edge.getToNodeId(), goalNodeId)
                    ));
                }
            }
        }

        return null;
    }

    private EdgeRelaxation relaxEdge(
            Edge edge,
            Edge previousEdge,
            RouteInfo lastBusRoute,
            long currentMillis,
            WeatherRoutingContext weatherContext
    ) {
        if (edge.getEdgeType() != EdgeType.BUS) {
            int costSeconds = edgeCostSeconds(edge, weatherContext);
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
        costSeconds += transferPenaltySeconds(lastBusRoute, edge, weatherContext);
        return new EdgeRelaxation(costSeconds, departureMillis, arrivalMillis);
    }

    private SearchState nextState(SearchState currentState, Edge edge) {
        RouteInfo lastBusRoute = currentState.lastBusRoute();
        if (edge.getEdgeType() == EdgeType.BUS) {
            lastBusRoute = edge.getRouteInfo();
        }

        return new SearchState(edge.getToNodeId(), lastBusRoute);
    }

    private int transferPenaltySeconds(
            RouteInfo previousRoute,
            Edge currentEdge,
            WeatherRoutingContext weatherContext
    ) {
        if (previousRoute == null || currentEdge.getEdgeType() != EdgeType.BUS) {
            return 0;
        }

        RouteInfo currentRoute = currentEdge.getRouteInfo();
        if (currentRoute == null) {
            return adjustedTransferPenaltySeconds(weatherContext);
        }

        boolean sameLine = previousRoute.lineId() == currentRoute.lineId();
        boolean sameRoute = previousRoute.routeId() == currentRoute.routeId();
        if (sameLine || sameRoute) {
            return 0;
        }

        return adjustedTransferPenaltySeconds(weatherContext);
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

    private boolean isAllowed(
            Edge edge,
            boolean allowBike,
            boolean allowBus,
            WeatherRoutingContext weatherContext
    ) {
        if (edge.getEdgeType() == EdgeType.BIKE) {
            return allowBike;
        }

        if (edge.getEdgeType() == EdgeType.BUS) {
            return allowBus;
        }

        if (weatherRoutingAdjuster != null
                && !weatherRoutingAdjuster.isEdgeAllowed(
                edge.getEdgeType(),
                edge.getDistanceMeters(),
                weatherContext
        )) {
            return false;
        }

        return true;
    }

    private int adjustedTransferPenaltySeconds(WeatherRoutingContext weatherContext) {
        if (weatherRoutingAdjuster == null) {
            return routingConfig.getTransferPenaltySeconds();
        }

        return weatherRoutingAdjuster.adjustedTransferPenaltySeconds(
                routingConfig.getTransferPenaltySeconds(),
                weatherContext
        );
    }

    private WeatherRoutingContext currentWeatherContext() {
        if (weatherRoutingAdjuster == null) {
            return WeatherRoutingContext.neutral();
        }

        return weatherRoutingAdjuster.currentWeather();
    }

    private PathResult reconstruct(
            Map<SearchState, SearchState> cameFrom,
            Map<SearchState, Edge> cameFromEdge,
            Map<SearchState, PathStepTiming> cameFromTiming,
            SearchState current,
            int totalCost
    ) {
        List<Integer> path = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        List<PathStepTiming> timings = new ArrayList<>();

        path.add(current.nodeId());

        while (cameFrom.containsKey(current)) {
            edges.add(cameFromEdge.get(current));
            timings.add(cameFromTiming.get(current));
            current = cameFrom.get(current);
            path.add(current.nodeId());
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
            LocalTime startTime,
            WeatherRoutingContext weatherContext
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
                    lastTiming.arrivalMillis(),
                    weatherContext
            ));
            addSameStopTransferLegIfNeeded(
                    graph,
                    legs,
                    previousEdge,
                    currentEdge,
                    lastTiming.arrivalMillis(),
                    pathResult.getTimings().get(i).departureMillis()
            );

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
                lastTiming.arrivalMillis(),
                weatherContext
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

        // For transit edges, only merge if on the same route
        if (previousEdge.getEdgeType() == EdgeType.BUS) {
            return Objects.equals(previousEdge.getRouteInfo(), currentEdge.getRouteInfo());
        }

        // For walk, bike, and transfer edges, always merge consecutive same-type edges
        return true;
    }

    private void addSameStopTransferLegIfNeeded(
            Graph graph,
            List<Leg> legs,
            Edge previousEdge,
            Edge currentEdge,
            long previousArrivalMillis,
            long currentDepartureMillis
    ) {
        if (!isSameStopBusTransfer(previousEdge, currentEdge)) {
            return;
        }

        GeoPoint stopPoint = nodePoint(graph, previousEdge.getToNodeId());
        int transferSeconds = (int) Math.max(0, Math.round(
                (currentDepartureMillis - previousArrivalMillis) / 1000.0
        ));

        legs.add(new Leg(
                "TRANSFER",
                stopPoint,
                stopPoint,
                String.valueOf(secondsToMillis(transferSeconds)),
                "0",
                List.of(stopPoint),
                null,
                null,
                null,
                null,
                String.valueOf(previousArrivalMillis),
                String.valueOf(currentDepartureMillis)
        ));
    }

    private boolean isSameStopBusTransfer(Edge previousEdge, Edge currentEdge) {
        if (previousEdge.getEdgeType() != EdgeType.BUS || currentEdge.getEdgeType() != EdgeType.BUS) {
            return false;
        }

        if (previousEdge.getToNodeId() != currentEdge.getFromNodeId()) {
            return false;
        }

        RouteInfo previousRoute = previousEdge.getRouteInfo();
        RouteInfo currentRoute = currentEdge.getRouteInfo();
        if (previousRoute == null || currentRoute == null) {
            return true;
        }

        boolean sameLine = previousRoute.lineId() == currentRoute.lineId();
        boolean sameRoute = previousRoute.routeId() == currentRoute.routeId();
        return !sameLine && !sameRoute;
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
            long arrivalMillis,
            WeatherRoutingContext weatherContext
    ) {
        RouteInfo routeInfo = firstEdge.getRouteInfo();
        LegNavigation navigation = legNavigation(graph, edges, firstEdge, lastEdge);

        return new Leg(
                toMode(firstEdge.getEdgeType()),
                nodePoint(graph, firstEdge.getFromNodeId()),
                nodePoint(graph, lastEdge.getToNodeId()),
                String.valueOf(secondsToMillis(durationSeconds)),
                String.valueOf(distanceMeters),
                navigation.polyline(),
                routeInfo == null ? null : routeInfo.lineCode(),
                routeInfo == null ? null : routeInfo.headsignName(),
                freeStands(graph, firstEdge, lastEdge),
                freeBikes(graph, firstEdge),
                String.valueOf(departureMillis),
                String.valueOf(arrivalMillis),
                navigation.navigationAvailable(),
                navigation.steps(),
                computeBikePrediction(graph, firstEdge, lastEdge, departureMillis, arrivalMillis, weatherContext)
        );
    }

    private BikeLegPredictionVao computeBikePrediction(
            Graph graph,
            Edge firstEdge,
            Edge lastEdge,
            long departureMillis,
            long arrivalMillis,
            WeatherRoutingContext weatherContext
    ) {
        if (firstEdge.getEdgeType() != EdgeType.BIKE) {
            return null;
        }

        Node pickupNode = graph.getNodes().get(firstEdge.getFromNodeId());
        Node returnNode = graph.getNodes().get(lastEdge.getToNodeId());
        if (!(pickupNode instanceof BikeNode pickup) || !(returnNode instanceof BikeNode returnStation)) {
            return null;
        }

        try {
            LocalDateTime pickupTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(departureMillis), ZoneId.systemDefault());
            LocalDateTime returnTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(arrivalMillis), ZoneId.systemDefault());
            float temperature = weatherContext.temperatureCelsius() != null ? weatherContext.temperatureCelsius().floatValue() : 15f;
            float rain = weatherContext.rainMm() != null ? weatherContext.rainMm() : 0f;
            float windSpeed = weatherContext.windSpeedMs() != null ? weatherContext.windSpeedMs() : 0f;

            int pickupDow = pickupTime.getDayOfWeek().getValue();
            var pickupResponse = bikePredictionService.predict(new BikePredictionRequest(
                    pickup.getStationNumber(),
                    pickupTime.getHour(), pickupDow, pickupDow >= 6 ? 1 : 0,
                    temperature, rain, windSpeed
            ));

            int returnDow = returnTime.getDayOfWeek().getValue();
            var returnResponse = bikePredictionService.predict(new BikePredictionRequest(
                    returnStation.getStationNumber(),
                    returnTime.getHour(), returnDow, returnDow >= 6 ? 1 : 0,
                    temperature, rain, windSpeed
            ));

            return new BikeLegPredictionVao(
                    pickupResponse.predictedBikes(),
                    pickupResponse.bikeAvailableProbability(),
                    returnResponse.predictedStands(),
                    returnResponse.standAvailableProbability()
            );
        } catch (Exception e) {
            log.warn("Bike prediction unavailable, skipping enrichment: {}", e.getMessage());
            return null;
        }
    }

    private String toMode(EdgeType edgeType) {
        return switch (edgeType) {
            case BUS -> "BUS";
            case BIKE -> "BIKE";
            case TRANSFER -> "TRANSFER";
            case WALK -> "WALK";
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

    private LegNavigation legNavigation(Graph graph, List<Edge> edges, Edge firstEdge, Edge lastEdge) {
        GeoPoint from = nodePoint(graph, firstEdge.getFromNodeId());
        GeoPoint to = nodePoint(graph, lastEdge.getToNodeId());

        if (navigationSupported(firstEdge)) {
            try {
                GoogleRoutesService.RouteDetails routeDetails =
                        googleRoutesService.fetchRouteDetails(from, to, firstEdge.getEdgeType());
                if (routeDetails != null) {
                    List<GeoPoint> polyline = routeDetails.polyline().isEmpty()
                            ? localPolyline(graph, edges, firstEdge, lastEdge)
                            : routeDetails.polyline();
                    List<NavigationStep> steps = routeDetails.steps();
                    return new LegNavigation(polyline, !steps.isEmpty(), steps.isEmpty() ? null : steps);
                }
            } catch (Exception e) {
                log.warn("Google navigation details unavailable for {} leg: {}", firstEdge.getEdgeType(), e.toString());
            }

            return new LegNavigation(localPolyline(graph, edges, firstEdge, lastEdge), false, null);
        }

        return new LegNavigation(localPolyline(graph, edges, firstEdge, lastEdge), null, null);
    }

    private boolean navigationSupported(Edge firstEdge) {
        return firstEdge.getEdgeType() == EdgeType.WALK
                || firstEdge.getEdgeType() == EdgeType.BIKE;
    }

    private List<GeoPoint> localPolyline(Graph graph, List<Edge> edges, Edge firstEdge, Edge lastEdge) {
        GeoPoint from = nodePoint(graph, firstEdge.getFromNodeId());
        GeoPoint to = nodePoint(graph, lastEdge.getToNodeId());

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

    private void validateAccessDistance(String endpoint, double lat, double lon, Node nearestStop) {
        int maxDistanceMeters = routingConfig.getMaxAccessDistanceMeters();
        double distanceMeters = helperService.haversineMeters(
                lat,
                lon,
                nearestStop.getLat(),
                nearestStop.getLon()
        );

        if (distanceMeters > maxDistanceMeters) {
            throw new RouteAccessDistanceException(endpoint, distanceMeters, maxDistanceMeters);
        }
    }

    private double heuristicSeconds(Graph graph, int currentNodeId, int goalNodeId) {
        Node current = graph.getNodes().get(currentNodeId);
        Node goal = graph.getNodes().get(goalNodeId);
        if (current == null || goal == null) {
            return 0;
        }

        return heuristicService.estimate(current, goal);
    }

    private int edgeCostSeconds(Edge edge, WeatherRoutingContext weatherContext) {
        return costFunction.calculateCost(edge, weatherContext);
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

    private record SearchState(int nodeId, RouteInfo lastBusRoute) {
    }

    private record NodeRecord(SearchState state, double fScore) {
    }

    private record EdgeRelaxation(int costSeconds, long departureMillis, long arrivalMillis) {
    }

    private record LegNavigation(
            List<GeoPoint> polyline,
            Boolean navigationAvailable,
            List<NavigationStep> steps
    ) {
    }
}
