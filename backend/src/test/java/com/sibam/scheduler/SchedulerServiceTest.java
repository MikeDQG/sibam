package com.sibam.scheduler;

import ai.onnxruntime.OrtException;
import com.sibam.service.BikePredictionService;
import com.sibam.service.BusDelayPredictionService;
import com.sibam.service.GTFSRTDataService;
import com.sibam.service.MBajkDataService;
import com.sibam.service.WeatherDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SchedulerServiceTest {

    private static final Clock MIDDAY = Clock.fixed(
            Instant.parse("2026-06-02T10:00:00Z"), // 12:00 Ljubljana (UTC+2)
            ZoneId.of("Europe/Ljubljana")
    );

    // 03:00 Ljubljana — outside 05:00–23:00 operating window
    private static final Clock NIGHT = Clock.fixed(
            Instant.parse("2026-06-02T01:00:00Z"),
            ZoneId.of("Europe/Ljubljana")
    );

    private MBajkDataService mbajkDataService;
    private WeatherDataService weatherDataService;
    private GTFSRTDataService gtfsRTDataService;
    private BikePredictionService bikePredictionService;
    private BusDelayPredictionService busDelayPredictionService;
    private SchedulerService scheduler;

    @BeforeEach
    void setUp() {
        mbajkDataService         = mock(MBajkDataService.class);
        weatherDataService       = mock(WeatherDataService.class);
        gtfsRTDataService        = mock(GTFSRTDataService.class);
        bikePredictionService    = mock(BikePredictionService.class);
        busDelayPredictionService = mock(BusDelayPredictionService.class);
        scheduler = new SchedulerService(mbajkDataService, weatherDataService, gtfsRTDataService, bikePredictionService, busDelayPredictionService, MIDDAY);
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

    // outside operating hours — all three methods skip regardless of flag

    @Test
    void fetchBikeIngestionSkipsOutsideOperatingHours() {
        SchedulerService nightScheduler = new SchedulerService(mbajkDataService, weatherDataService, gtfsRTDataService, bikePredictionService, busDelayPredictionService, NIGHT);
        ReflectionTestUtils.setField(nightScheduler, "fetchBikeIngestion", true);

        nightScheduler.fetchBikeIngestion();

        verify(mbajkDataService, never()).ingestBikesData(any());
    }

    @Test
    void fetchWeatherIngestionSkipsOutsideOperatingHours() {
        SchedulerService nightScheduler = new SchedulerService(mbajkDataService, weatherDataService, gtfsRTDataService, bikePredictionService, busDelayPredictionService, NIGHT);
        ReflectionTestUtils.setField(nightScheduler, "fetchWeatherIngestion", true);

        nightScheduler.fetchWeatherIngestion();

        verify(weatherDataService, never()).ingestWeatherData(any());
    }

    @Test
    void fetchBusIngestionSkipsOutsideOperatingHours() {
        SchedulerService nightScheduler = new SchedulerService(mbajkDataService, weatherDataService, gtfsRTDataService, bikePredictionService, busDelayPredictionService, NIGHT);
        ReflectionTestUtils.setField(nightScheduler, "fetchBusIngestion", true);

        nightScheduler.fetchBusIngestion();

        verify(gtfsRTDataService, never()).ingestRealtimeTrips(any());
    }

    // exception handling — exceptions are caught and must not propagate

    @Test
    void fetchBikeIngestionDoesNotPropagateException() {
        ReflectionTestUtils.setField(scheduler, "fetchBikeIngestion", true);
        doThrow(new RuntimeException("network error")).when(mbajkDataService).ingestBikesData(any());

        assertThatCode(() -> scheduler.fetchBikeIngestion()).doesNotThrowAnyException();
    }

    @Test
    void fetchWeatherIngestionDoesNotPropagateException() {
        ReflectionTestUtils.setField(scheduler, "fetchWeatherIngestion", true);
        doThrow(new RuntimeException("network error")).when(weatherDataService).ingestWeatherData(any());

        assertThatCode(() -> scheduler.fetchWeatherIngestion()).doesNotThrowAnyException();
    }

    @Test
    void fetchBusIngestionDoesNotPropagateException() {
        ReflectionTestUtils.setField(scheduler, "fetchBusIngestion", true);
        doThrow(new RuntimeException("network error")).when(gtfsRTDataService).ingestRealtimeTrips(any());

        assertThatCode(() -> scheduler.fetchBusIngestion()).doesNotThrowAnyException();
    }

    // reloadMlModels

    @Test
    void reloadMlModelsSkipsWhenFlagIsFalse() throws Exception {
        ReflectionTestUtils.setField(scheduler, "reloadMlModels", false);

        scheduler.reloadMlModels();

        verify(bikePredictionService, never()).reloadModels();
        verify(busDelayPredictionService, never()).reloadModel();
    }

    @Test
    void reloadMlModelsCallsBothServicesWhenFlagIsTrue() throws Exception {
        ReflectionTestUtils.setField(scheduler, "reloadMlModels", true);

        scheduler.reloadMlModels();

        verify(bikePredictionService).reloadModels();
        verify(busDelayPredictionService).reloadModel();
    }

    @Test
    void reloadMlModelsDoesNotPropagateException() throws Exception {
        ReflectionTestUtils.setField(scheduler, "reloadMlModels", true);
        doThrow(new OrtException("download failed")).when(bikePredictionService).reloadModels();

        assertThatCode(() -> scheduler.reloadMlModels()).doesNotThrowAnyException();
    }
}
