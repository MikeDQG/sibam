package com.sibam.scheduler;

import com.sibam.service.MBajkDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final MBajkDataService mbajkDataService;

    /**
     * Pridobivanje podatkov MBajk koles
     */
    @Scheduled(fixedRate = 1000 * 15)
    public void fetchBikesIngestion() {
        System.out.println();
        log.info("Fetching Bikes Ingestion");

        try {
            mbajkDataService.testBikesIngestion();
        } catch (Exception e) {
            log.error("Failed to fetch MBajk data", e);
        }
    }

    /**
     * Pridobivanje zamud in lokacij iz ProtoBuf datotek
     */
    @Scheduled(fixedRate = 1000 * 30)
    public void fetchBusPBIngestion() {
        System.out.println();
    }
}
