package com.sibam.graph.spatial;

import com.sibam.graph.model.BusNode;
import com.sibam.graph.model.Graph;
import com.sibam.graph.model.Node;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpatialSearchServiceTest {

    private final SpatialSearchService service = new SpatialSearchService(new HelperService());

    private Graph graphOf(Node... nodes) {
        var nodeMap = new java.util.LinkedHashMap<Integer, Node>();
        var edgeMap = new java.util.LinkedHashMap<Integer, List<com.sibam.graph.model.Edge>>();
        for (Node n : nodes) {
            nodeMap.put(n.getId(), n);
            edgeMap.put(n.getId(), new ArrayList<>());
        }
        return new Graph(nodeMap, edgeMap);
    }

    // --- findNearest(Graph, lat, lon, limit) ---

    @Test
    void returnsEmptyListForNullGraph() {
        assertThat(service.findNearest(null, 46.55, 15.64, 1)).isEmpty();
    }

    @Test
    void returnsEmptyListForEmptyGraph() {
        Graph empty = new Graph(Map.of(), Map.of());
        assertThat(service.findNearest(empty, 46.55, 15.64, 1)).isEmpty();
    }

    @Test
    void returnsEmptyListForZeroLimit() {
        Graph graph = graphOf(new BusNode(1, 46.55, 15.64, "A"));
        assertThat(service.findNearest(graph, 46.55, 15.64, 0)).isEmpty();
    }

    @Test
    void returnsSingleNodeWhenOnlyOneExists() {
        Node node = new BusNode(1, 46.55, 15.64, "Only");
        Graph graph = graphOf(node);

        List<Node> result = service.findNearest(graph, 46.55, 15.64, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    @Test
    void returnsClosestNodeAmongMultiple() {
        Node close = new BusNode(1, 46.550, 15.640, "Close");
        Node far   = new BusNode(2, 46.560, 15.650, "Far");
        Graph graph = graphOf(close, far);

        List<Node> result = service.findNearest(graph, 46.551, 15.641, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    @Test
    void respectsLimitWhenMultipleNodesExist() {
        Graph graph = graphOf(
                new BusNode(1, 46.550, 15.640, "A"),
                new BusNode(2, 46.551, 15.641, "B"),
                new BusNode(3, 46.560, 15.650, "C")
        );

        List<Node> result = service.findNearest(graph, 46.550, 15.640, 2);

        assertThat(result).hasSize(2);
    }

    @Test
    void resultIsOrderedNearestFirst() {
        Node nearest  = new BusNode(1, 46.550, 15.640, "Nearest");
        Node middle   = new BusNode(2, 46.552, 15.642, "Middle");
        Node furthest = new BusNode(3, 46.560, 15.650, "Furthest");
        Graph graph = graphOf(nearest, middle, furthest);

        List<Node> result = service.findNearest(graph, 46.550, 15.640, 3);

        assertThat(result.get(0).getId()).isEqualTo(1);
        assertThat(result.get(2).getId()).isEqualTo(3);
    }

    // --- findNearest(Graph, lat, lon) single-node form ---

    @Test
    void singleNodeFormReturnsNullForNullGraph() {
        assertThat(service.findNearest(null, 46.55, 15.64)).isNull();
    }

    @Test
    void singleNodeFormReturnsClosestNode() {
        Node close = new BusNode(1, 46.550, 15.640, "Close");
        Node far   = new BusNode(2, 46.560, 15.650, "Far");
        Graph graph = graphOf(close, far);

        Node result = service.findNearest(graph, 46.551, 15.641);

        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    void rebuildIndexWhenGraphChanges() {
        Node a = new BusNode(1, 46.550, 15.640, "A");
        Graph graph1 = graphOf(a);
        service.findNearest(graph1, 46.550, 15.640, 1);

        Node b = new BusNode(2, 46.560, 15.650, "B");
        Graph graph2 = graphOf(b);
        List<Node> result = service.findNearest(graph2, 46.560, 15.650, 1);

        assertThat(result.get(0).getId()).isEqualTo(2);
    }
}
