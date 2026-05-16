package com.sibam.graph.model.output;

import java.time.LocalTime;
import java.util.List;

public record Journey(
        List<Leg> legs,
        LocalTime startTime,
        LocalTime endTime,
        double totalDurationMinutes
) {}
