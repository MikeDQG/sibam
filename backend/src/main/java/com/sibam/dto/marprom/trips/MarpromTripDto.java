package com.sibam.dto.marprom.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarpromTripDto(
        @JsonProperty("TripId") int tripId,
        @JsonProperty("LineId") int lineId,
        @JsonProperty("RouteId") int routeId,
        @JsonProperty("ArrivalStopId") int arrivalStopId,
        @JsonProperty("ArrivalTime") String arrivalTime,
        @JsonProperty("DepartureStopId") int departureStopId,
        @JsonProperty("DepartureTime") String departureTime
) {
}
