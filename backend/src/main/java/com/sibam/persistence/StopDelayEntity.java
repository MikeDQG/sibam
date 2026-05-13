package com.sibam.persistence;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

/**
 * Zamuda avtobusa na posamezni postaji znotraj enega potovanja.
 * Vsak zapis predstavlja napoved zamude za eno prihodnjo postajo ob trenutku zajema podatkov.
 */
@Entity
@Table(name = "stop_delay_snapshots")
@Data
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
}
