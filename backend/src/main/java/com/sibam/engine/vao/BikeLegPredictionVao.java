package com.sibam.engine.vao;

public record BikeLegPredictionVao(
        Integer predictedBikesAtPickup,
        Double pickupBikeAvailableProbability,
        Integer predictedStandsAtReturn,
        Double returnStandAvailableProbability
) {}
