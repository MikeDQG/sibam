package com.sibam.graph.model;

public enum RouteAlternativeLabel {
    FASTEST("Najhitrejša"),
    BIKE_FRIENDLY("Bolj zelena"),
    TRANSIT_FRIENDLY("Bolj udobna"),
    ALTERNATIVE("Alternativa");

    private final String displayName;

    RouteAlternativeLabel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
