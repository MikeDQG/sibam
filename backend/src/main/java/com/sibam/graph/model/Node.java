package com.sibam.graph.model;

import java.io.Serializable;
public abstract class Node implements Serializable {

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
