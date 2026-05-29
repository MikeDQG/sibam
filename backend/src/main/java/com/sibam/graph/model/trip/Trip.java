package com.sibam.graph.model.trip;

import java.util.List;

/**
 * Razred namenjen routing algoritmu
 */
public class Trip {
    private String tripId;
    private String routeId;
    private String vehicleLabel;
    private String vehicleId;
    private double lat;
    private double lon;
    private Float bearing;
    private int current_stop_sequence;
    private String stop_id;
    private Long timestamp;
    private List<StopUpdate> stopUpdates;

    public Trip() {
    }

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
            List<StopUpdate> stopUpdates
    ) {
        this.tripId = tripId;
        this.routeId = routeId;
        this.vehicleLabel = vehicleLabel;
        this.vehicleId = vehicleId;
        this.lat = lat;
        this.lon = lon;
        this.bearing = bearing;
        this.current_stop_sequence = current_stop_sequence;
        this.stop_id = stop_id;
        this.timestamp = timestamp;
        this.stopUpdates = stopUpdates;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public void setVehicleLabel(String vehicleLabel) {
        this.vehicleLabel = vehicleLabel;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Float getBearing() {
        return bearing;
    }

    public void setBearing(Float bearing) {
        this.bearing = bearing;
    }

    public int getCurrent_stop_sequence() {
        return current_stop_sequence;
    }

    public void setCurrent_stop_sequence(int current_stop_sequence) {
        this.current_stop_sequence = current_stop_sequence;
    }

    public String getStop_id() {
        return stop_id;
    }

    public void setStop_id(String stop_id) {
        this.stop_id = stop_id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public List<StopUpdate> getStopUpdates() {
        return stopUpdates;
    }

    public void setStopUpdates(List<StopUpdate> stopUpdates) {
        this.stopUpdates = stopUpdates;
    }
}
