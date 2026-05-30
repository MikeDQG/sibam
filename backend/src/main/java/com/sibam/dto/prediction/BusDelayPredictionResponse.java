package com.sibam.dto.prediction;

/**
 * Odgovor z napovedano zamudo avtobusa v sekundah.
 */

public record BusDelayPredictionResponse(int predictedDelaySeconds) {}
