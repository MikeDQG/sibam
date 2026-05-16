package com.sibam.graph.model;

public record RouteInfo(
        int lineId,
        int routeId,
        String headsignName,
        String lineCode
) {
}
