package com.sibam.graph.routing;

public record WeatherRoutingContext(
        Double temperatureCelsius,
        boolean raining,
        Float rainMm,
        Float windSpeedMs
) {
    public static WeatherRoutingContext neutral() {
        return new WeatherRoutingContext(null, false, 0f, 0f);
    }
}
