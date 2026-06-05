package com.sibam.graph.builder;

import com.sibam.engine.VaoSerializer;
import com.sibam.engine.vao.BikeStationVao;
import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.engine.vao.ShapeNodeVao;
import com.sibam.graph.model.*;
import com.sibam.graph.spatial.DistanceCalculator;
import com.sibam.graph.spatial.HelperService;
import com.sibam.service.MBajkDataService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Graditelj statičnega multimodalnega grafa.
 *
 * Iz Marprom VAO podatkov doda BUS vozlišča in robove, iz MBajk podatkov BIKE
 * vozlišča in robove, med bližnjimi vozlišči pa še WALK povezave.
 */
@Component
public class StaticGraphBuilder implements GraphBuilder {

    private static final double BUS_SPEED_MPS = 6.0;
    private static final double BIKE_SPEED_MPS = 4.5;
    private static final int BIKE_NODE_ID_OFFSET = 1_000_000;

    private final VaoSerializer vaoSerializer;
    private final MBajkDataService mBajkDataService;
    private final HelperService helperService;
    private final BusEdgeBuilder busEdgeBuilder = new BusEdgeBuilder();
    private final BikeEdgeBuilder bikeEdgeBuilder = new BikeEdgeBuilder();
    private final WalkingEdgeBuilder walkingEdgeBuilder;

    public StaticGraphBuilder(
            VaoSerializer vaoSerializer,
            MBajkDataService mBajkDataService,
            HelperService helperService
    ) {
        this.vaoSerializer = vaoSerializer;
        this.mBajkDataService = mBajkDataService;
        this.helperService = helperService;
        this.walkingEdgeBuilder = new WalkingEdgeBuilder(helperService);
    }

    /**
     * Zgradi graf iz trenutno dostopnih Marprom in MBajk podatkov.
     *
     * @return graf z BUS, BIKE in WALK robovi
     */
    @Override
    public Graph build() {
        // Ensure VAO data is present
        if (vaoSerializer.getBusStopsMap() == null || vaoSerializer.getBusStopsMap().isEmpty()) {
            vaoSerializer.fetchData();
        }

        Map<Integer, Node> nodes = new HashMap<>();
        Map<Integer, List<Edge>> adjacencyList = new HashMap<>();

        // Build nodes from BusStopVao
        for (BusStopVao stop : vaoSerializer.getBusStopsMap().values()) {
            if (stop.lat() == null || stop.lon() == null) continue;
            BusNode node = new BusNode(stop.id(), stop.lat(), stop.lon(), stop.name());
            nodes.put(node.getId(), node);
            adjacencyList.put(node.getId(), new ArrayList<>());
        }

        addBusEdges(nodes, adjacencyList);
        addBikeNodes(nodes, adjacencyList);
        addBikeEdges(nodes, adjacencyList);
        addWalkingEdges(nodes, adjacencyList);

        return new Graph(nodes, adjacencyList);
    }

    /**
     * Doda MBajk postaje kot BikeNode vozlišča z aktualno razpoložljivostjo.
     */
    private void addBikeNodes(Map<Integer, Node> nodes, Map<Integer, List<Edge>> adjacencyList) {
        List<BikeStationVao> bikeStations = mBajkDataService.getBikeStationVaos();
        for (BikeStationVao station : bikeStations) {
            int nodeId = toBikeNodeId(station.number());
            int freeBikes = station.availability() == null ? 0 : station.availability().freeBikes();
            int freeStands = station.availability() == null ? 0 : station.availability().freeStands();
            BikeNode node = new BikeNode(
                    nodeId,
                    station.lat(),
                    station.lon(),
                    station.name(),
                    station.number(),
                    freeBikes,
                    freeStands
            );
            nodes.put(node.getId(), node);
            adjacencyList.put(node.getId(), new ArrayList<>());
        }
    }

    /**
     * Doda BIKE robove med postajami z razpoložljivimi kolesi in prostimi stojali.
     */
    private void addBikeEdges(Map<Integer, Node> nodes, Map<Integer, List<Edge>> adjacencyList) {
        List<BikeNode> bikeNodes = nodes.values().stream()
                .filter(BikeNode.class::isInstance)
                .map(BikeNode.class::cast)
                .toList();

        for (BikeNode from : bikeNodes) {
            if (from.getFreeBikes() <= 0) {
                continue;
            }

            for (BikeNode to : bikeNodes) {
                if (from.getId() == to.getId() || to.getFreeStands() <= 0) {
                    continue;
                }

                int distanceMeters = (int) Math.max(1, Math.round(DistanceCalculator.correctedDistanceMeters(
                        new GeoPoint(from.getLat(), from.getLon()),
                        new GeoPoint(to.getLat(), to.getLon()),
                        EdgeType.BIKE
                )));
                int travelTimeSeconds = (int) Math.max(1, Math.round(distanceMeters / BIKE_SPEED_MPS));

                adjacencyList.get(from.getId()).add(bikeEdgeBuilder.build(
                        from.getId(),
                        to.getId(),
                        distanceMeters,
                        travelTimeSeconds
                ));
            }
        }
    }

    /**
     * Doda dvosmerne WALK robove med vozlišči znotraj največje dovoljene razdalje.
     */
    private void addWalkingEdges(Map<Integer, Node> nodes, Map<Integer, List<Edge>> adjacencyList) {
        List<Node> nodeList = new ArrayList<>(nodes.values());
        int maxWalkM = helperService.getMaxWalkingDistanceMeters();

        for (int i = 0; i < nodeList.size(); i++) {
            Node ni = nodeList.get(i);
            for (int j = i + 1; j < nodeList.size(); j++) {
                Node nj = nodeList.get(j);
                double dist = helperService.haversineMeters(ni.getLat(), ni.getLon(), nj.getLat(), nj.getLon());
                if (dist <= maxWalkM) {
                    // Undirected walking connection (both directions)
                    adjacencyList.get(ni.getId()).add(walkingEdgeBuilder.build(ni, nj));
                    adjacencyList.get(nj.getId()).add(walkingEdgeBuilder.build(nj, ni));
                }
            }
        }
    }

    /**
     * Doda BUS robove med zaporednimi postajami na Marprom trasah.
     *
     * Robovi vsebujejo RouteInfo, razdaljo, približen čas vožnje in shape
     * polilinijo za prikaz avtobusne etape.
     */
    private void addBusEdges(Map<Integer, Node> nodes, Map<Integer, List<Edge>> adjacencyList) {
        if (vaoSerializer.getRoutesMap() == null || vaoSerializer.getRoutesMap().isEmpty()) {
            return;
        }

        for (RouteVao route : vaoSerializer.getRoutesMap().values()) {
            List<ShapeNodeVao> shapeNodes = route.shapeNodes();
            if (shapeNodes == null || shapeNodes.isEmpty()) {
                continue;
            }

            List<ShapeNodeVao> orderedShapeNodes = new ArrayList<>(shapeNodes);
            orderedShapeNodes.sort(Comparator.comparingInt(ShapeNodeVao::sequenceNo));

            ShapeNodeVao previousShapeNode = null;
            ShapeNodeVao previousStop = null;
            double distanceSincePreviousStop = 0;
            List<GeoPoint> shapeSincePreviousStop = new ArrayList<>();

            for (ShapeNodeVao current : orderedShapeNodes) {
                if (previousShapeNode != null) {
                    distanceSincePreviousStop += helperService.haversineMeters(
                            previousShapeNode.lat(),
                            previousShapeNode.lon(),
                            current.lat(),
                            current.lon()
                    );
                } else {
                    shapeSincePreviousStop.add(new GeoPoint(current.lat(), current.lon()));
                }

                if (previousShapeNode != null) {
                    shapeSincePreviousStop.add(new GeoPoint(current.lat(), current.lon()));
                }

                if (!current.isBusStop() || current.stopPointId() == null || !nodes.containsKey(current.stopPointId())) {
                    previousShapeNode = current;
                    continue;
                }

                if (previousStop != null
                        && !previousStop.stopPointId().equals(current.stopPointId())
                        && nodes.containsKey(previousStop.stopPointId())) {
                    int distanceMeters = (int) Math.max(1, Math.round(distanceSincePreviousStop));
                    int travelTimeSeconds = (int) Math.max(1, Math.round(distanceMeters / BUS_SPEED_MPS));
                    RouteInfo routeInfo = new RouteInfo(
                            route.LineId(),
                            route.routeId(),
                            route.headsignName(),
                            route.code()
                    );

                    adjacencyList.get(previousStop.stopPointId()).add(busEdgeBuilder.build(
                            previousStop.stopPointId(),
                            current.stopPointId(),
                            distanceMeters,
                            travelTimeSeconds,
                            routeInfo,
                            shapeSincePreviousStop,
                            previousStop.stopPointId()
                    ));
                }

                previousStop = current;
                previousShapeNode = current;
                distanceSincePreviousStop = 0;
                shapeSincePreviousStop = new ArrayList<>();
                shapeSincePreviousStop.add(new GeoPoint(current.lat(), current.lon()));
            }
        }
    }

    private int toBikeNodeId(int stationNumber) {
        return BIKE_NODE_ID_OFFSET + stationNumber;
    }
}
