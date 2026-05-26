package com.sibam.dto.prediction;

/**
 * Zahtevek za napoved zamude avtobusa. {@code stopId} se uporabi za določitev smeri linije iz preslikave stop_direction_mapping.json.
 */

public record BusDelayPredictionRequest(
        int routeId,
        int stopSequence,
        int hour,
        int dayOfWeek,
        int isWeekend,
        float temperature,
        float rain,
        float windSpeed,
        int stopId
) {}