package com.sibam.graph.spatial;


/**
 * Helper Service:
 * - function for computing Harvesine distance
 * - constant for walking speed (let's say 10 km/h)
 * - constant for max distance for walking edges (let's say 500 m)
 */
import org.springframework.stereotype.Service;

@Service
public class HelperService {

    // Earth radius in meters
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    // Walking speed 10 km/h
    private static final double WALKING_SPEED_KMH = 5.0;

    // Max distance for walking edges in meters
    private static final int MAX_WALKING_DISTANCE_M = 500;

    /**
     * Compute Haversine distance between two coordinates in meters.
     */
    public double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    public double getWalkingSpeedKmh() {
        return WALKING_SPEED_KMH;
    }

    public double getWalkingSpeedMps() {
        return (WALKING_SPEED_KMH * 1000.0) / 3600.0;
    }

    public int getMaxWalkingDistanceMeters() {
        return MAX_WALKING_DISTANCE_M;
    }
}
