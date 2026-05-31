package com.sibam.scheduler;

import com.sibam.service.GTFSRTDataService;
import com.sibam.service.MBajkDataService;
import com.sibam.service.WeatherDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests the flag-based skipping logic in SchedulerService.
 *
 * Note: isWithinOperatingHours() uses OffsetDateTime.now() directly and cannot be
 * unit-tested without refactoring to inject a Clock. These tests assume they run
 * between 05:00 and 23:00 Ljubljana time (standard CI conditions).
 */
class SchedulerServiceTest {

    private MBajkDataService mbajkDataService;
    private WeatherDataService weatherDataService;
    private GTFSRTDataService gtfsRTDataService;
    private SchedulerService scheduler;

    @BeforeEach
    void setUp() {
        mbajkDataService    = mock(MBajkDataService.class);
        weatherDataService  = mock(WeatherDataService.class);
        gtfsRTDataService   = mock(GTFSRTDataService.class);
        scheduler = new SchedulerService(mbajkDataService, weatherDataService, gtfsRTDataService);
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
