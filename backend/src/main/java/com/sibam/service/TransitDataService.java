package com.sibam.service;

import com.sibam.dto.marprom.lines.MarpromLineDto;
import com.sibam.dto.marprom.lines.MarpromLinesResponse;
import com.sibam.dto.marprom.routes.MarpromRouteDto;
import com.sibam.dto.marprom.routes.MarpromRoutesResponseDto;
import com.sibam.dto.marprom.schedules.MarpromLineScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromRouteScheduleDto;
import com.sibam.dto.marprom.schedules.MarpromScheduleResponse;
import com.sibam.dto.marprom.schedules.MarpromStopScheduleDto;
import com.sibam.dto.marprom.stops.MarpromStopDto;
import com.sibam.dto.marprom.stops.MarpromStopsResponse;
import com.sibam.dto.marprom.trips.MarpromTripDto;
import com.sibam.dto.marprom.trips.MarpromTripsResponseDto;
import org.springframework.stereotype.Service;
import com.sibam.integration.marprom.MarpromClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransitDataService {

    private final MarpromClient marpromClient;

    public TransitDataService(MarpromClient marpromClient) {
        this.marpromClient = marpromClient;
    }

    public List<MarpromStopDto> getBusStops() {

        MarpromStopsResponse response = marpromClient.getAllStops().block();
        if (response == null) {
            System.out.println("Ni najdenih postaj");
            return null;
        }

        System.out.println("\n--- ŠibaM: Začenjam prevzem podatkov ---");

        List<MarpromStopDto> stopPoints = response.stops();

        System.out.println("Število najdenih postajališč: " + stopPoints.size());

        // Izpis prvih treh za kontrolo
        stopPoints.stream().limit(3).forEach(s -> {
            System.out.printf("Postaja: %s (ID: %s) na [%s, %s]%n",
                    s.name(), s.id(), s.lat(), s.lon());
        });
        return stopPoints;
    }

    public List<MarpromLineDto> getLines() {
        MarpromLinesResponse response = marpromClient.getLines().block();
        if (response == null) {
            System.out.println("Ni najdenih linij.");
            return null;
        }

        System.out.println("\n--- ŠibaM: Začenjam prevzem linij ---");

        List<MarpromLineDto> lines = response.lines();

        System.out.println("Število najdenih linij: " + lines.size());

        // Izpis prvih treh za kontrolo
        lines.stream().limit(3).forEach(s -> {
            System.out.printf("Linija: %s (ID: %s) %s%n",
                    s.code(), s.lineId(), s.destination());
        });

        return lines;
    }


    public List<MarpromRouteDto> getAllRoutes() {
        List<MarpromLineDto> lines = getLines();
        List<MarpromRouteDto> routes = new ArrayList<>();
        lines.forEach(l -> {
            routes.addAll(getRoutes(l.lineId()));
        });
        return routes;
    }

    public List<MarpromRouteDto> getRoutes(int lineId) {

        MarpromRoutesResponseDto response = marpromClient.getRoutes(lineId).block();

        if (response == null) {
            System.out.println("Ni najdenih tras.");
            return null;
        }
        System.out.println("\n--- ŠibaM: Začenjam prevzem tras ---");

        List<MarpromRouteDto> routes = response.routes();

        System.out.println("Število najdenih tras: " + routes.size());

        // Izpis prvih treh za kontrolo
        routes.stream().limit(3).forEach(s -> {
            System.out.printf("Trasa: %s (LineId: %s; RouteId: %s) %n",
                    s.headsignName(), s.lineId(), s.routeId());
        });

        return routes;
    }

    public List<MarpromStopScheduleDto> getStopScheduleForLine(int lineId) {

        MarpromScheduleResponse response = marpromClient.getStopScheduleForLine(lineId).block();
        if (response == null) {
            System.out.println("Ni najdenih voznih redov.");
            return null;
        }
        System.out.println("\n--- ŠibaM: Začenjam prevzem voznega reda ---");

        List<MarpromStopScheduleDto> schedules = response.schedules();

        MarpromStopScheduleDto schedule = schedules.getFirst();
        System.out.printf("Postajalisce: %s (Ime: %s; Naslov: %s) %n",schedule.stopPoint().id(),schedule.stopPoint().name(), schedule.stopPoint().address());
        MarpromLineScheduleDto scheduleForLine = schedule.scheduleForLine().getFirst();
        MarpromRouteScheduleDto s = scheduleForLine.routeAndSchedules().getFirst();
        System.out.printf("Vozni red za linijo: %s; Smer: %s; Prvi odhod: %s %n",scheduleForLine.lineId(),s.direction(),s.departures().getFirst());

        return schedules;
    }

    public List<MarpromTripDto> getTrips(int lineId) {

        MarpromTripsResponseDto response = marpromClient.getTrips(lineId).block();
        if (response == null) {
            System.out.println("Ni najdenih potovanj.");
            return null;
        }
        System.out.println("\n--- ŠibaM: Začenjam prevzem potovanj ---");

        List<MarpromTripDto> trips = response.trips();

        System.out.println("Število najdenih potovanj: " + trips.size());

        // Izpis prvih treh za kontrolo
        trips.stream().limit(3).forEach(s -> {
            System.out.printf("Potovanje: %s (LineId: %s; RouteId: %s) %n",
                    s.tripId(), s.lineId(), s.routeId());
        });

        return trips;
    }
}
