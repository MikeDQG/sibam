package com.sibam.persistence;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

/**
 * Statični podatki o postaji MBajk kolesarskega sistema.
 * Vsaka postaja se shrani enkrat — ob prvem zaznanju v sistemu.
 */
@Entity
@Table(name = "bike_stations")
@Data
public class BikeStation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "number", unique = true, nullable = false)
    private int number;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "capacity", nullable = false)
    private int capacity;
}
