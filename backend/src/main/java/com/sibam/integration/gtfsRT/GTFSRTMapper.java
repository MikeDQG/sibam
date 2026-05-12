package com.sibam.integration.gtfsRT;

import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.sibam.model.trip.StopUpdate;
import com.sibam.model.trip.Trip;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GTFSRTMapper {

    public Trip gtfsRTToTrip(VehiclePosition vehiclePosition, TripUpdate tripUpdate) {

        Trip trip = new Trip();

        // VehiclePosition podatki
        if (vehiclePosition != null) {

            if (vehiclePosition.hasTrip()) {
                trip.setTripId(vehiclePosition.getTrip().getTripId());
                trip.setRouteId(vehiclePosition.getTrip().getRouteId());
            }

            if (vehiclePosition.hasVehicle()) {
                trip.setVehicleId(vehiclePosition.getVehicle().getId());
                trip.setVehicleLabel(vehiclePosition.getVehicle().getLabel());
            }

            if (vehiclePosition.hasPosition()) {
                trip.setLat(vehiclePosition.getPosition().getLatitude());
                trip.setLon(vehiclePosition.getPosition().getLongitude());
                if (vehiclePosition.getPosition().hasBearing()) {
                    trip.setBearing(vehiclePosition.getPosition().getBearing());
                }
            }

            if (vehiclePosition.hasCurrentStopSequence()) {
                trip.setCurrent_stop_sequence(vehiclePosition.getCurrentStopSequence());
            }

            if (vehiclePosition.hasStopId()) {
                trip.setStop_id(vehiclePosition.getStopId());
            }

            if (vehiclePosition.hasTimestamp()) {
                trip.setTimestamp(vehiclePosition.getTimestamp());
            }
        }

        // TripUpdate podatki
        if (tripUpdate != null) {

            // fallback če VehiclePosition nima podatkov
            if (trip.getTripId() == null && tripUpdate.hasTrip()) {
                trip.setTripId(tripUpdate.getTrip().getTripId());
            }

            if (trip.getRouteId() == null && tripUpdate.hasTrip()) {
                trip.setRouteId(tripUpdate.getTrip().getRouteId());
            }

            List<StopUpdate> stopUpdates = getStopUpdates(tripUpdate);
            trip.setStopUpdates(stopUpdates);
        }

        return trip;
    }

    private static List<StopUpdate> getStopUpdates(TripUpdate tripUpdate) {
        List<StopUpdate> stopUpdates = new ArrayList<>();

        for (StopTimeUpdate stopTimeUpdate : tripUpdate.getStopTimeUpdateList()) {
            StopUpdate stopUpdate = new StopUpdate();

            stopUpdate.setStopSequence(stopTimeUpdate.getStopSequence());
            stopUpdate.setStopId(stopTimeUpdate.getStopId());

            if (stopTimeUpdate.hasArrival()) {
                stopUpdate.setDelay(stopTimeUpdate.getArrival().getDelay());
            }
            stopUpdates.add(stopUpdate);
        }
        return stopUpdates;
    }
}