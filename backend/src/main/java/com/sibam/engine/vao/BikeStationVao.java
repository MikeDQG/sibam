package com.sibam.engine.vao;

public record BikeStationVao(
        int number,
        String name,
        String address,
        double lat,
        double lon,
        int capacity,
        BikeAvailabilityVao availability
) {
}
