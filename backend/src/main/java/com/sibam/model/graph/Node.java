package com.sibam.model.graph;

import java.util.List;

public class Node {
    String id;
    NodeType type;
    double lat, lon;
    double heuristic;
    List<Edge> ougoingEdges;
}
