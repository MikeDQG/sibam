package com.sibam.engine.vao;

import java.util.List;

public record RouteVao(
        int routeId,
        int LineId,
        String code,
        String destination,
        String headsignName,
        List<ShapeNodeVao> shapeNodes,
        List<BusStopVao> busStops
) {
}
