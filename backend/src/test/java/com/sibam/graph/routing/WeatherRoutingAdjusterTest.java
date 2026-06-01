package com.sibam.graph.routing;

import com.sibam.graph.model.EdgeType;
import com.sibam.persistence.WeatherSnapshot;
import com.sibam.repository.WeatherSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeatherRoutingAdjusterTest {

    @Test
    void rainPenalizesWalkAndBikeReducesTransferPenaltyAndLimitsWalkDistance() {
        WeatherRoutingAdjuster adjuster = adjusterFor(snapshot(12.0, 1.2, "Rain"));

        assertThat(adjuster.adjustedEdgeCost(EdgeType.WALK, 100)).isEqualTo(200);
        assertThat(adjuster.adjustedEdgeCost(EdgeType.BIKE, 100)).isEqualTo(500);
        assertThat(adjuster.adjustedTransferPenaltySeconds(300)).isEqualTo(150);
        assertThat(adjuster.isEdgeAllowed(EdgeType.WALK, 500)).isTrue();
        assertThat(adjuster.isEdgeAllowed(EdgeType.WALK, 501)).isFalse();
    }

    @Test
    void freezingWeatherPenalizesBikeMoreThanWalk() {
        WeatherRoutingAdjuster adjuster = adjusterFor(snapshot(-3.0, null, "Clear"));

        assertThat(adjuster.adjustedEdgeCost(EdgeType.WALK, 100)).isEqualTo(150);
        assertThat(adjuster.adjustedEdgeCost(EdgeType.BIKE, 100)).isEqualTo(200);
    }

    @Test
    void hotWeatherPenalizesWalkMoreThanBike() {
        WeatherRoutingAdjuster adjuster = adjusterFor(snapshot(32.0, null, "Clear"));

        assertThat(adjuster.adjustedEdgeCost(EdgeType.WALK, 100)).isEqualTo(130);
        assertThat(adjuster.adjustedEdgeCost(EdgeType.BIKE, 100)).isEqualTo(110);
    }

    @Test
    void contextAwareMethodsDoNotQueryRepositoryRepeatedly() {
        WeatherSnapshotRepository repository = mock(WeatherSnapshotRepository.class);
        WeatherSnapshot snapshot = snapshot(12.0, 1.0, "Rain");
        when(repository.findFirstByOrderByRecordedAtDesc()).thenReturn(Optional.of(snapshot));
        WeatherRoutingAdjuster adjuster = new WeatherRoutingAdjuster(
                new RoutingConfig(300, 1000, 5.0, 3.0, 1.5),
                repository
        );

        WeatherRoutingContext weather = adjuster.currentWeather();
        adjuster.adjustedEdgeCost(EdgeType.WALK, 100, weather);
        adjuster.adjustedEdgeCost(EdgeType.BIKE, 100, weather);
        adjuster.adjustedTransferPenaltySeconds(300, weather);
        adjuster.isEdgeAllowed(EdgeType.WALK, 400, weather);

        verify(repository, times(1)).findFirstByOrderByRecordedAtDesc();
    }

    private WeatherRoutingAdjuster adjusterFor(WeatherSnapshot snapshot) {
        WeatherSnapshotRepository repository = mock(WeatherSnapshotRepository.class);
        when(repository.findFirstByOrderByRecordedAtDesc()).thenReturn(Optional.of(snapshot));
        return new WeatherRoutingAdjuster(new RoutingConfig(300, 1000, 5.0, 3.0, 1.5), repository);
    }

    private WeatherSnapshot snapshot(double temperature, Double rain, String condition) {
        WeatherSnapshot snapshot = new WeatherSnapshot();
        snapshot.setTemperature(temperature);
        snapshot.setFeelsLike(temperature);
        snapshot.setHumidity(70);
        snapshot.setWindSpeed(2.0);
        snapshot.setRain(rain);
        snapshot.setCondition(condition);
        snapshot.setRecordedAt(OffsetDateTime.now());
        return snapshot;
    }
}
