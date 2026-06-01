package com.sibam.scheduler;

import com.sibam.service.GTFSRTDataService;
import com.sibam.service.MBajkDataService;
import com.sibam.service.WeatherDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
public class SchedulerService {
    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final MBajkDataService mbajkDataService;
    private final WeatherDataService weatherDataService;
    private final GTFSRTDataService gtfsRTDataService;
    private static final ZoneId LJUBLJANA = ZoneId.of("Europe/Ljubljana");

    @Value("${schedulers.fetch-bike-ingestion.on}")
    private boolean fetchBikeIngestion;

    @Value("${schedulers.fetch-weather-ingestion.on}")
    private boolean fetchWeatherIngestion;

    @Value("${schedulers.fetch-bus-ingestion.on}")
    private boolean fetchBusIngestion;

    public SchedulerService(
            MBajkDataService mbajkDataService,
            WeatherDataService weatherDataService,
            GTFSRTDataService gtfsRTDataService
    ) {
        this.mbajkDataService = mbajkDataService;
        this.weatherDataService = weatherDataService;
        this.gtfsRTDataService = gtfsRTDataService;
    }

    private boolean isWithinOperatingHours() {
        int hour = OffsetDateTime.now(LJUBLJANA).getHour();
        return hour >= 5 && hour < 23;
    }

    /**
     * Pridobivanje podatkov MBajk koles in vremena, vsakih 5 minut
     */
    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void fetchBikeIngestion() {
        OffsetDateTime fetchedAt = OffsetDateTime.now(LJUBLJANA);
        if (!isWithinOperatingHours()) {
            log.info("Skipping bike ingestion, not within operating hours");
            return;
        }
        if (!fetchBikeIngestion) {
            log.info("Skipping bike ingestion, blocked by config");
            return;
        }
        try {
            mbajkDataService.ingestBikesData(fetchedAt);
            log.info("MBajk ingestion completed");
        } catch (Exception e) {
            log.error("Failed to fetch ingestion data", e);
        }
    }

    /**
     * Pridobivanje podatkov vremena, vsako 1 uro
     */
    @Scheduled(fixedRate = 1000 * 60 * 60)
    public void fetchWeatherIngestion() {
        OffsetDateTime fetchedAt = OffsetDateTime.now(LJUBLJANA);
        if (!isWithinOperatingHours()) {
            log.info("Skipping weather ingestion, not within operating hours");
            return;
        }
        if (!fetchWeatherIngestion) {
            log.info("Skipping weather ingestion, blocked by config");
            return;
        }
        try {
            weatherDataService.ingestWeatherData(fetchedAt);
            log.info("Weather ingestion completed");
        } catch (Exception e) {
            log.error("Failed to fetch ingestion data", e);
        }
    }

    /**
     * Pridobivanje podatkov o zamudah avtobusov v realnem času, vsako minuto
     */
    @Scheduled(fixedRate = 1000 * 60)
    public void fetchBusIngestion() {
        if (!isWithinOperatingHours()) {
            log.info("Skipping bus trips ingestion, not within operating hours");
            return;
        }
        if (!fetchBusIngestion) {
            log.info("Skipping bus trips ingestion, blocked by config");
            return;
        }
        OffsetDateTime fetchedAt = OffsetDateTime.now(LJUBLJANA);

        try {
            gtfsRTDataService.ingestRealtimeTrips(fetchedAt);
            log.info("Bus trips ingestion completed");
        } catch (Exception e) {
            log.error("Failed to fetch ingestion data", e);
        }
    }
}
