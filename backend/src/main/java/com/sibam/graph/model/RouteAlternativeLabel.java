package com.sibam.graph.model;

public enum RouteAlternativeLabel {
    FASTEST("Fastest"),
    BIKE_FRIENDLY("Bike friendly"),
    TRANSIT_FRIENDLY("Transit friendly"),
    ALTERNATIVE("Alternative");

    private final String displayName;

    RouteAlternativeLabel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
