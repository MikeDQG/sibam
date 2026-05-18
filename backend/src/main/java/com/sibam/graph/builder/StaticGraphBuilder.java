package com.sibam.graph.builder;

import com.sibam.engine.VaoSerializer;
import com.sibam.engine.vao.BusStopVao;
import com.sibam.engine.vao.RouteVao;
import com.sibam.engine.vao.ShapeNodeVao;
import com.sibam.graph.model.*;
import com.sibam.graph.spatial.HelperService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StaticGraphBuilder implements GraphBuilder {

    private static final double BUS_SPEED_MPS = 6.0;

    private final VaoSerializer vaoSerializer;
    private final HelperService helperService;
    private final BusEdgeBuilder busEdgeBuilder = new BusEdgeBuilder();
    private final WalkingEdgeBuilder walkingEdgeBuilder;

    public StaticGraphBuilder(VaoSerializer vaoSerializer, HelperService helperService) {
        this.vaoSerializer = vaoSerializer;
        this.helperService = helperService;
        this.walkingEdgeBuilder = new WalkingEdgeBuilder(helperService);
    }

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
        // Simple graph: create walking edges between stops within max walking distance
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

        return new Graph(nodes, adjacencyList);
    }

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

                if (!current.isBusStop() || current.stopId() == null || !nodes.containsKey(current.stopId())) {
                    previousShapeNode = current;
                    continue;
                }

                if (previousStop != null
                        && !previousStop.stopId().equals(current.stopId())
                        && nodes.containsKey(previousStop.stopId())) {
                    int distanceMeters = (int) Math.max(1, Math.round(distanceSincePreviousStop));
                    int travelTimeSeconds = (int) Math.max(1, Math.round(distanceMeters / BUS_SPEED_MPS));
                    RouteInfo routeInfo = new RouteInfo(
                            route.LineId(),
                            route.routeId(),
                            route.headsignName(),
                            route.code()
                    );

                    adjacencyList.get(previousStop.stopId()).add(busEdgeBuilder.build(
                            previousStop.stopId(),
                            current.stopId(),
                            distanceMeters,
                            travelTimeSeconds,
                            routeInfo,
                            shapeSincePreviousStop
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
}
