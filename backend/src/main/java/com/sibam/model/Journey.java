package com.sibam.model;

import java.time.LocalTime;
import java.util.List;

public record Journey(
        List<Leg> legs,
        LocalTime startTime,
        LocalTime endTime,
        double totalDurationMinutes
) {}
