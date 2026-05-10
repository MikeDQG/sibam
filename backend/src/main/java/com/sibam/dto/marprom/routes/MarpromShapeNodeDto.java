package com.sibam.dto.marprom.routes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromShapeNodeDto(
        @JsonProperty("SequenceNo") int sequenceNo,
        @JsonProperty("Lat") double lat,
        @JsonProperty("Lon") double lon,

        // Uporabimo Integer, da Jackson lahko nastavi null, če polja ni v JSON
        @JsonProperty("StopPointId") Integer stopPointId,
        @JsonProperty("StopId") Integer stopId
) {
    /**
     * Pomožna metoda, ki nam pove, ali je ta točka dejansko postajališče.
     */
    public boolean isBusStop() {
        return stopPointId != null;
    }
}