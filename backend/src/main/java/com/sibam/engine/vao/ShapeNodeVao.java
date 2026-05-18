package com.sibam.engine.vao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShapeNodeVao(
        int sequenceNo,
        double lat,
        double lon,
        Integer stopPointId
) {
    /**
     * Pomožna metoda, ki nam pove, ali je ta točka dejansko postajališče.
     */
    public boolean isBusStop() {
        return stopPointId != null;
    }
}
