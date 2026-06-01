package com.sibam.graph.routing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoutingConfig {

//    public static final int TRANSFER_PENALTY_SECONDS = 300;
//    public static final int BIKE_DISTANCE_THRESHOLD_METERS = 1_000;
//    public static final double BIKE_LONG_DISTANCE_MULTIPLIER = 5.0;
//    public static final double WALK_LONG_DISTANCE_MULTIPLIER = 3.0;
//    public static final double BIKE_SHORT_DISTANCE_MULTIPLIER = 1.5;

    /*
     * Routing tuning knobs.
     *
     * These values are intentionally centralized here for manual tuning. Adjust
     * them when you want to change pathfinding behavior, such as discouraging
     * transfers, long bike trips, or long walks. Defaults can be overridden from
     * application properties with the routing.* keys below.
     */

    private final int transferPenaltySeconds;
    private final int bikeDistanceThresholdMeters;
    private final double bikeLongDistanceMultiplier;
    private final double walkLongDistanceMultiplier;
    private final double bikeShortDistanceMultiplier;

    public RoutingConfig(
            @Value("${routing.transfer-penalty-seconds:300}") int transferPenaltySeconds,
            @Value("${routing.bike-distance-threshold-meters:1000}") int bikeDistanceThresholdMeters,
            @Value("${routing.bike-long-distance-multiplier:5.0}") double bikeLongDistanceMultiplier,
            @Value("${routing.walk-long-distance-multiplier:3.0}") double walkLongDistanceMultiplier,
            @Value("${routing.bike-short-distance-multiplier:1.5}") double bikeShortDistanceMultiplier
    ) {
        this.transferPenaltySeconds = transferPenaltySeconds;
        this.bikeDistanceThresholdMeters = bikeDistanceThresholdMeters;
        this.bikeLongDistanceMultiplier = bikeLongDistanceMultiplier;
        this.walkLongDistanceMultiplier = walkLongDistanceMultiplier;
        this.bikeShortDistanceMultiplier = bikeShortDistanceMultiplier;
    }

    public int getTransferPenaltySeconds() {
        return transferPenaltySeconds;
    }

    public int getBikeDistanceThresholdMeters() {
        return bikeDistanceThresholdMeters;
    }

    public double getBikeLongDistanceMultiplier() {
        return bikeLongDistanceMultiplier;
    }

    public double getWalkLongDistanceMultiplier() {
        return walkLongDistanceMultiplier;
    }

    public double getBikeShortDistanceMultiplier() {
        return bikeShortDistanceMultiplier;
    }
}
