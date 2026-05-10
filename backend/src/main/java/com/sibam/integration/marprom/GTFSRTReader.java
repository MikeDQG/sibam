package com.sibam.integration.marprom;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GTFSRTReader {

    public static void main(String[] args) {
        List<String> files = new ArrayList<String>();
        files.add("src/main/resources/vehicle_positions.pb");
        files.add("src/main/resources/trip_updates.pb");

        for(String file : files){
            read(file);
        }
    }

    public static void read(String filePath) {
        // Pot do vaše lokalne GTFS-RT datoteke

        // Uporaba try-with-resources za samodejno zapiranje streama
        try (FileInputStream fis = new FileInputStream(filePath)) {

            // Razčlenimo binarno datoteko v FeedMessage objekt
            FeedMessage feed = FeedMessage.parseFrom(fis);

            // Sprehodimo se skozi vse entitete v sporočilu
            for (FeedEntity entity : feed.getEntityList()) {

                // Preverimo, katero vrsto podatkov entiteta vsebuje
                if (entity.hasTripUpdate()) {
                    TripUpdate tripUpdate = entity.getTripUpdate();

                    // Osnovne informacije o vožnji
                    System.out.println("=== TRIP UPDATE ===");
                    System.out.println("Trip ID: " + tripUpdate.getTrip().getTripId());
                    System.out.println("Route ID: " + tripUpdate.getTrip().getRouteId());
                    System.out.println("Start date: " + tripUpdate.getTrip().getStartDate());
                    System.out.println("Start time: " + tripUpdate.getTrip().getStartTime());
                    System.out.println("Schedule relationship: " + tripUpdate.getTrip().getScheduleRelationship());

                    // Informacije o zamudi (če obstaja)
                    if (tripUpdate.hasDelay()) {
                        System.out.println("Delay (seconds): " + tripUpdate.getDelay());
                        System.out.println("Delay (minutes): " + (tripUpdate.getDelay() / 60));
                    }

                    // Podatki o vsaki postaji (stop time updates)
                    for (TripUpdate.StopTimeUpdate stopTime : tripUpdate.getStopTimeUpdateList()) {
                        System.out.println("  Stop sequence: " + stopTime.getStopSequence());
                        System.out.println("  Stop ID: " + stopTime.getStopId());

                        if (stopTime.hasArrival()) {
                            System.out.println("    Arrival delay: " + stopTime.getArrival().getDelay());
                            System.out.println("    Arrival time: " + stopTime.getArrival().getTime());
                        }

                        if (stopTime.hasDeparture()) {
                            System.out.println("    Departure delay: " + stopTime.getDeparture().getDelay());
                        }

                        System.out.println("    Schedule relationship: " + stopTime.getScheduleRelationship());
                        System.out.println("  ---");
                    }
                    System.out.println();
                } else if (entity.hasVehicle()) {
                    VehiclePosition vehicle = entity.getVehicle();

                    System.out.println("=== VEHICLE POSITION ===");
                    System.out.println("Vehicle ID: " + vehicle.getVehicle().getId());
                    System.out.println("Vehicle label: " + vehicle.getVehicle().getLabel());

                    // Informacije o trenutni vožnji
                    if (vehicle.hasTrip()) {
                        System.out.println("Current trip ID: " + vehicle.getTrip().getTripId());
                        System.out.println("Route ID: " + vehicle.getTrip().getRouteId());
                        System.out.println("Direction ID: " + vehicle.getTrip().getDirectionId());
                        System.out.println("Start date: " + vehicle.getTrip().getStartDate());
                    }

                    // Lokacija
                    if (vehicle.hasPosition()) {
                        System.out.println("Latitude: " + vehicle.getPosition().getLatitude());
                        System.out.println("Longitude: " + vehicle.getPosition().getLongitude());
                        if (vehicle.getPosition().hasBearing()) {
                            System.out.println("Bearing (direction): " + vehicle.getPosition().getBearing());
                        }
                    }

                    // Hitrost
//                    if (vehicle.hasField()) {
//                        System.out.println("Speed (km/h): " + (vehicle.getSpeed() * 3.6));
//                        System.out.println("Speed (m/s): " + vehicle.getSpeed());
//                    }

                    // Časovni žig
                    if (vehicle.hasTimestamp()) {
                        System.out.println("Timestamp: " + new Date(vehicle.getTimestamp() * 1000));
                    }

                    // Status vozila
                    if (vehicle.hasCurrentStatus()) {
                        System.out.println("Status: " + vehicle.getCurrentStatus());
                        // Možne vrednosti: IN_TRANSIT, STOPPED_AT, INCOMING_AT
                    }

                    System.out.println("Occupancy status: " + vehicle.getOccupancyStatus());
                    System.out.println();
                } else if (entity.hasAlert()) {
                    System.out.println("Alert: " + entity.getAlert().getHeaderText().getTranslation(0).getText());
                    // Tukaj obdelajte alarme
                }
            }

        } catch (IOException e) {
            System.err.println("Napaka pri branju datoteke: " + e.getMessage());
            e.printStackTrace();
        }
    }
}