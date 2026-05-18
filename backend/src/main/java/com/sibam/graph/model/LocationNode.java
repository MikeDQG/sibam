package com.sibam.graph.model;

public class LocationNode extends Node {

    private final String name;

    public LocationNode(int id, double lat, double lon, String name) {
        super(id, lat, lon);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
