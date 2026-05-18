package com.sibam.persistence;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Trenutno stanje razpoložljivosti koles in stojal na postaji MBajk.
 * Shranjuje se vsakih 5 minut za namen napovedovanja razpoložljivosti.
 */
@Entity
@Table(name = "bike_station_snapshots")
@Data
public class BikeStationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private BikeStation station;

    @Column(name = "status")
    private String status;

    @Column(name = "bikes", nullable = false)
    private int bikes;

    @Column(name = "stands", nullable = false)
    private int stands;

    @Column(name = "mechanical_bikes", nullable = false)
    private int mechanicalBikes;

    @Column(name = "electrical_bikes", nullable = false)
    private int electricalBikes;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
}
