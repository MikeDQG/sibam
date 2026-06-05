package com.sibam.graph.routing;

import com.sibam.config.FallbackProperties;
import com.sibam.graph.model.EdgeType;
import com.sibam.persistence.WeatherSnapshot;
import com.sibam.repository.WeatherSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Component
public class WeatherRoutingAdjuster {

    private static final Logger log = LoggerFactory.getLogger(WeatherRoutingAdjuster.class);

    private final RoutingConfig routingConfig;
    private final WeatherSnapshotRepository weatherSnapshotRepository;
    private final FallbackProperties fallbackProperties;
    private final Clock clock;

    @Autowired
    public WeatherRoutingAdjuster(
            RoutingConfig routingConfig,
            WeatherSnapshotRepository weatherSnapshotRepository,
            FallbackProperties fallbackProperties,
            Clock clock
    ) {
        this.routingConfig = routingConfig;
        this.weatherSnapshotRepository = weatherSnapshotRepository;
        this.fallbackProperties = fallbackProperties;
        this.clock = clock;
    }

    WeatherRoutingAdjuster(
            RoutingConfig routingConfig,
            WeatherSnapshotRepository weatherSnapshotRepository
    ) {
        this(
                routingConfig,
                weatherSnapshotRepository,
                new FallbackProperties(60),
                Clock.system(ZoneId.of("Europe/Ljubljana"))
        );
    }

    public int adjustedEdgeCost(EdgeType edgeType, int baseCostSeconds) {
        return adjustedEdgeCost(edgeType, baseCostSeconds, currentWeather());
    }

    public int adjustedEdgeCost(
            EdgeType edgeType,
            int baseCostSeconds,
            WeatherRoutingContext weather
    ) {
        double multiplier = edgeMultiplier(edgeType, weather);
        return (int) Math.max(1, Math.round(baseCostSeconds * multiplier));
    }

    public int adjustedTransferPenaltySeconds(int baseTransferPenaltySeconds) {
        return adjustedTransferPenaltySeconds(baseTransferPenaltySeconds, currentWeather());
    }

    public int adjustedTransferPenaltySeconds(
            int baseTransferPenaltySeconds,
            WeatherRoutingContext weather
    ) {
        if (!weather.raining()) {
            return baseTransferPenaltySeconds;
        }

        return (int) Math.max(0, Math.round(
                baseTransferPenaltySeconds * routingConfig.getRainTransferPenaltyMultiplier()
        ));
    }

    public boolean isEdgeAllowed(EdgeType edgeType, int distanceMeters) {
        return isEdgeAllowed(edgeType, distanceMeters, currentWeather());
    }

    public boolean isEdgeAllowed(
            EdgeType edgeType,
            int distanceMeters,
            WeatherRoutingContext weather
    ) {
        if (!weather.raining() || !isWalkingType(edgeType)) {
            return true;
        }

        return distanceMeters <= routingConfig.getRainMaxWalkDistanceMeters();
    }

    public WeatherRoutingContext currentWeather() {
        try {
            return weatherSnapshotRepository.findFirstByOrderByRecordedAtDesc()
                    .filter(this::isFresh)
                    .map(this::toContext)
                    .orElseGet(() -> {
                        log.info("Weather snapshot missing or stale. Falling back to neutral routing weather.");
                        return WeatherRoutingContext.neutral();
                    });
        } catch (DataAccessException ex) {
            log.warn("Could not load latest weather snapshot. Falling back to neutral routing weather: {}", ex.getMessage());
            return WeatherRoutingContext.neutral();
        }
    }

    private boolean isFresh(WeatherSnapshot snapshot) {
        if (snapshot.getRecordedAt() == null) {
            return false;
        }

        OffsetDateTime oldestFresh = OffsetDateTime.now(clock).minus(fallbackProperties.realtimeMaxAge());
        return !snapshot.getRecordedAt().isBefore(oldestFresh);
    }

    private WeatherRoutingContext toContext(WeatherSnapshot snapshot) {
        boolean rainAmount = snapshot.getRain() != null
                && snapshot.getRain() > routingConfig.getRainThresholdMillimeters();
        boolean rainCondition = isRainCondition(snapshot.getCondition());
        float rainMm = snapshot.getRain() != null ? snapshot.getRain().floatValue() : 0f;
        float windSpeedMs = (float) snapshot.getWindSpeed();
        return new WeatherRoutingContext(snapshot.getTemperature(), rainAmount || rainCondition, rainMm, windSpeedMs);
    }

    private double edgeMultiplier(EdgeType edgeType, WeatherRoutingContext weather) {
        double multiplier = 1.0;
        if (weather.raining()) {
            if (isWalkingType(edgeType)) {
                multiplier *= routingConfig.getRainWalkMultiplier();
            } else if (edgeType == EdgeType.BIKE) {
                multiplier *= routingConfig.getRainBikeMultiplier();
            }
        }

        Double temperature = weather.temperatureCelsius();
        if (temperature == null) {
            return multiplier;
        }

        if (temperature < routingConfig.getFreezingTemperatureCelsius()) {
            if (isWalkingType(edgeType)) {
                multiplier *= routingConfig.getFreezingWalkMultiplier();
            } else if (edgeType == EdgeType.BIKE) {
                multiplier *= routingConfig.getFreezingBikeMultiplier();
            }
            return multiplier;
        }

        if (temperature < routingConfig.getCoolTemperatureCelsius()) {
            if (isWalkingType(edgeType)) {
                multiplier *= routingConfig.getCoolWalkMultiplier();
            } else if (edgeType == EdgeType.BIKE) {
                multiplier *= routingConfig.getCoolBikeMultiplier();
            }
            return multiplier;
        }

        if (temperature > routingConfig.getHotTemperatureCelsius()) {
            if (isWalkingType(edgeType)) {
                multiplier *= routingConfig.getHotWalkMultiplier();
            } else if (edgeType == EdgeType.BIKE) {
                multiplier *= routingConfig.getHotBikeMultiplier();
            }
        }

        return multiplier;
    }

    private boolean isWalkingType(EdgeType edgeType) {
        return edgeType == EdgeType.WALK;
    }

    private boolean isRainCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return false;
        }

        String normalized = condition.toLowerCase(Locale.ROOT);
        return normalized.contains("rain")
                || normalized.contains("drizzle")
                || normalized.contains("thunderstorm");
    }
}
