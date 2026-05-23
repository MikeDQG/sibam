package com.sibam.dto.prediction;

public record BikePredictionResponse(
        int predictedBikes,
        int predictedStands,
        double bikeAvailableProbability,
        double standAvailableProbability
) {}
