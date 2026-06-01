package com.sibam.persistence;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Zamuda avtobusa na posamezni postaji znotraj enega potovanja.
 * Vsak zapis predstavlja napoved zamude za eno prihodnjo postajo ob trenutku zajema podatkov.
 */
@Entity
@Table(name = "stop_delay_snapshots")
public class StopDelayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "trip_snapshot_id", nullable = false)
    private TripEntity trip;

    @Column(name = "stop_sequence", nullable = false)
    private int stopSequence;

    @Column(name = "delay_seconds", nullable = false)
    private int delaySeconds;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TripEntity getTrip() {
        return trip;
    }

    public void setTrip(TripEntity trip) {
        this.trip = trip;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(int stopSequence) {
        this.stopSequence = stopSequence;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }
}
