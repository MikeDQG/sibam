package com.sibam.model;

import java.time.LocalTime;
import java.util.List;

public class BusEdge extends Edge {
    int routeId;
    List<LocalTime> departures;
}
