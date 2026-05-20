package com.sibam.graph.model;

public class BusNode extends Node {

    private final String stopName;

    public BusNode(int id, double lat, double lon, String stopName) {
        super(id, lat, lon);
        this.stopName = stopName;
    }

    public String getStopName() {
        return stopName;
    }
}
