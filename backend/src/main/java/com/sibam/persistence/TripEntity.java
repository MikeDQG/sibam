package com.sibam.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Trenutno stanje aktivnega avtobusnega potovanja — položaj vozila, pot in trenutna postaja.
 * Shranjuje se vsakih 30 sekund za namen napovedovanja zamud.
 */
@Entity
@Table(name = "trip_snapshots")
public class TripEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "vehicle_id")
    private String vehicleId;

    @Column(name = "vehicle_label")
    private String vehicleLabel;

    @Column(name = "lat")
    private double lat;

    @Column(name = "lon")
    private double lon;

    @Column(name = "bearing")
    private Float bearing;

    @Column(name = "current_stop_sequence")
    private int currentStopSequence;

    @Column(name = "current_stop_id")
    private String currentStopId;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public void setVehicleLabel(String vehicleLabel) {
        this.vehicleLabel = vehicleLabel;
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

    public int getCurrentStopSequence() {
        return currentStopSequence;
    }

    public void setCurrentStopSequence(int currentStopSequence) {
        this.currentStopSequence = currentStopSequence;
    }

    public String getCurrentStopId() {
        return currentStopId;
    }

    public void setCurrentStopId(String currentStopId) {
        this.currentStopId = currentStopId;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
