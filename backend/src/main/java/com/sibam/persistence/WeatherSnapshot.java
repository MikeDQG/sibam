package com.sibam.persistence;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "weather_snapshots")
@Data
public class WeatherSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "temperature", nullable = false)
    private double temperature;

    @Column(name = "feels_like", nullable = false)
    private double feelsLike;

    @Column(name = "humidity", nullable = false)
    private int humidity;

    @Column(name = "wind_speed", nullable = false)
    private double windSpeed;

    @Column(name = "rain")
    private Double rain;

    @Column(name = "condition")
    private String condition;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
}
