package com.sibam.graph.model;

import java.io.Serializable;
import java.util.List;

public abstract class Node implements Serializable {
//    String id;
//    NodeType type;
//    double lat, lon;
//    double heuristic;
//    List<Edge> ougoingEdges;

    protected final int id;
    protected final double lat;
    protected final double lon;

    protected Node(int id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }

    public int getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

}
