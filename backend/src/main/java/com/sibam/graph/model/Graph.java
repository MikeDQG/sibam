package com.sibam.graph.model;

import java.util.List;
import java.util.Map;

public class Graph {
    private final Map<Integer, Node> nodes;
    private final Map<Integer, List<Edge>> adjacencyList;

    public Graph(
            Map<Integer, Node> nodes,
            Map<Integer, List<Edge>> adjacencyList
    ) {
        this.nodes = nodes;
        this.adjacencyList = adjacencyList;
    }

    public Map<Integer, Node> getNodes() {
        return nodes;
    }

    public Map<Integer, List<Edge>> getAdjacencyList() {
        return adjacencyList;
    }

    public List<Edge> getNeighbors(int nodeId) {
        return adjacencyList.getOrDefault(nodeId, List.of());
    }
}
