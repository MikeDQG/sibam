package com.sibam.service;

import com.sibam.dto.weather.*;
import com.sibam.integration.weather.WeatherClient;
import com.sibam.persistence.WeatherSnapshot;
import com.sibam.repository.WeatherSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WeatherDataServiceTest {

    private WeatherClient weatherClient;
    private WeatherSnapshotRepository weatherSnapshotRepository;
    private WeatherDataService service;

    @BeforeEach
    void setUp() {
        weatherClient = mock(WeatherClient.class);
        weatherSnapshotRepository = mock(WeatherSnapshotRepository.class);
        service = new WeatherDataService(weatherClient, weatherSnapshotRepository);
        when(weatherSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private WeatherResponseDto response(Double rainAmount, List<WeatherConditionDto> conditions) {
        return new WeatherResponseDto(
                new WeatherMainDto(15.0, 13.0, 65),
                new WeatherWindDto(3.5),
                rainAmount != null ? new WeatherRainDto(rainAmount) : null,
                conditions
        );
    }

    @Test
    void ingestSavesSnapshotWithCorrectFieldsWhenNoRain() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        when(weatherSnapshotRepository.save(any())).thenAnswer(inv -> { latch.countDown(); return inv.getArgument(0); });
        when(weatherClient.getCurrentWeather()).thenReturn(
                Mono.just(response(null, List.of(new WeatherConditionDto("Clear", "clear sky"))))
        );

        service.ingestWeatherData(OffsetDateTime.now());
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

        ArgumentCaptor<WeatherSnapshot> captor = ArgumentCaptor.forClass(WeatherSnapshot.class);
        verify(weatherSnapshotRepository).save(captor.capture());
        WeatherSnapshot saved = captor.getValue();
        assertThat(saved.getTemperature()).isEqualTo(15.0);
        assertThat(saved.getWindSpeed()).isEqualTo(3.5);
        assertThat(saved.getRain()).isNull();
        assertThat(saved.getCondition()).isEqualTo("Clear");
    }

    @Test
    void ingestSavesRainAmountWhenRainDtoPresent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        when(weatherSnapshotRepository.save(any())).thenAnswer(inv -> { latch.countDown(); return inv.getArgument(0); });
        when(weatherClient.getCurrentWeather()).thenReturn(
                Mono.just(response(2.5, List.of(new WeatherConditionDto("Rain", "light rain"))))
        );

        service.ingestWeatherData(OffsetDateTime.now());
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

        ArgumentCaptor<WeatherSnapshot> captor = ArgumentCaptor.forClass(WeatherSnapshot.class);
        verify(weatherSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getRain()).isEqualTo(2.5);
        assertThat(captor.getValue().getCondition()).isEqualTo("Rain");
    }

    @Test
    void ingestSetsNullConditionWhenWeatherListIsEmpty() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        when(weatherSnapshotRepository.save(any())).thenAnswer(inv -> { latch.countDown(); return inv.getArgument(0); });
        when(weatherClient.getCurrentWeather()).thenReturn(
                Mono.just(response(null, List.of()))
        );

        service.ingestWeatherData(OffsetDateTime.now());
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

        ArgumentCaptor<WeatherSnapshot> captor = ArgumentCaptor.forClass(WeatherSnapshot.class);
        verify(weatherSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getCondition()).isNull();
    }

    @Test
    void ingestSetsNullConditionWhenWeatherListIsNull() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        when(weatherSnapshotRepository.save(any())).thenAnswer(inv -> { latch.countDown(); return inv.getArgument(0); });
        when(weatherClient.getCurrentWeather()).thenReturn(
                Mono.just(response(null, null))
        );

        service.ingestWeatherData(OffsetDateTime.now());
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

        ArgumentCaptor<WeatherSnapshot> captor = ArgumentCaptor.forClass(WeatherSnapshot.class);
        verify(weatherSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getCondition()).isNull();
    }

    @Test
    void ingestSetsRecordedAtFromParameter() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        when(weatherSnapshotRepository.save(any())).thenAnswer(inv -> { latch.countDown(); return inv.getArgument(0); });
        OffsetDateTime fetchedAt = OffsetDateTime.parse("2026-06-04T10:00:00+02:00");
        when(weatherClient.getCurrentWeather()).thenReturn(
                Mono.just(response(null, List.of(new WeatherConditionDto("Clouds", "few clouds"))))
        );

        service.ingestWeatherData(fetchedAt);
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

        ArgumentCaptor<WeatherSnapshot> captor = ArgumentCaptor.forClass(WeatherSnapshot.class);
        verify(weatherSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getRecordedAt()).isEqualTo(fetchedAt);
    }
}
