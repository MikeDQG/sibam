package com.sibam.graph.spatial;

import com.sibam.graph.model.EdgeType;
import com.sibam.graph.model.GeoPoint;

public final class DistanceCalculator {

    public static final double WALK_DISTANCE_FACTOR = 1.25;
    public static final double BIKE_DISTANCE_FACTOR = 1.15;

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private DistanceCalculator() {
    }

    public static double manhattanDistance(GeoPoint from, GeoPoint to) {
        return Math.abs(from.lat() - to.lat()) + Math.abs(from.lon() - to.lon());
    }

    public static double manhattanMeters(GeoPoint from, GeoPoint to) {
        double northSouth = haversineMeters(from.lat(), from.lon(), to.lat(), from.lon());
        double eastWest = haversineMeters(to.lat(), from.lon(), to.lat(), to.lon());
        return northSouth + eastWest;
    }

    public static double correctedDistanceMeters(GeoPoint from, GeoPoint to, EdgeType edgeType) {
        if (edgeType == EdgeType.WALK) {
            return manhattanMeters(from, to) * WALK_DISTANCE_FACTOR;
        }

        if (edgeType == EdgeType.BIKE) {
            return manhattanMeters(from, to) * BIKE_DISTANCE_FACTOR;
        }

        return haversineMeters(from.lat(), from.lon(), to.lat(), to.lon());
    }

    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
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
}
