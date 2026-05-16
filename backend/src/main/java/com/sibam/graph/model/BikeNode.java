package com.sibam.graph.model;

public class BikeNode extends Node {

    private final String stationName;

    public BikeNode(int id, double lat, double lon, String stationName) {
        super(id, lat, lon);
        this.stationName = stationName;
    }

    public String getStationName() {
        return stationName;
    }
}