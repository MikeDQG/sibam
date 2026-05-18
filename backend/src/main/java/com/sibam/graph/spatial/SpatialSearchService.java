package com.sibam.graph.spatial;

import com.sibam.graph.model.Graph;
import com.sibam.graph.model.Node;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpatialSearchService {

    private Graph indexedGraph;
    private RTreeIndex rTreeIndex = new RTreeIndex();

    public SpatialSearchService(HelperService helperService) {
    }

    public List<Node> findNearest(
            Graph graph,
            double lat,
            double lon,
            int limit
    ) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty() || limit <= 0) {
            return List.of();
        }

        ensureIndex(graph);
        return rTreeIndex.nearest(lat, lon, limit);
    }

    public Node findNearest(Graph graph, double lat, double lon) {
        List<Node> nearest = findNearest(graph, lat, lon, 1);
        if (nearest.isEmpty()) {
            return null;
        }

        return nearest.getFirst();
    }

    private synchronized void ensureIndex(Graph graph) {
        if (indexedGraph == graph) {
            return;
        }

        RTreeIndex nextIndex = new RTreeIndex();
        nextIndex.build(List.copyOf(graph.getNodes().values()));
        rTreeIndex = nextIndex;
        indexedGraph = graph;
    }
}
