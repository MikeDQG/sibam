package com.sibam.dto.prediction;

public record BikePredictionRequest(
        int stationNumber,
        int hour,
        int dayOfWeek,
        int isWeekend,
        float temperature,
        float rain,
        float windSpeed
) {}
