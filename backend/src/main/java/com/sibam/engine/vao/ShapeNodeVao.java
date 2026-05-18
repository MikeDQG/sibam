package com.sibam.engine.vao;

public record ShapeNodeVao(
        int sequenceNo,
        double lat,
        double lon,
        Integer stopPointId,
        Integer stopId
) {
    /**
     * Pomožna metoda, ki nam pove, ali je ta točka dejansko postajališče.
     */
    public boolean isBusStop() {
        return stopPointId != null;
    }
}
