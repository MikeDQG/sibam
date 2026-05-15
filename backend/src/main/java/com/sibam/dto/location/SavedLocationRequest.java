package com.sibam.dto.location;

import java.util.UUID;

public record SavedLocationRequest(
        UUID userId,
        String name,
        String address,
        Double latitude,
        Double longitude
) {}
