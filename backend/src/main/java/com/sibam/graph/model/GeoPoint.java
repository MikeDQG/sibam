package com.sibam.graph.model;

import java.io.Serializable;

public record GeoPoint(
        double lat,
        double lon
) implements Serializable {
}
