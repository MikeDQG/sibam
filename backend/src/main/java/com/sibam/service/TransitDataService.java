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
    private void onAppReady() {
        System.out.println("Application ready");
        testDataIngestion();
        testGetLines();
        testGetRoutes();
        testGetStopScheduleForLine();
        testGetTrips();
    }

    public void testDataIngestion() {

        marpromClient.getAllStops().subscribe(response -> {
            System.out.println("\n--- ŠibaM: Začenjam testni prevzem podatkov ---");

            List<Map<String, Object>> stopPoints = (List<Map<String, Object>>) response.get("StopPoints"); //[cite: 2]

            System.out.println("Število najdenih postajališč: " + stopPoints.size());

            // Izpis prvih treh za kontrolo
            stopPoints.stream().limit(3).forEach(s -> {
                System.out.printf("Postaja: %s (ID: %s) na [%s, %s]%n",
                        s.get("Name"), s.get("StopPointId"), s.get("Lat"), s.get("Lon"));
            });
        });
    }

    public void testGetLines() {
        marpromClient.getLines().subscribe(response -> {
            System.out.println("\n--- ŠibaM: Začenjam testni prevzem linij ---");

            List<Map<String, Object>> lines = (List<Map<String, Object>>) response.get("Lines");

            System.out.println("Število najdenih linij: " + lines.size());

            // Izpis prvih treh za kontrolo
            lines.stream().limit(3).forEach(s -> {
                System.out.printf("Linija: %s (ID: %s) %s%n",
                        s.get("Code"), s.get("LineId"), s.get("Description"));
            });
        });
    }

    public void testGetRoutes() {
        int lineId = 67;    // test, 67 je Tezno

        marpromClient.getRoutes(lineId).subscribe(response -> {
            System.out.println("\n--- ŠibaM: Začenjam testni prevzem tras ---");

            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("Routes");

            System.out.println("Število najdenih tras: " + routes.size());

            // Izpis prvih treh za kontrolo
            routes.stream().limit(3).forEach(s -> {
                System.out.printf("Trasa: %s (LineId: %s; RouteId: %s) %n",
                        s.get("HeadsignName"), s.get("LineId"), s.get("RouteId"));
            });
        });
    }

    public void testGetStopScheduleForLine() {
        int lineId = 67;    // test, 67 je Tezno

        marpromClient.getStopScheduleForLine(lineId).subscribe(response -> {
            System.out.println("\n--- ŠibaM: Začenjam testni prevzem voznega reda ---");

            List<Map<String, Object>> schedules = (List<Map<String, Object>>) response.get("Schedules");

            // Izpis prvih treh za kontrolo
            schedules.stream().limit(3).forEach(s -> {
                Map<String, Object> stopPoint = (Map<String, Object>) s.get("StopPoint");

                List<Map<String, Object>> scheduleForLine = (List<Map<String, Object>>) s.get("ScheduleForLine");

                System.out.printf("StopPoint: %s (StopPointId: %s; Description: %s) %s%n",
                        stopPoint.get("Name"), stopPoint.get("StopPointId"), stopPoint.get("Description"), scheduleForLine.getFirst().get("LineId"));
            });
        });
    }

    public void testGetTrips() {
        int lineId = 67;    // test, 67 je Tezno

        marpromClient.getTrips(lineId).subscribe(response -> {
            System.out.println("\n--- ŠibaM: Začenjam testni prevzem potovanj ---");

            List<Map<String, Object>> trips = (List<Map<String, Object>>) response.get("Trips");

            System.out.println("Število najdenih potovanj: " + trips.size());

            // Izpis prvih treh za kontrolo
            trips.stream().limit(3).forEach(s -> {
                System.out.printf("Potovanje: %s (LineId: %s; RouteId: %s) %n",
                        s.get("TripId"), s.get("LineId"), s.get("RouteId"));
            });
        });
    }
}
