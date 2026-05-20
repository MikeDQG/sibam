package com.sibam.engine.vao;

import java.util.List;

public record RouteScheduleVao(
        String direction,
        List<String> departures
) {}
