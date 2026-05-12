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

    /**
     * Pridobivanje podatkov MBajk koles in vremena, vsakih 5 minut
     */
    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void fetchBikeAndWeatherIngestion() {
        OffsetDateTime fetchedAt = OffsetDateTime.now(ZoneId.of("Europe/Ljubljana"));
        log.info("Fetching ingestion at {}", fetchedAt);

        try {
            mbajkDataService.ingestBikesData(fetchedAt);
            weatherDataService.ingestWeatherData(fetchedAt);
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
        log.info("Fetching Vehicle Positions");

        try {
            gtfsRTDataService.getRealtimeTrips();
        } catch (Exception e) {
            log.error("Failed to fetch Trips data", e);
        }
    }
}
