package com.sibam.graph.model;

public enum RouteAlternativeLabel {
    FASTEST("Najhitrej\u0161a pot"),
    WALK_FRIENDLY("Pe\u0161 pot"),
    BIKE_FRIENDLY("Kolesarska pot"),
    TRANSIT_FRIENDLY("Avtobusna pot"),
    MULTIMODAL("Multimodalna pot"),
    ALTERNATIVE("Alternativa");

    private final String displayName;

    RouteAlternativeLabel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
