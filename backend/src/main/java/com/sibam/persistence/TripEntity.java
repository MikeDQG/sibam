package com.sibam.persistence;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Trenutno stanje aktivnega avtobusnega potovanja — položaj vozila, pot in trenutna postaja.
 * Shranjuje se vsakih 30 sekund za namen napovedovanja zamud.
 */
@Entity
@Table(name = "trip_snapshots")
@Data
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
}
