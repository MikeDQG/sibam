package com.sibam.scheduler;

import com.sibam.service.GTFSRTDataService;
import com.sibam.service.MBajkDataService;
import com.sibam.service.WeatherDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final MBajkDataService mbajkDataService;
    private final WeatherDataService weatherDataService;
    private final GTFSRTDataService gtfsRTDataService;
    private static final ZoneId LJUBLJANA = ZoneId.of("Europe/Ljubljana");

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
        if (!isWithinOperatingHours()) return;
        try {
            mbajkDataService.ingestBikesData(fetchedAt);
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
        if (!isWithinOperatingHours()) return;
        try {
            weatherDataService.ingestWeatherData(fetchedAt);
        } catch (Exception e) {
            log.error("Failed to fetch ingestion data", e);
        }
    }


    /**
     * Pridobivanje podatkov o zamudah avtobusov v realnem času, vsako minuto
     */

    @Scheduled(fixedRate = 1000 * 60)
    public void fetchBusIngestion() {
        if (!isWithinOperatingHours()) return;
        OffsetDateTime fetchedAt = OffsetDateTime.now(LJUBLJANA);

        try {
            gtfsRTDataService.ingestRealtimeTrips(fetchedAt);
        } catch (Exception e) {
            log.error("Failed to fetch ingestion data", e);
        }
    }

    /**
     * Pridobivanje zamud in lokacij iz ProtoBuf datotek
     */
    @Scheduled(fixedRate = 1000 * 30)
    public void fetchBusTripsIngestion() {
        System.out.println();

        try {
            gtfsRTDataService.getRealtimeTrips();
        } catch (Exception e) {
            log.error("Failed to fetch Trips data", e);
        }
    }
}
