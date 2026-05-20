package com.sibam.engine.vao;

import java.util.List;

public record LineScheduleVao(
        int lineId,
        List<RouteScheduleVao> routeAndSchedules
) {}
