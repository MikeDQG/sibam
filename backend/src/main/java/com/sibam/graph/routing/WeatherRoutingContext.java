package com.sibam.graph.routing;

public record WeatherRoutingContext(
        Double temperatureCelsius,
        boolean raining
) {
    public static WeatherRoutingContext neutral() {
        return new WeatherRoutingContext(null, false);
    }
}
