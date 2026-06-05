package com.sibam.scheduler;

import com.sibam.service.BikePredictionService;
import com.sibam.service.BusDelayPredictionService;
import com.sibam.service.GTFSRTDataService;
import com.sibam.service.MBajkDataService;
import com.sibam.service.WeatherDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Scheduler za periodični zajem zunanjih podatkov in osveževanje ML modelov.
 *
 * Zajema MBajk, OpenWeatherMap in Marprom GTFS-RT podatke v operativnem oknu
 * ter ponoči sproži ponovni prenos ONNX modelov iz Supabase Storage.
 */
@Service
public class SchedulerService {
    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    private static final String FAILED_INGESTION_MESSAGE = "Failed to fetch ingestion data";

    private final MBajkDataService mbajkDataService;
    private final WeatherDataService weatherDataService;
    private final GTFSRTDataService gtfsRTDataService;
    private final BikePredictionService bikePredictionService;
    private final BusDelayPredictionService busDelayPredictionService;
    private final Clock clock;

    @Value("${schedulers.fetch-bike-ingestion.on}")
    private boolean fetchBikeIngestion;

    @Value("${schedulers.fetch-weather-ingestion.on}")
    private boolean fetchWeatherIngestion;

    @Value("${schedulers.fetch-bus-ingestion.on}")
    private boolean fetchBusIngestion;

    @Value("${schedulers.reload-ml-models.on:false}")
    private boolean reloadMlModels;

    public SchedulerService(
            MBajkDataService mbajkDataService,
            WeatherDataService weatherDataService,
            GTFSRTDataService gtfsRTDataService,
            BikePredictionService bikePredictionService,
            BusDelayPredictionService busDelayPredictionService,
            Clock clock
    ) {
        this.mbajkDataService = mbajkDataService;
        this.weatherDataService = weatherDataService;
        this.gtfsRTDataService = gtfsRTDataService;
        this.bikePredictionService = bikePredictionService;
        this.busDelayPredictionService = busDelayPredictionService;
        this.clock = clock;
    }

    /**
     * Preveri, ali se schedulerji trenutno smejo izvajati.
     *
     * @return true med 05:00 in 23:00 glede na injiciran Clock
     */
    private boolean isWithinOperatingHours() {
        int hour = OffsetDateTime.now(clock).getHour();
        return hour >= 5 && hour < 23;
    }

    /**
     * Pridobivanje podatkov MBajk koles in vremena, vsakih 5 minut
     */
    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void fetchBikeIngestion() {
        OffsetDateTime fetchedAt = OffsetDateTime.now(clock);
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
            log.error(FAILED_INGESTION_MESSAGE, e);
        }
    }

    /**
     * Pridobivanje podatkov vremena, vsako 1 uro
     */
    @Scheduled(fixedRate = 1000 * 60 * 60)
    public void fetchWeatherIngestion() {
        OffsetDateTime fetchedAt = OffsetDateTime.now(clock);
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
            log.error(FAILED_INGESTION_MESSAGE, e);
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
        OffsetDateTime fetchedAt = OffsetDateTime.now(clock);

        try {
            gtfsRTDataService.ingestRealtimeTrips(fetchedAt);
            log.info("Bus trips ingestion completed");
        } catch (Exception e) {
            log.error(FAILED_INGESTION_MESSAGE, e);
        }
    }

    /**
     * Ponoven prenos ML modelov iz Supabase Storage, vsak dan ob 3:30 UTC (5:30 CEST).
     * Zažene se 2,5h po nočnem ML pipeline-u (1:00 UTC), ki pripravi nove modele.
     */
    @Scheduled(cron = "0 30 3 * * *")
    public void reloadMlModels() {
        if (!reloadMlModels) {
            log.info("Skipping ML model reload, blocked by config");
            return;
        }
        try {
            bikePredictionService.reloadModels();
            busDelayPredictionService.reloadModel();
            log.info("ML models reloaded successfully");
        } catch (Exception e) {
            log.error("Failed to reload ML models", e);
        }
    }
}
