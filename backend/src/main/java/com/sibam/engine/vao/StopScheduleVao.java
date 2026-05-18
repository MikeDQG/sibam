package com.sibam.engine.vao;

import java.util.List;

public record StopScheduleVao(
        int stopPointId,
        String name,
        String address,
        List<LineScheduleVao> scheduleForLine
) {}
