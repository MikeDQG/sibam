package com.sibam.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Trenutno stanje razpoložljivosti koles in stojal na postaji MBajk.
 * Shranjuje se vsakih 5 minut za namen napovedovanja razpoložljivosti.
 */
@Entity
@Table(name = "bike_station_snapshots")
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BikeStation getStation() {
        return station;
    }

    public void setStation(BikeStation station) {
        this.station = station;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getBikes() {
        return bikes;
    }

    public void setBikes(int bikes) {
        this.bikes = bikes;
    }

    public int getStands() {
        return stands;
    }

    public void setStands(int stands) {
        this.stands = stands;
    }

    public int getMechanicalBikes() {
        return mechanicalBikes;
    }

    public void setMechanicalBikes(int mechanicalBikes) {
        this.mechanicalBikes = mechanicalBikes;
    }

    public int getElectricalBikes() {
        return electricalBikes;
    }

    public void setElectricalBikes(int electricalBikes) {
        this.electricalBikes = electricalBikes;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
