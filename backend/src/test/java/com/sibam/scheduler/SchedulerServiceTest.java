package com.sibam.scheduler;

import com.sibam.service.GTFSRTDataService;
import com.sibam.service.MBajkDataService;
import com.sibam.service.WeatherDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SchedulerServiceTest {

    private static final Clock MIDDAY = Clock.fixed(
            Instant.parse("2026-06-02T10:00:00Z"), // 12:00 Ljubljana (UTC+2)
            ZoneId.of("Europe/Ljubljana")
    );

    private MBajkDataService mbajkDataService;
    private WeatherDataService weatherDataService;
    private GTFSRTDataService gtfsRTDataService;
    private SchedulerService scheduler;

    @BeforeEach
    void setUp() {
        mbajkDataService    = mock(MBajkDataService.class);
        weatherDataService  = mock(WeatherDataService.class);
        gtfsRTDataService   = mock(GTFSRTDataService.class);
        scheduler = new SchedulerService(mbajkDataService, weatherDataService, gtfsRTDataService, MIDDAY);
    }

    // fetchBikeIngestion

    @Test
    void fetchBikeIngestionSkipsWhenFlagIsFalse() {
        ReflectionTestUtils.setField(scheduler, "fetchBikeIngestion", false);

        scheduler.fetchBikeIngestion();

        verify(mbajkDataService, never()).ingestBikesData(any());
    }

    @Test
    void fetchBikeIngestionCallsServiceWhenFlagIsTrue() {
        ReflectionTestUtils.setField(scheduler, "fetchBikeIngestion", true);

        scheduler.fetchBikeIngestion();

        verify(mbajkDataService).ingestBikesData(any());
    }

    // fetchWeatherIngestion

    @Test
    void fetchWeatherIngestionSkipsWhenFlagIsFalse() {
        ReflectionTestUtils.setField(scheduler, "fetchWeatherIngestion", false);

        scheduler.fetchWeatherIngestion();

        verify(weatherDataService, never()).ingestWeatherData(any());
    }

    @Test
    void fetchWeatherIngestionCallsServiceWhenFlagIsTrue() {
        ReflectionTestUtils.setField(scheduler, "fetchWeatherIngestion", true);

        scheduler.fetchWeatherIngestion();

        verify(weatherDataService).ingestWeatherData(any());
    }

    // fetchBusIngestion

    @Test
    void fetchBusIngestionSkipsWhenFlagIsFalse() {
        ReflectionTestUtils.setField(scheduler, "fetchBusIngestion", false);

        scheduler.fetchBusIngestion();

        verify(gtfsRTDataService, never()).ingestRealtimeTrips(any());
    }

    @Test
    void fetchBusIngestionCallsServiceWhenFlagIsTrue() {
        ReflectionTestUtils.setField(scheduler, "fetchBusIngestion", true);

        scheduler.fetchBusIngestion();

        verify(gtfsRTDataService).ingestRealtimeTrips(any());
    }
}
