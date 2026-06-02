package com.sibam.engine.vao;

import java.util.List;
import java.util.Map;

public record WeeklyScheduleCacheVao(
        List<String> dates,
        Map<String, String> dateToScheduleKey,
        Map<String, Map<Integer, StopScheduleVao>> uniqueSchedules,
        Map<String, List<Integer>> activeRouteIdsByDate
) {
    public int uniqueScheduleCount() {
        return uniqueSchedules == null ? 0 : uniqueSchedules.size();
    }
}
