package com.sibam.graph.routing;

public class RouteAccessDistanceException extends RuntimeException {

    private final String endpoint;
    private final double distanceMeters;
    private final int maxDistanceMeters;

    public RouteAccessDistanceException(String endpoint, double distanceMeters, int maxDistanceMeters) {
        super(endpoint + " is too far from the nearest bike or bus stop");
        this.endpoint = endpoint;
        this.distanceMeters = distanceMeters;
        this.maxDistanceMeters = maxDistanceMeters;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public int getMaxDistanceMeters() {
        return maxDistanceMeters;
    }
}
