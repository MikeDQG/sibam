package com.sibam.dto;

public record ApiErrorResponse(
        String status,
        String code,
        String message,
        String endpoint,
        double distanceMeters,
        int maxDistanceMeters
) {
}
