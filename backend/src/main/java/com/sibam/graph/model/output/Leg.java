package com.sibam.graph.model.output;

import com.sibam.graph.model.Mode;

import java.time.LocalTime;

public record Leg(
        Mode mode,
        String fromNodeName,
        String toNodeName,
        LocalTime departureTime,
        LocalTime arrivalTime,
        Integer lineId // null, če je pešačenje
) {}