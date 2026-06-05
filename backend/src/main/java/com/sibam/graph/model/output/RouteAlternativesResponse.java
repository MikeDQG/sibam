package com.sibam.graph.model.output;

import java.util.List;

public record RouteAlternativesResponse(
        String status,
        List<RouteAlternative> routes
) {
}
