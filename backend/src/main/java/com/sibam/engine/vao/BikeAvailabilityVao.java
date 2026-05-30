package com.sibam.engine.vao;

import java.time.OffsetDateTime;

public record BikeAvailabilityVao(
        int freeBikes,
        int freeStands,
        int mechanicalBikes,
        int electricalBikes,
        String status,
        OffsetDateTime recordedAt
) {
}
