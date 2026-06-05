package com.sibam.graph.model;

public enum RouteAlternativeLabel {
    FASTEST("Najhitrejša pot"),
    BIKE_FRIENDLY("Zelena pot"),
    TRANSIT_FRIENDLY("Avtobusna pot"),
    ALTERNATIVE("Alternativa");

    private final String displayName;

    RouteAlternativeLabel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
