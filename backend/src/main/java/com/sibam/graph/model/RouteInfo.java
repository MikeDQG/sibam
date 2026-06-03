package com.sibam.graph.model;

import java.io.Serializable;

public record RouteInfo(
        int lineId,
        int routeId,
        String headsignName,
        String lineCode
) implements Serializable {
}
