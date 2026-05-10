package com.sibam.model;

import java.util.List;

public class Node {
    String id;
    NodeType type;
    double lat, lon;
    double heuristic;
    List<Edge> ougoingEdges;
}
