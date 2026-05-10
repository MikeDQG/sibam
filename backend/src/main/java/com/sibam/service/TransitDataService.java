package com.sibam.service;

import com.sibam.dto.marprom.lines.MarpromLineDto;
import com.sibam.dto.marprom.lines.MarpromRouteShortDto;
import com.sibam.dto.marprom.routes.MarpromRouteDto;
import com.sibam.dto.marprom.schedules.MarpromLineScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromRouteScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromStopScheduleDto;
import com.sibam.dto.marprom.stops.MarpromStopDto;
import com.sibam.dto.marprom.trips.MarpromTripDto;
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

            List<MarpromStopDto> stopPoints = response.stops();

            System.out.println("Število najdenih postajališč: " + stopPoints.size());

            // Izpis prvih treh za kontrolo
            stopPoints.stream().limit(3).forEach(s -> {
                System.out.printf("Postaja: %s (ID: %s) na [%s, %s]%n",
                        s.name(), s.id(), s.lat(), s.lon());
            });
        });
    }

    public void testGetLines() {
        marpromClient.getLines().subscribe(response -> {
            System.out.println("\n--- ŠibaM: Začenjam testni prevzem linij ---");

            List<MarpromLineDto> lines = response.lines();

            System.out.println("Število najdenih linij: " + lines.size());

            // Izpis prvih treh za kontrolo
            lines.stream().limit(3).forEach(s -> {
                System.out.printf("Linija: %s (ID: %s) %s%n",
                        s.code(), s.lineId(), s.destination());
            });
        });
    }

    public void testGetRoutes() {
        int lineId = 67;    // test, 67 je Tezno

        marpromClient.getRoutes(lineId).subscribe(response -> {
            System.out.println("\n--- ŠibaM: Začenjam testni prevzem tras ---");

            List<MarpromRouteDto> routes = response.routes();

            System.out.println("Število najdenih tras: " + routes.size());

            // Izpis prvih treh za kontrolo
            routes.stream().limit(3).forEach(s -> {
                System.out.printf("Trasa: %s (LineId: %s; RouteId: %s) %n",
                        s.headsignName(), s.lineId(), s.routeId());
            });
        });
    }

    public void testGetStopScheduleForLine() {
        int lineId = 67;    // test, 67 je Tezno

        marpromClient.getStopScheduleForLine(lineId).subscribe(response -> {
            System.out.println("\n--- ŠibaM: Začenjam testni prevzem voznega reda ---");

            List<MarpromStopScheduleDto> schedules = response.schedules();

            MarpromStopScheduleDto schedule = schedules.getFirst();
            System.out.printf("Postajalisce: %s (Ime: %s; Naslov: %s) %n",schedule.stopPoint().id(),schedule.stopPoint().name(), schedule.stopPoint().address());
            MarpromLineScheduleDto scheduleForLine = schedule.scheduleForLine().getFirst();
            MarpromRouteScheduleDto s = scheduleForLine.routeAndSchedules().getFirst();
            System.out.printf("Vozni red za linijo: %s; Smer: %s; Prvi odhod: %s %n",scheduleForLine.lineId(),s.direction(),s.departures().getFirst());
        });
    }

    public void testGetTrips() {
        int lineId = 67;    // test, 67 je Tezno

        marpromClient.getTrips(lineId).subscribe(response -> {
            System.out.println("\n--- ŠibaM: Začenjam testni prevzem potovanj ---");

            List<MarpromTripDto> trips = response.trips();

            System.out.println("Število najdenih potovanj: " + trips.size());

            // Izpis prvih treh za kontrolo
            trips.stream().limit(3).forEach(s -> {
                System.out.printf("Potovanje: %s (LineId: %s; RouteId: %s) %n",
                        s.tripId(), s.lineId(), s.routeId());
            });
        });
    }
}
