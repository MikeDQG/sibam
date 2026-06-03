package com.sibam.engine.vao;

import java.util.List;

public record DailyScheduleCacheVao(
        String date,
        String scheduleKey,
        List<Integer> activeRouteIds
) {}
