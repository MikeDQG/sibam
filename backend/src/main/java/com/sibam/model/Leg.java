package com.sibam.model;

import java.time.LocalTime;

public record Leg(
        Mode mode,
        String fromNodeName,
        String toNodeName,
        LocalTime departureTime,
        LocalTime arrivalTime,
        Integer lineId // null, če je pešačenje
) {}