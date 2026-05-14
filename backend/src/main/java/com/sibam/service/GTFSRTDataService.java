package com.sibam.service;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.sibam.integration.gtfsRT.GTFSRTClient;
import com.sibam.integration.gtfsRT.GTFSRTMapper;
import com.sibam.model.trip.StopUpdate;
import com.sibam.model.trip.Trip;
import com.sibam.persistence.TripEntity;
import com.sibam.persistence.StopDelayEntity;
import com.sibam.repository.StopDelaySnapshotRepository;
import com.sibam.repository.TripSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GTFSRTDataService {
    private final GTFSRTClient gtfsRTClient;
    private final GTFSRTMapper gtfsRTMapper;
    private final TripSnapshotRepository tripSnapshotRepository;
    private final StopDelaySnapshotRepository stopDelaySnapshotRepository;


    public GTFSRTDataService(GTFSRTClient gtfsRTClient, GTFSRTMapper gtfsRTMapper, TripSnapshotRepository tripSnapshotRepository, StopDelaySnapshotRepository stopDelaySnapshotRepository) {
        this.gtfsRTClient = gtfsRTClient;
        this.gtfsRTMapper = gtfsRTMapper;
        this.tripSnapshotRepository = tripSnapshotRepository;
        this.stopDelaySnapshotRepository = stopDelaySnapshotRepository;
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

    public void ingestRealtimeTrips(OffsetDateTime fetchedAt) {
       List<Trip> allTrips= getRealtimeTrips();

       for (Trip trip : allTrips) {
           TripEntity entity = new TripEntity();
           String tripId = trip.getTripId();
           entity.setTripId(tripId == null || tripId.isEmpty() ? null : tripId);
           entity.setRouteId(trip.getRouteId());
           entity.setBearing(trip.getBearing());
           entity.setLat(trip.getLat());
           entity.setLon(trip.getLon());
           entity.setRecordedAt(fetchedAt);
           entity.setVehicleId(trip.getVehicleId());
           entity.setVehicleLabel(trip.getVehicleLabel());
           entity.setCurrentStopSequence(trip.getCurrent_stop_sequence());
           entity.setCurrentStopId(trip.getStop_id());

           TripEntity savedEntity = tripSnapshotRepository.save(entity);

           if (trip.getStopUpdates() != null) {
               for (StopUpdate stopUpdate : trip.getStopUpdates()) {
                   StopDelayEntity stopDelay = new StopDelayEntity();
                   stopDelay.setTrip(savedEntity);
                   stopDelay.setStopSequence(stopUpdate.getStopSequence());
                   stopDelay.setDelaySeconds(stopUpdate.getDelay());
                   stopDelaySnapshotRepository.save(stopDelay);
              }
           }




       }
    }
}
