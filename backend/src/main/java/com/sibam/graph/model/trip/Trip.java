package com.sibam.graph.model.trip;

import lombok.Data;

import java.util.List;

/**
 * Razred namenjen routing algoritmu
 */
@Data
public class Trip {
    String tripId;
    String routeId;
    String vehicleLabel;
    String vehicleId;
    double lat;
    double lon;
    Float bearing;
    int current_stop_sequence;
    String stop_id;
    Long timestamp;
    List<StopUpdate>  stopUpdates;

    public Trip() {}
    public Trip(
            String tripId,
            String routeId,
            String vehicleLabel,
            String vehicleId,
            double lat,
            double lon,
            Float bearing,
            int current_stop_sequence,
            String stop_id,
            Long timestamp,
            List<StopUpdate>  stopUpdates
            ) {}
}
