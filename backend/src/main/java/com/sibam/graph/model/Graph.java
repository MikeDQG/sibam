package com.sibam.graph.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Interni usmerjevalni graf z vozlišči in adjacency listo robov.
 *
 * Graf združuje avtobusne postaje, MBajk postaje in začasna uporabniška
 * vozlišča ter je osnovni vhod za A* routing.
 */
public class Graph implements Serializable {
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

    /**
     * Vrne izhodne robove vozlišča.
     *
     * @param nodeId ID vozlišča
     * @return seznam sosednjih robov ali prazen seznam
     */
    public List<Edge> getNeighbors(int nodeId) {
        return adjacencyList.getOrDefault(nodeId, List.of());
    }
}
