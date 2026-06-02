package com.sibam.graph.routing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final double rainWalkMultiplier;
    private final double rainBikeMultiplier;
    private final double rainTransferPenaltyMultiplier;
    private final int rainMaxWalkDistanceMeters;
    private final double rainThresholdMillimeters;
    private final double freezingTemperatureCelsius;
    private final double coolTemperatureCelsius;
    private final double hotTemperatureCelsius;
    private final double freezingWalkMultiplier;
    private final double freezingBikeMultiplier;
    private final double coolWalkMultiplier;
    private final double coolBikeMultiplier;
    private final double hotWalkMultiplier;
    private final double hotBikeMultiplier;
    private final int maxAccessDistanceMeters;

    @Autowired
    public RoutingConfig(
            @Value("${routing.transfer-penalty-seconds:300}") int transferPenaltySeconds,
            @Value("${routing.bike-distance-threshold-meters:1000}") int bikeDistanceThresholdMeters,
            @Value("${routing.bike-long-distance-multiplier:5.0}") double bikeLongDistanceMultiplier,
            @Value("${routing.walk-long-distance-multiplier:3.0}") double walkLongDistanceMultiplier,
            @Value("${routing.bike-short-distance-multiplier:1.5}") double bikeShortDistanceMultiplier,
            @Value("${routing.weather.rain-walk-multiplier:2.0}") double rainWalkMultiplier,
            @Value("${routing.weather.rain-bike-multiplier:5.0}") double rainBikeMultiplier,
            @Value("${routing.weather.rain-transfer-penalty-multiplier:0.5}") double rainTransferPenaltyMultiplier,
            @Value("${routing.weather.rain-max-walk-distance-meters:500}") int rainMaxWalkDistanceMeters,
            @Value("${routing.weather.rain-threshold-millimeters:0.0}") double rainThresholdMillimeters,
            @Value("${routing.weather.freezing-temperature-celsius:0.0}") double freezingTemperatureCelsius,
            @Value("${routing.weather.cool-temperature-celsius:10.0}") double coolTemperatureCelsius,
            @Value("${routing.weather.hot-temperature-celsius:30.0}") double hotTemperatureCelsius,
            @Value("${routing.weather.freezing-walk-multiplier:1.5}") double freezingWalkMultiplier,
            @Value("${routing.weather.freezing-bike-multiplier:2.0}") double freezingBikeMultiplier,
            @Value("${routing.weather.cool-walk-multiplier:1.2}") double coolWalkMultiplier,
            @Value("${routing.weather.cool-bike-multiplier:1.3}") double coolBikeMultiplier,
            @Value("${routing.weather.hot-walk-multiplier:1.3}") double hotWalkMultiplier,
            @Value("${routing.weather.hot-bike-multiplier:1.1}") double hotBikeMultiplier,
            @Value("${routing.max-access-distance-meters:3000}") int maxAccessDistanceMeters
    ) {
        this.transferPenaltySeconds = transferPenaltySeconds;
        this.bikeDistanceThresholdMeters = bikeDistanceThresholdMeters;
        this.bikeLongDistanceMultiplier = bikeLongDistanceMultiplier;
        this.walkLongDistanceMultiplier = walkLongDistanceMultiplier;
        this.bikeShortDistanceMultiplier = bikeShortDistanceMultiplier;
        this.rainWalkMultiplier = rainWalkMultiplier;
        this.rainBikeMultiplier = rainBikeMultiplier;
        this.rainTransferPenaltyMultiplier = rainTransferPenaltyMultiplier;
        this.rainMaxWalkDistanceMeters = rainMaxWalkDistanceMeters;
        this.rainThresholdMillimeters = rainThresholdMillimeters;
        this.freezingTemperatureCelsius = freezingTemperatureCelsius;
        this.coolTemperatureCelsius = coolTemperatureCelsius;
        this.hotTemperatureCelsius = hotTemperatureCelsius;
        this.freezingWalkMultiplier = freezingWalkMultiplier;
        this.freezingBikeMultiplier = freezingBikeMultiplier;
        this.coolWalkMultiplier = coolWalkMultiplier;
        this.coolBikeMultiplier = coolBikeMultiplier;
        this.hotWalkMultiplier = hotWalkMultiplier;
        this.hotBikeMultiplier = hotBikeMultiplier;
        this.maxAccessDistanceMeters = maxAccessDistanceMeters;
    }

    public RoutingConfig(
            int transferPenaltySeconds,
            int bikeDistanceThresholdMeters,
            double bikeLongDistanceMultiplier,
            double walkLongDistanceMultiplier,
            double bikeShortDistanceMultiplier
    ) {
        this(
                transferPenaltySeconds,
                bikeDistanceThresholdMeters,
                bikeLongDistanceMultiplier,
                walkLongDistanceMultiplier,
                bikeShortDistanceMultiplier,
                2.0,
                5.0,
                0.5,
                500,
                0.0,
                0.0,
                10.0,
                30.0,
                1.5,
                2.0,
                1.2,
                1.3,
                1.3,
                1.1,
                3000
        );
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

    public double getRainWalkMultiplier() {
        return rainWalkMultiplier;
    }

    public double getRainBikeMultiplier() {
        return rainBikeMultiplier;
    }

    public double getRainTransferPenaltyMultiplier() {
        return rainTransferPenaltyMultiplier;
    }

    public int getRainMaxWalkDistanceMeters() {
        return rainMaxWalkDistanceMeters;
    }

    public double getRainThresholdMillimeters() {
        return rainThresholdMillimeters;
    }

    public double getFreezingTemperatureCelsius() {
        return freezingTemperatureCelsius;
    }

    public double getCoolTemperatureCelsius() {
        return coolTemperatureCelsius;
    }

    public double getHotTemperatureCelsius() {
        return hotTemperatureCelsius;
    }

    public double getFreezingWalkMultiplier() {
        return freezingWalkMultiplier;
    }

    public double getFreezingBikeMultiplier() {
        return freezingBikeMultiplier;
    }

    public double getCoolWalkMultiplier() {
        return coolWalkMultiplier;
    }

    public double getCoolBikeMultiplier() {
        return coolBikeMultiplier;
    }

    public double getHotWalkMultiplier() {
        return hotWalkMultiplier;
    }

    public double getHotBikeMultiplier() {
        return hotBikeMultiplier;
    }

    public int getMaxAccessDistanceMeters() {
        return maxAccessDistanceMeters;
    }
}
