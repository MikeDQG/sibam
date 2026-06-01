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

    // Walking speed 10 km/h
    private static final double WALKING_SPEED_KMH = 5.0;

    // Max distance for walking edges in meters
    private static final int MAX_WALKING_DISTANCE_M = 500;

    /**
     * Compute Haversine distance between two coordinates in meters.
     */
    public double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        return DistanceCalculator.haversineMeters(lat1, lon1, lat2, lon2);
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
