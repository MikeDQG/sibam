package com.sibam.service;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.sibam.integration.gtfsRT.GTFSRTClient;
import com.sibam.integration.gtfsRT.GTFSRTMapper;
import com.sibam.model.trip.Trip;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GTFSRTDataService {
    private final GTFSRTClient gtfsRTClient;
    private final GTFSRTMapper gtfsRTMapper;

    public GTFSRTDataService(GTFSRTClient gtfsRTClient, GTFSRTMapper gtfsRTMapper) {
        this.gtfsRTClient = gtfsRTClient;
        this.gtfsRTMapper = gtfsRTMapper;
    }

    public List<Trip> getRealtimeTrips() {

        List<VehiclePosition> vehicles = gtfsRTClient.getVehiclePositions().getEntityList().stream().filter(FeedEntity::hasVehicle).map(FeedEntity::getVehicle).toList();

        Map<String, TripUpdate> updatesByKey = gtfsRTClient.getTripUpdates().getEntityList().stream().filter(FeedEntity::hasTripUpdate).map(FeedEntity::getTripUpdate).filter(TripUpdate::hasTrip).collect(Collectors.toMap(this::createKey, Function.identity(),

                // če pride duplicate key
                // obdrži novejši timestamp
                (existing, replacement) -> {

                    long existingTimestamp = existing.hasTimestamp() ? existing.getTimestamp() : 0;

                    long replacementTimestamp = replacement.hasTimestamp() ? replacement.getTimestamp() : 0;

                    return replacementTimestamp >= existingTimestamp ? replacement : existing;
                }));

        List<Trip> trips = vehicles.stream().map(vehicle -> {

            TripUpdate update = null;

            if (vehicle.hasTrip()) {

                String key = createKey(vehicle);

                update = updatesByKey.get(key);
            }

            return gtfsRTMapper.gtfsRTToTrip(vehicle, update);
        }).toList();

        log.info("Loaded {} realtime trips", trips.size());

        trips.stream().limit(3).forEach(trip -> log.info("Trip {} Route {} Vehicle {}", trip.getTripId(), trip.getRouteId(), trip.getVehicleId()));

        return trips;
    }

    private String createKey(TripUpdate update) {

        String tripId = update.getTrip().getTripId();

        String vehicleId = update.hasVehicle() ? update.getVehicle().getId() : "UNKNOWN";

        return tripId + "_" + vehicleId;
    }

    private String createKey(VehiclePosition vehicle) {

        String tripId = vehicle.hasTrip() ? vehicle.getTrip().getTripId() : "UNKNOWN";

        String vehicleId = vehicle.hasVehicle() ? vehicle.getVehicle().getId() : "UNKNOWN";

        return tripId + "_" + vehicleId;
    }
}
