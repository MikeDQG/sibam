package com.sibam.service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import com.sibam.integration.marprom.MarpromClient;

import java.util.List;
import java.util.Map;

@Service
public class TransitDataService {

    private final MarpromClient marpromClient;

    public TransitDataService(MarpromClient marpromClient) {
        this.marpromClient = marpromClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void testDataIngestion() {
        System.out.println("--- ŠibaM: Začenjam testni prevzem podatkov ---");

        marpromClient.getAllStops().subscribe(response -> {
            List<Map<String, Object>> stopPoints = (List<Map<String, Object>>) response.get("StopPoints"); //[cite: 2]

            System.out.println("Število najdenih postajališč: " + stopPoints.size());

            // Izpis prvih treh za kontrolo
            stopPoints.stream().limit(3).forEach(s -> {
                System.out.printf("Postaja: %s (ID: %s) na [%s, %s]%n",
                        s.get("Name"), s.get("StopPointId"), s.get("Lat"), s.get("Lon"));
            });
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void testGetLines() {
        System.out.println("--- ŠibaM: Začenjam testni prevzem linij ---");

    }
}
